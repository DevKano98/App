package com.example.engine

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.FocusDayOfWeek
import com.example.data.model.LockSession
import com.example.data.model.Schedule
import com.example.data.model.SessionStatus
import com.example.receiver.LockAlarmReceiver
import java.util.Calendar

data class NextLockInfo(
    val scheduleName: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long
)

class LockEngine(private val context: Context) {

    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    /**
     * Pure function: Computes whether a given lock session is actively running at [currentTime].
     */
    fun isSessionActive(session: LockSession?, currentTime: Long = System.currentTimeMillis()): Boolean {
        if (session == null) return false
        if (session.status != SessionStatus.ACTIVE) return false
        return currentTime in session.startedAt..session.endsAt
    }

    /**
     * Pure function: Computes remaining time in milliseconds for an active session.
     */
    fun getRemainingMillis(session: LockSession?, currentTime: Long = System.currentTimeMillis()): Long {
        if (session == null || !isSessionActive(session, currentTime)) return 0L
        return (session.endsAt - currentTime).coerceAtLeast(0L)
    }

    /**
     * Pure function: Formats milliseconds into HH:mm:ss or mm:ss string.
     */
    fun formatRemainingTime(millis: Long): String {
        val totalSeconds = (millis / 1000).coerceAtLeast(0L)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Pure function: Computes total protected time in milliseconds completed or actively running today.
     */
    fun calculateTodayProtectedTimeMillis(
        sessions: List<LockSession>,
        currentTime: Long = System.currentTimeMillis()
    ): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTime
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfDay = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        val endOfDay = calendar.timeInMillis

        var totalMillis = 0L

        for (session in sessions) {
            val sessionStart = session.startedAt.coerceAtLeast(startOfDay)
            val sessionEnd = when (session.status) {
                SessionStatus.ACTIVE -> currentTime.coerceAtMost(session.endsAt).coerceAtMost(endOfDay)
                SessionStatus.COMPLETED, SessionStatus.EMERGENCY_OVERRIDE, SessionStatus.CANCELLED ->
                    session.endsAt.coerceAtMost(endOfDay)
            }

            if (sessionEnd > sessionStart) {
                totalMillis += (sessionEnd - sessionStart)
            }
        }

        return totalMillis
    }

    /**
     * Pure function: Formats total protected time into human-readable string (e.g. "2h 45m" or "35m").
     */
    fun formatProtectedTime(millis: Long): String {
        val totalMinutes = millis / (1000 * 60)
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
            hours > 0 -> "${hours}h"
            minutes > 0 -> "${minutes}m"
            millis > 0 -> "< 1m"
            else -> "0m"
        }
    }

    /**
     * Computes the next scheduled lock time from enabled schedules.
     */
    fun calculateNextScheduledLock(
        schedules: List<Schedule>,
        currentTime: Long = System.currentTimeMillis()
    ): NextLockInfo? {
        val enabledSchedules = schedules.filter { it.enabled && it.repeatDays.isNotEmpty() }
        if (enabledSchedules.isEmpty()) return null

        var nearestNextLock: NextLockInfo? = null

        for (schedule in enabledSchedules) {
            val nextOccurrence = getNextScheduleStartOccurrence(schedule, currentTime) ?: continue
            if (nearestNextLock == null || nextOccurrence.startTimeMillis < nearestNextLock.startTimeMillis) {
                nearestNextLock = nextOccurrence
            }
        }

        return nearestNextLock
    }

    /**
     * Checks if [currentTime] is within an active occurrence window for [schedule].
     * Returns Pair(windowStartMillis, windowEndMillis) if currently inside, else null.
     */
    fun isInsideScheduleWindow(schedule: Schedule, currentTime: Long): Pair<Long, Long>? {
        if (!schedule.enabled || schedule.repeatDays.isEmpty()) return null
        val (startH, startM) = parseTime(schedule.startTime) ?: return null
        val (endH, endM) = parseTime(schedule.endTime) ?: return null

        // Check if today matches or if yesterday started an overnight session that continues into today
        for (dayOffset in listOf(0, -1)) {
            val startCal = Calendar.getInstance().apply {
                timeInMillis = currentTime
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, startH)
                set(Calendar.MINUTE, startM)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = FocusDayOfWeek.fromCalendarDay(startCal.get(Calendar.DAY_OF_WEEK))
            if (schedule.repeatDays.contains(dayOfWeek)) {
                val startMillis = startCal.timeInMillis
                val endCal = (startCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, endH)
                    set(Calendar.MINUTE, endM)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (endH < startH || (endH == startH && endM <= startM)) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                val endMillis = endCal.timeInMillis

                if (currentTime in startMillis until endMillis) {
                    return Pair(startMillis, endMillis)
                }
            }
        }
        return null
    }

    /**
     * Calculates the next future occurrence timestamp (starting strictly after [currentTime]) for a schedule.
     */
    fun getNextScheduleStartOccurrence(schedule: Schedule, currentTime: Long): NextLockInfo? {
        if (!schedule.enabled || schedule.repeatDays.isEmpty()) return null
        val (startH, startM) = parseTime(schedule.startTime) ?: return null
        val (endH, endM) = parseTime(schedule.endTime) ?: return null

        // Look ahead over next 8 days
        for (dayOffset in 0..7) {
            val candidateCal = Calendar.getInstance().apply {
                timeInMillis = currentTime
                add(Calendar.DAY_OF_YEAR, dayOffset)
                set(Calendar.HOUR_OF_DAY, startH)
                set(Calendar.MINUTE, startM)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val dayOfWeek = FocusDayOfWeek.fromCalendarDay(candidateCal.get(Calendar.DAY_OF_WEEK))
            if (schedule.repeatDays.contains(dayOfWeek)) {
                val startMillis = candidateCal.timeInMillis

                val endCal = (candidateCal.clone() as Calendar).apply {
                    set(Calendar.HOUR_OF_DAY, endH)
                    set(Calendar.MINUTE, endM)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (endH < startH || (endH == startH && endM <= startM)) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                val endMillis = endCal.timeInMillis

                // Return first candidate start in future
                if (startMillis > currentTime) {
                    return NextLockInfo(
                        scheduleName = schedule.name,
                        startTimeMillis = startMillis,
                        endTimeMillis = endMillis
                    )
                }
            }
        }

        return null
    }

    private fun parseTime(timeStr: String): Pair<Int, Int>? {
        val parts = timeStr.split(":")
        if (parts.size != 2) return null
        val hour = parts[0].trim().toIntOrNull() ?: return null
        val min = parts[1].trim().toIntOrNull() ?: return null
        return Pair(hour, min)
    }

    /**
     * Schedules exact alarm for a recurring [Schedule].
     */
    fun scheduleScheduleAlarm(schedule: Schedule) {
        if (alarmManager == null || !schedule.enabled) return
        val currentTime = System.currentTimeMillis()
        val nextOccurrence = getNextScheduleStartOccurrence(schedule, currentTime) ?: return

        val intent = Intent(context, LockAlarmReceiver::class.java).apply {
            action = LockAlarmReceiver.ACTION_SCHEDULE_TRIGGER
            putExtra(LockAlarmReceiver.EXTRA_SCHEDULE_ID, schedule.id)
            putExtra(LockAlarmReceiver.EXTRA_SESSION_NAME, schedule.name)
            putExtra(LockAlarmReceiver.EXTRA_SCHEDULE_END_TIME, nextOccurrence.endTimeMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (100000L + schedule.id).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        setExactAlarm(nextOccurrence.startTimeMillis, pendingIntent)
    }

    /**
     * Cancels any pending alarm for a [Schedule].
     */
    fun cancelScheduleAlarm(scheduleId: Long) {
        if (alarmManager == null) return
        val intent = Intent(context, LockAlarmReceiver::class.java).apply {
            action = LockAlarmReceiver.ACTION_SCHEDULE_TRIGGER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            (100000L + scheduleId).toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    /**
     * Reschedules alarms for all enabled schedules.
     */
    fun rescheduleAllSchedules(schedules: List<Schedule>) {
        for (schedule in schedules) {
            if (schedule.enabled) {
                scheduleScheduleAlarm(schedule)
            } else {
                cancelScheduleAlarm(schedule.id)
            }
        }
    }

    /**
     * Schedules exact alarm for a lock session start/end.
     */
    fun scheduleSessionAlarms(session: LockSession) {
        if (alarmManager == null) return

        val currentTime = System.currentTimeMillis()

        // Schedule start alarm if start is in future
        if (session.startedAt > currentTime) {
            val startIntent = Intent(context, LockAlarmReceiver::class.java).apply {
                action = LockAlarmReceiver.ACTION_LOCK_START
                putExtra(LockAlarmReceiver.EXTRA_SESSION_ID, session.id)
                putExtra(LockAlarmReceiver.EXTRA_SESSION_NAME, session.name)
            }
            val startPendingIntent = PendingIntent.getBroadcast(
                context,
                (session.id * 10).toInt(),
                startIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(session.startedAt, startPendingIntent)
        }

        // Schedule end alarm
        if (session.endsAt > currentTime) {
            val endIntent = Intent(context, LockAlarmReceiver::class.java).apply {
                action = LockAlarmReceiver.ACTION_LOCK_END
                putExtra(LockAlarmReceiver.EXTRA_SESSION_ID, session.id)
                putExtra(LockAlarmReceiver.EXTRA_SESSION_NAME, session.name)
            }
            val endPendingIntent = PendingIntent.getBroadcast(
                context,
                (session.id * 10 + 1).toInt(),
                endIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            setExactAlarm(session.endsAt, endPendingIntent)
        }
    }

    /**
     * Cancels alarms for a session.
     */
    fun cancelSessionAlarms(sessionId: Long) {
        if (alarmManager == null) return

        val startIntent = Intent(context, LockAlarmReceiver::class.java).apply {
            action = LockAlarmReceiver.ACTION_LOCK_START
        }
        val startPendingIntent = PendingIntent.getBroadcast(
            context,
            (sessionId * 10).toInt(),
            startIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (startPendingIntent != null) {
            alarmManager.cancel(startPendingIntent)
            startPendingIntent.cancel()
        }

        val endIntent = Intent(context, LockAlarmReceiver::class.java).apply {
            action = LockAlarmReceiver.ACTION_LOCK_END
        }
        val endPendingIntent = PendingIntent.getBroadcast(
            context,
            (sessionId * 10 + 1).toInt(),
            endIntent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (endPendingIntent != null) {
            alarmManager.cancel(endPendingIntent)
            endPendingIntent.cancel()
        }
    }

    private fun setExactAlarm(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (alarmManager == null) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // Fallback if exact alarm permission requires user consent on certain vendor ROMs
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }
}
