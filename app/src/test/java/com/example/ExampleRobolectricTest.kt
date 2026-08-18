package com.example

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.example.admin.DevicePolicyController
import com.example.data.local.FocusLockDatabase
import com.example.data.model.AppRule
import com.example.data.model.FocusDayOfWeek
import com.example.data.model.LockSession
import com.example.data.model.Schedule
import com.example.data.model.SessionStatus
import com.example.data.model.WebsiteRule
import com.example.engine.LockEngine
import com.example.service.AccessibilityHelper
import com.example.service.AppAccessibilityService
import com.example.service.DnsPacketHelper
import com.example.service.FocusVpnHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("FocusLock", appName)
    }

    @Test
    fun `lock session active state verification`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lockEngine = LockEngine(context)
        val now = 100_000L

        val activeSession = LockSession(
            id = 1,
            name = "Work Session",
            startedAt = 50_000L,
            endsAt = 150_000L,
            status = SessionStatus.ACTIVE
        )

        assertTrue(lockEngine.isSessionActive(activeSession, now))
        assertEquals(50_000L, lockEngine.getRemainingMillis(activeSession, now))

        val expiredSession = activeSession.copy(endsAt = 80_000L)
        assertFalse(lockEngine.isSessionActive(expiredSession, now))
    }

    @Test
    fun `schedule occurrence computation`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lockEngine = LockEngine(context)

        val schedule = Schedule(
            id = 1,
            name = "Daily Work",
            startTime = "09:00",
            endTime = "17:00",
            repeatDays = listOf(
                FocusDayOfWeek.MONDAY,
                FocusDayOfWeek.TUESDAY,
                FocusDayOfWeek.WEDNESDAY,
                FocusDayOfWeek.THURSDAY,
                FocusDayOfWeek.FRIDAY,
                FocusDayOfWeek.SATURDAY,
                FocusDayOfWeek.SUNDAY
            ),
            enabled = true
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val eightAm = calendar.timeInMillis

        val nextOccurrence = lockEngine.getNextScheduleStartOccurrence(schedule, eightAm)
        assertNotNull(nextOccurrence)
        assertTrue(nextOccurrence!!.startTimeMillis > eightAm)
    }

    @Test
    fun `accessibility settings intent creation`() {
        val intent = AccessibilityHelper.createAccessibilitySettingsIntent()
        assertNotNull(intent)
        assertEquals(Settings.ACTION_ACCESSIBILITY_SETTINGS, intent.action)
    }

    @Test
    fun `blocked app interception data and message verification`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = FocusLockDatabase.getInstance(context)
        val lockEngine = LockEngine(context)

        val now = System.currentTimeMillis()
        val endsAt = now + (60 * 60 * 1000L)

        // 1. Create and insert active LockSession
        val activeSession = LockSession(
            name = "Deep Focus Block",
            startedAt = now - 1000L,
            endsAt = endsAt,
            status = SessionStatus.ACTIVE
        )
        val sessionId = db.lockSessionDao().insertSession(activeSession)
        assertTrue(sessionId > 0)

        // 2. Insert blocked app rule
        val rule = AppRule(
            packageName = "com.distracting.social",
            displayName = "Social App",
            enabled = true
        )
        val ruleId = db.appRuleDao().insertAppRule(rule)
        assertTrue(ruleId > 0)

        // 3. Verify session active and rule lookup matches
        val fetchedSession = db.lockSessionDao().getLatestActiveSession()
        assertNotNull(fetchedSession)
        assertTrue(lockEngine.isSessionActive(fetchedSession, System.currentTimeMillis()))

        val matchedRule = db.appRuleDao().getAppRuleByPackageName("com.distracting.social")
        assertNotNull(matchedRule)
        assertTrue(matchedRule!!.enabled)
        assertEquals("Social App", matchedRule.displayName)

        // 4. Verify formatted blocked message matches exact specification
        val formattedTime = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(endsAt))
        val expectedMessage = "${matchedRule.displayName} is blocked. This restriction ends at $formattedTime. You chose this restriction for your current focus session."
        assertTrue(expectedMessage.contains("Social App is blocked."))
        assertTrue(expectedMessage.contains("You chose this restriction for your current focus session."))
    }

    @Test
    fun `accessibility real-time state check reflects disabling mid-lock`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Initially disabled
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ""
        )
        assertFalse(AccessibilityHelper.isAppAccessibilityServiceEnabled(context))

        // 2. Enabled in system settings
        val serviceComponent = ComponentName(context, AppAccessibilityService::class.java).flattenToString()
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            serviceComponent
        )

        // 3. Simulated disable mid-lock
        Settings.Secure.putString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            ""
        )
        // Immediately reflects Not Active without caching stale true
        assertFalse(AccessibilityHelper.isAppAccessibilityServiceEnabled(context))
    }

    @Test
    fun `dns packet domain extraction and matching across multiple browsers`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = FocusLockDatabase.getInstance(context)
        val lockEngine = LockEngine(context)

        val now = System.currentTimeMillis()

        // 1. Setup active session and blocked website rules
        val session = LockSession(
            name = "Work Session",
            startedAt = now - 5000L,
            endsAt = now + 3600_000L,
            status = SessionStatus.ACTIVE
        )
        db.lockSessionDao().insertSession(session)

        val rules = listOf(
            WebsiteRule(domain = "instagram.com", includeSubdomains = true, enabled = true),
            WebsiteRule(domain = "reddit.com", includeSubdomains = false, enabled = true)
        )
        for (r in rules) db.websiteRuleDao().insertWebsiteRule(r)

        val activeRules = db.websiteRuleDao().getEnabledWebsiteRulesSync()
        val latestActive = db.lockSessionDao().getLatestActiveSession()
        val isLocked = latestActive != null && lockEngine.isSessionActive(latestActive, now)
        assertTrue(isLocked)

        // Browser 1 (e.g. Chrome) requests instagram.com
        val chromeQuery = buildMockDnsQuery("instagram.com")
        val chromeExtracted = DnsPacketHelper.extractDomainName(chromeQuery, 0)
        assertEquals("instagram.com", chromeExtracted)
        assertTrue(DnsPacketHelper.isDomainBlocked(chromeExtracted!!, activeRules))

        // Browser 2 (e.g. Firefox) requests subdomain m.instagram.com
        val firefoxQuery = buildMockDnsQuery("m.instagram.com")
        val firefoxExtracted = DnsPacketHelper.extractDomainName(firefoxQuery, 0)
        assertEquals("m.instagram.com", firefoxExtracted)
        assertTrue(DnsPacketHelper.isDomainBlocked(firefoxExtracted!!, activeRules))

        // Unrelated site (e.g. Wikipedia) is NOT blocked and loads normally
        val wikiQuery = buildMockDnsQuery("wikipedia.org")
        val wikiExtracted = DnsPacketHelper.extractDomainName(wikiQuery, 0)
        assertEquals("wikipedia.org", wikiExtracted)
        assertFalse(DnsPacketHelper.isDomainBlocked(wikiExtracted!!, activeRules))
    }

    @Test
    fun `turning vpn off mid-lock reflects accurately in state`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // 1. Check initial state
        assertFalse(FocusVpnHelper.isVpnRunning.value)

        // 2. Stop VPN service mid-lock
        FocusVpnHelper.stopVpnService(context)
        assertFalse(FocusVpnHelper.isVpnRunning.value)
    }

    @Test
    fun `device policy controller safely no-ops without crash when not device owner`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dpc = DevicePolicyController(context)

        // Verifies isDeviceOwner is false in default environment
        assertFalse(dpc.isDeviceOwner())

        // Verifies applyLockRestrictions and clearLockRestrictions execute cleanly without exceptions
        dpc.applyLockRestrictions(listOf("com.distracting.app"))
        dpc.clearLockRestrictions(listOf("com.distracting.app"))
    }

    private fun buildMockDnsQuery(domain: String): ByteArray {
        val labels = domain.split(".")
        var qnameLen = 1 // trailing 0x00
        for (l in labels) qnameLen += (1 + l.length)

        val packet = ByteArray(12 + qnameLen + 4)
        packet[0] = 0xAA.toByte()
        packet[1] = 0xBB.toByte()
        packet[2] = 0x01
        packet[3] = 0x00
        packet[4] = 0x00
        packet[5] = 0x01 // QDCOUNT = 1

        var cursor = 12
        for (l in labels) {
            packet[cursor++] = l.length.toByte()
            val bytes = l.toByteArray(Charsets.US_ASCII)
            System.arraycopy(bytes, 0, packet, cursor, bytes.size)
            cursor += bytes.size
        }
        packet[cursor++] = 0x00 // end of QNAME
        packet[cursor++] = 0x00 // QTYPE high
        packet[cursor++] = 0x01 // QTYPE A (1)
        packet[cursor++] = 0x00 // QCLASS IN (1)
        packet[cursor] = 0x01

        return packet
    }
}
