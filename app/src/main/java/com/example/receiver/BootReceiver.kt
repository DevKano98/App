package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.local.FocusLockDatabase
import com.example.data.model.LockSession
import com.example.data.model.SessionStatus
import com.example.engine.LockEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == Intent.ACTION_TIMEZONE_CHANGED ||
            action == Intent.ACTION_TIME_CHANGED
        ) {
            val db = FocusLockDatabase.getInstance(context)
            val lockEngine = LockEngine(context)

            CoroutineScope(Dispatchers.IO).launch {
                val now = System.currentTimeMillis()

                // 1. Mark any expired active sessions as COMPLETED
                db.lockSessionDao().completeExpiredSessions(now)

                // 2. Reschedule ongoing active session if present
                val activeSession = db.lockSessionDao().getLatestActiveSession()
                if (activeSession != null && lockEngine.isSessionActive(activeSession, now)) {
                    lockEngine.scheduleSessionAlarms(activeSession)
                }

                // 3. Load all enabled schedules
                val enabledSchedules = db.scheduleDao().getEnabledSchedulesSync()

                for (schedule in enabledSchedules) {
                    // Check if device rebooted while inside an active schedule window
                    val insideWindow = lockEngine.isInsideScheduleWindow(schedule, now)
                    if (insideWindow != null) {
                        val (startMillis, endMillis) = insideWindow
                        val currentActive = db.lockSessionDao().getLatestActiveSession()
                        val hasActive = currentActive != null && lockEngine.isSessionActive(currentActive, now)

                        if (!hasActive) {
                            val enabledApps = db.appRuleDao().getEnabledAppRulesSync().size
                            val enabledWebs = db.websiteRuleDao().getEnabledWebsiteRulesSync().size

                            val resumedSession = LockSession(
                                name = schedule.name,
                                startedAt = startMillis,
                                endsAt = endMillis,
                                status = SessionStatus.ACTIVE,
                                blockedAppCount = enabledApps,
                                blockedWebsiteCount = enabledWebs
                            )

                            val createdId = db.lockSessionDao().insertSession(resumedSession)
                            val created = resumedSession.copy(id = createdId)
                            lockEngine.scheduleSessionAlarms(created)
                        }
                    }

                    // Schedule next alarm occurrence for future triggers
                    lockEngine.scheduleScheduleAlarm(schedule)
                }
            }
        }
    }
}
