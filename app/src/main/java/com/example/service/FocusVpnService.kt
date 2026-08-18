package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.FocusLockDatabase
import com.example.engine.LockEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * FocusVpnService: Local, on-device DNS filter for website blocking.
 *
 * SCOPE & PRIVACY GUARANTEES:
 * 1. Operates strictly at the DNS level by intercepting UDP Port 53 queries sent to 10.0.0.1.
 * 2. Only checks the queried domain name against user-defined WebsiteRules when a LockSession is ACTIVE.
 * 3. Cannot inspect, decrypt, or filter specific paths inside encrypted HTTPS connections (e.g., blocks instagram.com entirely, but cannot block youtube.com/shorts while allowing youtube.com).
 * 4. Never proxies, logs, stores, or transmits web page content or browsing traffic anywhere off the device.
 * 5. Allowed DNS queries are relayed directly to standard public resolvers (Google DNS 8.8.8.8) with zero modifications.
 */
class FocusVpnService : VpnService() {

    companion object {
        const val ACTION_START_VPN = "com.example.focuslock.ACTION_START_VPN"
        const val ACTION_STOP_VPN = "com.example.focuslock.ACTION_STOP_VPN"

        const val CHANNEL_ID = "focus_vpn_channel"
        const val NOTIFICATION_ID = 2001

        private val _isVpnRunning = MutableStateFlow(false)
        val isVpnRunning = _isVpnRunning.asStateFlow()

        // Upstream public DNS resolver (standard Google Public DNS)
        private const val UPSTREAM_DNS_IP = "8.8.8.8"
        private const val DNS_PORT = 53
    }

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnScope: CoroutineScope? = null
    private var isTerminating = false

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START_VPN

        when (action) {
            ACTION_START_VPN -> {
                if (!_isVpnRunning.value) {
                    startVpnEngine()
                }
            }
            ACTION_STOP_VPN -> {
                stopVpnEngine()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startVpnEngine() {
        isTerminating = false
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        try {
            // Build local TUN interface routing only local virtual DNS server
            val builder = Builder().apply {
                setSession("FocusLock DNS Filter")
                addAddress("10.0.0.2", 32)
                addDnsServer("10.0.0.1")
                addRoute("10.0.0.1", 32)
                setBlocking(true)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    setMetered(false)
                }
            }

            vpnInterface = builder.establish()
            if (vpnInterface == null) {
                _isVpnRunning.value = false
                stopSelf()
                return
            }

            _isVpnRunning.value = true
            vpnScope = CoroutineScope(Dispatchers.IO + Job())
            startDnsPacketLoop(vpnInterface!!)

        } catch (e: Exception) {
            _isVpnRunning.value = false
            stopSelf()
        }
    }

    private fun startDnsPacketLoop(pfd: ParcelFileDescriptor) {
        vpnScope?.launch(Dispatchers.IO) {
            val db = FocusLockDatabase.getInstance(applicationContext)
            val lockEngine = LockEngine(applicationContext)
            val inputStream = FileInputStream(pfd.fileDescriptor)
            val outputStream = FileOutputStream(pfd.fileDescriptor)

            val packetBuffer = ByteArray(4096)
            var upstreamSocket: DatagramSocket? = null

            try {
                upstreamSocket = DatagramSocket().apply {
                    soTimeout = 2000
                    protect(this) // Bypass VPN routing to reach public network
                }

                val upstreamAddr = InetAddress.getByName(UPSTREAM_DNS_IP)

                while (isActive && !isTerminating) {
                    val length = try {
                        inputStream.read(packetBuffer)
                    } catch (e: Exception) {
                        break
                    }

                    if (length <= 0) continue

                    // Parse IPv4 packet
                    val versionAndIhl = packetBuffer[0].toInt() and 0xFF
                    val version = versionAndIhl shr 4
                    if (version != 4) continue // Only IPv4 UDP DNS handled

                    val ihl = (versionAndIhl and 0x0F) * 4
                    if (length < ihl + 8) continue

                    val protocol = packetBuffer[9].toInt() and 0xFF
                    if (protocol != 17) continue // UDP = 17

                    val srcPort = ((packetBuffer[ihl].toInt() and 0xFF) shl 8) or (packetBuffer[ihl + 1].toInt() and 0xFF)
                    val dstPort = ((packetBuffer[ihl + 2].toInt() and 0xFF) shl 8) or (packetBuffer[ihl + 3].toInt() and 0xFF)

                    if (dstPort != DNS_PORT) continue // Only DNS queries intercepted

                    val dnsPayloadOffset = ihl + 8
                    val dnsPayloadLen = length - dnsPayloadOffset
                    if (dnsPayloadLen < 12) continue

                    val queriedDomain = DnsPacketHelper.extractDomainName(packetBuffer, dnsPayloadOffset)

                    val now = System.currentTimeMillis()
                    val activeSession = db.lockSessionDao().getLatestActiveSession()
                    val isLocked = activeSession != null && lockEngine.isSessionActive(activeSession, now)

                    var isBlocked = false
                    if (isLocked && queriedDomain != null) {
                        val websiteRules = db.websiteRuleDao().getEnabledWebsiteRulesSync()
                        isBlocked = DnsPacketHelper.isDomainBlocked(queriedDomain, websiteRules)
                    }

                    if (isBlocked) {
                        // Synthesize local NXDOMAIN response
                        val nxDomainPacket = DnsPacketHelper.buildNxDomainIpPacket(
                            ipPacket = packetBuffer.copyOf(length),
                            ipHeaderLen = ihl,
                            udpHeaderLen = 8
                        )
                        if (nxDomainPacket != null) {
                            try {
                                synchronized(outputStream) {
                                    outputStream.write(nxDomainPacket)
                                }
                            } catch (e: Exception) {
                                // Ignore write failure
                            }
                        }
                    } else {
                        // Relay to upstream public DNS server and return real response
                        try {
                            val dnsQueryPayload = ByteArray(dnsPayloadLen)
                            System.arraycopy(packetBuffer, dnsPayloadOffset, dnsQueryPayload, 0, dnsPayloadLen)

                            val outgoingPacket = DatagramPacket(
                                dnsQueryPayload,
                                dnsPayloadLen,
                                upstreamAddr,
                                DNS_PORT
                            )
                            upstreamSocket.send(outgoingPacket)

                            val responseBuffer = ByteArray(2048)
                            val incomingPacket = DatagramPacket(responseBuffer, responseBuffer.size)
                            upstreamSocket.receive(incomingPacket)

                            val clientIp = packetBuffer.copyOfRange(12, 16)
                            val serverIp = packetBuffer.copyOfRange(16, 20)

                            val rawDnsResponse = responseBuffer.copyOf(incomingPacket.length)
                            val responseIpPacket = DnsPacketHelper.wrapUpstreamDnsResponse(
                                upstreamDnsPayload = rawDnsResponse,
                                clientIp = clientIp,
                                serverIp = serverIp,
                                clientPort = srcPort,
                                serverPort = dstPort
                            )

                            synchronized(outputStream) {
                                outputStream.write(responseIpPacket)
                            }
                        } catch (e: Exception) {
                            // Upstream timeout or drop
                        }
                    }
                }
            } catch (e: Exception) {
                // General loop error
            } finally {
                upstreamSocket?.close()
            }
        }
    }

    private fun stopVpnEngine() {
        isTerminating = true
        _isVpnRunning.value = false
        vpnScope?.cancel()
        vpnScope = null
        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            // Ignore
        }
        vpnInterface = null
    }

    override fun onDestroy() {
        stopVpnEngine()
        super.onDestroy()
    }

    override fun onRevoke() {
        stopVpnEngine()
        super.onRevoke()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "DNS Website Filter"
            val descriptionText = "Active on-device DNS blocking for focus sessions"
            val channel = NotificationChannel(
                CHANNEL_ID,
                name,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = descriptionText
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FocusLock DNS Filter")
            .setContentText("On-device domain filtering active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }
}
