package com.example.service

import com.example.data.model.WebsiteRule
import java.nio.ByteBuffer

object DnsPacketHelper {

    /**
     * Extracts the requested domain name from a raw DNS query payload (RFC 1035 format).
     * Domain names are encoded as length-prefixed labels (e.g. \x06google\x03com\x00).
     */
    fun extractDomainName(dnsPayload: ByteArray, offset: Int = 0): String? {
        // DNS header is 12 bytes minimum
        if (dnsPayload.size < offset + 12) return null

        var cursor = offset + 12 // Start of Question section
        val domainParts = mutableListOf<String>()

        try {
            while (cursor < dnsPayload.size) {
                val labelLength = dnsPayload[cursor].toInt() and 0xFF
                if (labelLength == 0) {
                    // End of QNAME
                    break
                }
                // If it's a pointer (compression), not typical in top of question, but guard against it
                if ((labelLength and 0xC0) == 0xC0) {
                    break
                }
                cursor++
                if (cursor + labelLength > dnsPayload.size) return null
                val label = String(dnsPayload, cursor, labelLength, Charsets.US_ASCII)
                domainParts.add(label)
                cursor += labelLength
            }
        } catch (e: Exception) {
            return null
        }

        return if (domainParts.isNotEmpty()) domainParts.joinToString(".").lowercase() else null
    }

    /**
     * Checks whether [domain] matches any enabled [WebsiteRule].
     * Supports exact match and subdomain match (when includeSubdomains is true).
     */
    fun isDomainBlocked(domain: String, rules: List<WebsiteRule>): Boolean {
        val cleanDomain = domain.lowercase().trim().removePrefix("www.")
        for (rule in rules) {
            if (!rule.enabled) continue
            val ruleDomain = rule.domain.lowercase().trim().removePrefix("www.")

            if (cleanDomain == ruleDomain) {
                return true
            }

            if (rule.includeSubdomains) {
                if (cleanDomain.endsWith(".$ruleDomain")) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Builds an IPv4 UDP packet containing a DNS NXDOMAIN (Name Not Found / RCODE=3) response.
     */
    fun buildNxDomainIpPacket(
        ipPacket: ByteArray,
        ipHeaderLen: Int,
        udpHeaderLen: Int
    ): ByteArray? {
        val totalHeaderLen = ipHeaderLen + udpHeaderLen
        if (ipPacket.size < totalHeaderLen + 12) return null

        val dnsPayloadOffset = totalHeaderLen
        val dnsPayloadLen = ipPacket.size - dnsPayloadOffset

        // Read DNS Question section length
        var cursor = dnsPayloadOffset + 12
        while (cursor < ipPacket.size && ipPacket[cursor] != 0.toByte()) {
            val len = ipPacket[cursor].toInt() and 0xFF
            cursor += (len + 1)
        }
        cursor++ // skip 0x00
        cursor += 4 // QTYPE (2) + QCLASS (2)

        val questionSectionEnd = cursor.coerceAtMost(ipPacket.size)
        val questionSectionLen = questionSectionEnd - (dnsPayloadOffset + 12)
        val dnsResponseLen = 12 + questionSectionLen

        val responseDnsPayload = ByteArray(dnsResponseLen)
        // Copy Transaction ID
        responseDnsPayload[0] = ipPacket[dnsPayloadOffset]
        responseDnsPayload[1] = ipPacket[dnsPayloadOffset + 1]

        // Flags: QR=1 (Response), Opcode=0, AA=0, TC=0, RD=1, RA=1, RCODE=3 (NXDOMAIN)
        // 0x8183 -> 1000 0001 1000 0011
        responseDnsPayload[2] = 0x81.toByte()
        responseDnsPayload[3] = 0x83.toByte()

        // QDCOUNT = 1
        responseDnsPayload[4] = 0x00.toByte()
        responseDnsPayload[5] = 0x01.toByte()
        // ANCOUNT = 0, NSCOUNT = 0, ARCOUNT = 0
        responseDnsPayload[6] = 0
        responseDnsPayload[7] = 0
        responseDnsPayload[8] = 0
        responseDnsPayload[9] = 0
        responseDnsPayload[10] = 0
        responseDnsPayload[11] = 0

        // Copy Question section
        System.arraycopy(
            ipPacket,
            dnsPayloadOffset + 12,
            responseDnsPayload,
            12,
            questionSectionLen
        )

        return wrapInIpv4Udp(
            dnsPayload = responseDnsPayload,
            srcIp = ipPacket.copyOfRange(16, 20), // original dst IP becomes src
            dstIp = ipPacket.copyOfRange(12, 16), // original src IP becomes dst
            srcPort = getShort(ipPacket, ipHeaderLen + 2), // original dst port
            dstPort = getShort(ipPacket, ipHeaderLen) // original src port
        )
    }

    /**
     * Wraps a raw DNS response from upstream into an IPv4 UDP packet targeting the client.
     */
    fun wrapUpstreamDnsResponse(
        upstreamDnsPayload: ByteArray,
        clientIp: ByteArray,
        serverIp: ByteArray,
        clientPort: Int,
        serverPort: Int
    ): ByteArray {
        return wrapInIpv4Udp(
            dnsPayload = upstreamDnsPayload,
            srcIp = serverIp,
            dstIp = clientIp,
            srcPort = serverPort,
            dstPort = clientPort
        )
    }

    private fun wrapInIpv4Udp(
        dnsPayload: ByteArray,
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int
    ): ByteArray {
        val ipHeaderLen = 20
        val udpHeaderLen = 8
        val udpTotalLen = udpHeaderLen + dnsPayload.size
        val ipTotalLen = ipHeaderLen + udpTotalLen

        val packet = ByteArray(ipTotalLen)
        val buf = ByteBuffer.wrap(packet)

        // IPv4 Header
        buf.put(0x45.toByte()) // Version 4, IHL 5 (20 bytes)
        buf.put(0x00.toByte()) // DSCP / ECN
        buf.putShort(ipTotalLen.toShort()) // Total Length
        buf.putShort(0x0000.toShort()) // Identification
        buf.putShort(0x4000.toShort()) // Flags (Don't Fragment)
        buf.put(64.toByte()) // TTL
        buf.put(17.toByte()) // Protocol 17 (UDP)
        buf.putShort(0.toShort()) // Checksum placeholder
        buf.put(srcIp) // Source IP
        buf.put(dstIp) // Dest IP

        // Compute IP Header Checksum
        val ipChecksum = computeChecksum(packet, 0, ipHeaderLen)
        packet[10] = (ipChecksum shr 8).toByte()
        packet[11] = (ipChecksum and 0xFF).toByte()

        // UDP Header
        buf.position(ipHeaderLen)
        buf.putShort(srcPort.toShort())
        buf.putShort(dstPort.toShort())
        buf.putShort(udpTotalLen.toShort())
        buf.putShort(0.toShort()) // UDP checksum optional in IPv4

        // DNS Payload
        buf.put(dnsPayload)

        return packet
    }

    private fun getShort(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 8) or (bytes[offset + 1].toInt() and 0xFF)
    }

    private fun computeChecksum(data: ByteArray, offset: Int, length: Int): Int {
        var sum = 0
        var i = offset
        while (i < offset + length - 1) {
            val word = ((data[i].toInt() and 0xFF) shl 8) or (data[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < offset + length) {
            sum += (data[i].toInt() and 0xFF) shl 8
        }
        while ((sum shr 16) > 0) {
            sum = (sum and 0xFFFF) + (sum shr 16)
        }
        return sum.inv() and 0xFFFF
    }
}
