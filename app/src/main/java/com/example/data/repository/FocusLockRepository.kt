package com.example.data.repository

import com.example.data.local.AppRuleDao
import com.example.data.local.LockSessionDao
import com.example.data.local.ScheduleDao
import com.example.data.local.WebsiteRuleDao
import com.example.data.model.AppRule
import com.example.data.model.LockSession
import com.example.data.model.Schedule
import com.example.data.model.SessionStatus
import com.example.data.model.WebsiteRule
import kotlinx.coroutines.flow.Flow

class FocusLockRepository(
    private val appRuleDao: AppRuleDao,
    private val websiteRuleDao: WebsiteRuleDao,
    private val scheduleDao: ScheduleDao,
    private val lockSessionDao: LockSessionDao
) {
    // App Rules
    val allAppRules: Flow<List<AppRule>> = appRuleDao.getAllAppRules()
    val enabledAppRules: Flow<List<AppRule>> = appRuleDao.getEnabledAppRules()

    suspend fun getEnabledAppRulesSync(): List<AppRule> = appRuleDao.getEnabledAppRulesSync()
    suspend fun getAppRuleById(id: Long): AppRule? = appRuleDao.getAppRuleById(id)
    suspend fun getAppRulesByIds(ids: List<Long>): List<AppRule> = appRuleDao.getAppRulesByIds(ids)
    suspend fun insertAppRule(appRule: AppRule): Long = appRuleDao.insertAppRule(appRule)
    suspend fun insertAppRules(appRules: List<AppRule>): List<Long> = appRuleDao.insertAppRules(appRules)
    suspend fun updateAppRule(appRule: AppRule) = appRuleDao.updateAppRule(appRule)
    suspend fun deleteAppRule(appRule: AppRule) = appRuleDao.deleteAppRule(appRule)
    suspend fun deleteAppRuleById(id: Long) = appRuleDao.deleteAppRuleById(id)

    // Website Rules
    val allWebsiteRules: Flow<List<WebsiteRule>> = websiteRuleDao.getAllWebsiteRules()
    val enabledWebsiteRules: Flow<List<WebsiteRule>> = websiteRuleDao.getEnabledWebsiteRules()

    suspend fun getEnabledWebsiteRulesSync(): List<WebsiteRule> = websiteRuleDao.getEnabledWebsiteRulesSync()

    suspend fun getWebsiteRuleById(id: Long): WebsiteRule? = websiteRuleDao.getWebsiteRuleById(id)
    suspend fun getWebsiteRulesByIds(ids: List<Long>): List<WebsiteRule> = websiteRuleDao.getWebsiteRulesByIds(ids)
    suspend fun insertWebsiteRule(websiteRule: WebsiteRule): Long = websiteRuleDao.insertWebsiteRule(websiteRule)
    suspend fun updateWebsiteRule(websiteRule: WebsiteRule) = websiteRuleDao.updateWebsiteRule(websiteRule)
    suspend fun deleteWebsiteRule(websiteRule: WebsiteRule) = websiteRuleDao.deleteWebsiteRule(websiteRule)
    suspend fun deleteWebsiteRuleById(id: Long) = websiteRuleDao.deleteWebsiteRuleById(id)

    // Schedules
    val allSchedules: Flow<List<Schedule>> = scheduleDao.getAllSchedules()
    val enabledSchedules: Flow<List<Schedule>> = scheduleDao.getEnabledSchedules()

    suspend fun getEnabledSchedulesSync(): List<Schedule> = scheduleDao.getEnabledSchedulesSync()
    suspend fun getScheduleById(id: Long): Schedule? = scheduleDao.getScheduleById(id)
    suspend fun insertSchedule(schedule: Schedule): Long = scheduleDao.insertSchedule(schedule)
    suspend fun updateSchedule(schedule: Schedule) = scheduleDao.updateSchedule(schedule)
    suspend fun deleteSchedule(schedule: Schedule) = scheduleDao.deleteSchedule(schedule)
    suspend fun deleteScheduleById(id: Long) = scheduleDao.deleteScheduleById(id)

    // Lock Sessions
    val allLockSessions: Flow<List<LockSession>> = lockSessionDao.getAllLockSessions()

    fun getActiveSessionFlow(currentTime: Long): Flow<LockSession?> =
        lockSessionDao.getActiveSessionFlow(currentTime)

    suspend fun getLatestActiveSession(): LockSession? = lockSessionDao.getLatestActiveSession()
    suspend fun getSessionById(id: Long): LockSession? = lockSessionDao.getSessionById(id)
    suspend fun insertSession(session: LockSession): Long = lockSessionDao.insertSession(session)
    suspend fun updateSession(session: LockSession) = lockSessionDao.updateSession(session)
    suspend fun updateSessionStatus(id: Long, newStatus: SessionStatus) =
        lockSessionDao.updateSessionStatus(id, newStatus)
    suspend fun completeExpiredSessions(currentTime: Long): Int =
        lockSessionDao.completeExpiredSessions(currentTime)
    suspend fun clearAllSessions() = lockSessionDao.clearAllSessions()
    suspend fun getSessionsForDaySync(startOfDay: Long, endOfDay: Long): List<LockSession> =
        lockSessionDao.getSessionsForDaySync(startOfDay, endOfDay)
}
