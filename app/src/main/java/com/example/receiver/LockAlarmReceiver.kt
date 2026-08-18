package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.admin.DevicePolicyController
import com.example.data.local.FocusLockDatabase
import com.example.data.model.LockSession
import com.example.data.model.SessionStatus
import com.example.engine.LockEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LockAlarmReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_LOCK_START = "com.example.focuslock.ACTION_LOCK_START"
        const val ACTION_LOCK_END = "com.example.focuslock.ACTION_LOCK_END"
        const val ACTION_SCHEDULE_TRIGGER = "com.example.focuslock.ACTION_SCHEDULE_TRIGGER"

        const val EXTRA_SESSION_ID = "extra_session_id"
        const val EXTRA_SESSION_NAME = "extra_session_name"
        const val EXTRA_SCHEDULE_ID = "extra_schedule_id"
        const val EXTRA_SCHEDULE_END_TIME = "extra_schedule_end_time"

        const val CHANNEL_ID = "focus_lock_channel"
        const val NOTIFICATION_ID_START = 1001
        const val NOTIFICATION_ID_END = 1002
        const val NOTIFICATION_ID_SCHEDULE = 1003
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        createNotificationChannel(context)

        val db = FocusLockDatabase.getInstance(context)
        val lockEngine = LockEngine(context)
        val devicePolicyController = DevicePolicyController(context)

        CoroutineScope(Dispatchers.IO).launch {
            val now = System.currentTimeMillis()

            when (action) {
                ACTION_SCHEDULE_TRIGGER -> {
                    val scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L)
                    val scheduledEndTime = intent.getLongExtra(EXTRA_SCHEDULE_END_TIME, now + (30 * 60 * 1000L))
                    val schedule = if (scheduleId > 0) db.scheduleDao().getScheduleById(scheduleId) else null

                    if (schedule != null && schedule.enabled) {
                        // Check if an active session is already running
                        val latestActive = db.lockSessionDao().getLatestActiveSession()
                        val isAlreadyLocked = latestActive != null && lockEngine.isSessionActive(latestActive, now)

                        if (!isAlreadyLocked) {
                            val enabledApps = db.appRuleDao().getEnabledAppRulesSync()
                            val enabledWebsitesCount = db.websiteRuleDao().getEnabledWebsiteRulesSync().size

                            val newSession = LockSession(
                                name = schedule.name,
                                startedAt = now,
                                endsAt = scheduledEndTime.coerceAtLeast(now + 60_000L),
                                status = SessionStatus.ACTIVE,
                                blockedAppCount = enabledApps.size,
                                blockedWebsiteCount = enabledWebsitesCount
                            )

                            val createdId = db.lockSessionDao().insertSession(newSession)
                            val createdSession = newSession.copy(id = createdId)
                            lockEngine.scheduleSessionAlarms(createdSession)

                            // Apply Device Owner restrictions if provisioned
                            devicePolicyController.applyLockRestrictions(enabledApps.map { it.packageName })
                        }

                        // Always schedule the next recurrence for this schedule
                        lockEngine.scheduleScheduleAlarm(schedule)

                        showNotification(
                            context = context,
                            notificationId = NOTIFICATION_ID_SCHEDULE,
                            title = "Scheduled Lock: ${schedule.name}",
                            message = "Your scheduled focus block is now active."
                        )
                    }
                }

                ACTION_LOCK_START -> {
                    val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                    val sessionName = intent.getStringExtra(EXTRA_SESSION_NAME) ?: "Focus Session"
                    if (sessionId > 0) {
                        db.lockSessionDao().updateSessionStatus(sessionId, SessionStatus.ACTIVE)
                    }

                    val enabledApps = db.appRuleDao().getEnabledAppRulesSync()
                    devicePolicyController.applyLockRestrictions(enabledApps.map { it.packageName })

                    showNotification(
                        context = context,
                        notificationId = NOTIFICATION_ID_START,
                        title = "FocusLock Active",
                        message = "Session \"$sessionName\" is now locked. Stay focused."
                    )
                }

                ACTION_LOCK_END -> {
                    val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
                    val sessionName = intent.getStringExtra(EXTRA_SESSION_NAME) ?: "Focus Session"
                    if (sessionId > 0) {
                        db.lockSessionDao().updateSessionStatus(sessionId, SessionStatus.COMPLETED)
                    } else {
                        db.lockSessionDao().completeExpiredSessions(now)
                    }

                    val enabledApps = db.appRuleDao().getEnabledAppRulesSync()
                    devicePolicyController.clearLockRestrictions(enabledApps.map { it.packageName })

                    showNotification(
                        context = context,
                        notificationId = NOTIFICATION_ID_END,
                        title = "Focus Session Complete",
                        message = "\"$sessionName\" has concluded. Great work."
                    )
                }
            }
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Focus Sessions"
            val descriptionText = "Notifications for active lock sessions and schedule events"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showNotification(context: Context, notificationId: Int, title: String, message: String) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            notificationManager.notify(notificationId, notification)
        } catch (e: Exception) {
            // Permission or system error
        }
    }
}
