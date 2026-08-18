package com.example.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.data.local.FocusLockDatabase
import com.example.engine.LockEngine
import com.example.ui.blocked.BlockedAppActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AppAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastBlockedPackage: String? = null
    private var lastBlockedTimestamp: Long = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            return
        }

        // Strictly inspect only the foreground package name — no window content, keystrokes, or text
        val pkgName = event.packageName?.toString() ?: return

        // Skip our own package, system UI, and system keyboard
        if (pkgName == applicationContext.packageName ||
            pkgName == "com.android.systemui" ||
            pkgName == "android" ||
            pkgName.startsWith("com.google.android.inputmethod")
        ) {
            return
        }

        // Throttle rapid duplicate events for the same window
        val now = System.currentTimeMillis()
        if (pkgName == lastBlockedPackage && (now - lastBlockedTimestamp) < 1200L) {
            return
        }

        val db = FocusLockDatabase.getInstance(applicationContext)
        val lockEngine = LockEngine(applicationContext)

        serviceScope.launch {
            val activeSession = db.lockSessionDao().getLatestActiveSession()
            if (activeSession != null && lockEngine.isSessionActive(activeSession, now)) {
                val rule = db.appRuleDao().getAppRuleByPackageName(pkgName)
                if (rule != null && rule.enabled) {
                    lastBlockedPackage = pkgName
                    lastBlockedTimestamp = now

                    // Send to Home first to close the restricted app
                    performGlobalAction(GLOBAL_ACTION_HOME)

                    // Launch the full-screen Blocked screen Activity
                    val blockedIntent = Intent(applicationContext, BlockedAppActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                Intent.FLAG_ACTIVITY_SINGLE_TOP
                        putExtra(BlockedAppActivity.EXTRA_APP_NAME, rule.displayName)
                        putExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME, rule.packageName)
                        putExtra(BlockedAppActivity.EXTRA_ENDS_AT, activeSession.endsAt)
                    }
                    startActivity(blockedIntent)
                }
            }
        }
    }

    override fun onInterrupt() {
        // No-op
    }
}
