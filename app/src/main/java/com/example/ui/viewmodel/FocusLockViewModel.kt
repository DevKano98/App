package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.FocusLockApplication
import com.example.data.model.AppRule
import com.example.data.model.InstalledAppInfo
import com.example.data.model.LockSession
import com.example.data.model.Schedule
import com.example.data.model.SessionStatus
import com.example.data.model.WebsiteRule
import com.example.engine.NextLockInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.Calendar

data class FocusLockUiState(
    val activeSession: LockSession? = null,
    val isCurrentlyLocked: Boolean = false,
    val remainingMillis: Long = 0L,
    val remainingFormatted: String = "00:00",
    val nextLockInfo: NextLockInfo? = null,
    val todayProtectedTimeMillis: Long = 0L,
    val todayProtectedFormatted: String = "0m",
    val appRules: List<AppRule> = emptyList(),
    val websiteRules: List<WebsiteRule> = emptyList(),
    val schedules: List<Schedule> = emptyList(),
    val historySessions: List<LockSession> = emptyList(),
    val installedApps: List<InstalledAppInfo> = emptyList(),
    val isLoadingInstalledApps: Boolean = false,
    val themeMode: String = "SYSTEM",
    val emergencyPassphrase: String = "END FOCUS SESSION",
    val strictMode: Boolean = true,
    val appBlockingEnabled: Boolean = false,
    val websiteBlockingEnabled: Boolean = false,
    val installProtectionEnabled: Boolean = false
)

class FocusLockViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as FocusLockApplication
    private val repository = app.repository
    private val installedAppsRepo = app.installedAppsRepository
    private val preferencesRepo = app.userPreferencesRepository
    private val lockEngine = app.lockEngine
    private val devicePolicyController = app.devicePolicyController

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    private val _isLoadingApps = MutableStateFlow(false)
    private val _currentTimeTicker = MutableStateFlow(System.currentTimeMillis())

    private data class DataBundle(
        val sessions: List<LockSession>,
        val appRules: List<AppRule>,
        val websiteRules: List<WebsiteRule>,
        val schedules: List<Schedule>
    )

    private data class PrefsBundle(
        val themeMode: String,
        val passphrase: String,
        val appBlocking: Boolean,
        val websiteBlocking: Boolean,
        val installProtection: Boolean
    )

    private data class AppsBundle(
        val apps: List<InstalledAppInfo>,
        val loading: Boolean
    )

    private val dataFlow = combine(
        repository.allLockSessions,
        repository.allAppRules,
        repository.allWebsiteRules,
        repository.allSchedules
    ) { sessions, appRules, websiteRules, schedules ->
        DataBundle(sessions, appRules, websiteRules, schedules)
    }

    private val prefsFlow = combine(
        preferencesRepo.themeMode,
        preferencesRepo.emergencyPassphrase,
        preferencesRepo.appBlockingEnabled,
        preferencesRepo.websiteBlockingEnabled,
        preferencesRepo.installProtectionEnabled
    ) { theme, pass, appBlock, webBlock, instBlock ->
        PrefsBundle(theme, pass, appBlock, webBlock, instBlock)
    }

    private val appsFlow = combine(
        _installedApps,
        _isLoadingApps
    ) { apps, loading ->
        AppsBundle(apps, loading)
    }

    val uiState: StateFlow<FocusLockUiState> = combine(
        dataFlow,
        prefsFlow,
        appsFlow,
        _currentTimeTicker
    ) { data, prefs, apps, currentTime ->
        val activeSessions = data.sessions.filter { it.status == SessionStatus.ACTIVE }
        var currentActiveSession: LockSession? = null

        for (session in activeSessions) {
            if (lockEngine.isSessionActive(session, currentTime)) {
                currentActiveSession = session
                break
            }
        }

        val isLocked = currentActiveSession != null
        val remainingMillis = if (currentActiveSession != null) {
            lockEngine.getRemainingMillis(currentActiveSession, currentTime)
        } else 0L

        val nextLock = lockEngine.calculateNextScheduledLock(data.schedules, currentTime)
        val todayProtectedMillis = lockEngine.calculateTodayProtectedTimeMillis(data.sessions, currentTime)

        FocusLockUiState(
            activeSession = currentActiveSession,
            isCurrentlyLocked = isLocked,
            remainingMillis = remainingMillis,
            remainingFormatted = lockEngine.formatRemainingTime(remainingMillis),
            nextLockInfo = nextLock,
            todayProtectedTimeMillis = todayProtectedMillis,
            todayProtectedFormatted = lockEngine.formatProtectedTime(todayProtectedMillis),
            appRules = data.appRules,
            websiteRules = data.websiteRules,
            schedules = data.schedules,
            historySessions = data.sessions.filter { it.status != SessionStatus.ACTIVE || !lockEngine.isSessionActive(it, currentTime) },
            installedApps = apps.apps,
            isLoadingInstalledApps = apps.loading,
            themeMode = prefs.themeMode,
            emergencyPassphrase = prefs.passphrase,
            appBlockingEnabled = prefs.appBlocking,
            websiteBlockingEnabled = prefs.websiteBlocking,
            installProtectionEnabled = prefs.installProtection
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FocusLockUiState()
    )

    init {
        // Start second ticker for smooth active countdown & real-time updates
        viewModelScope.launch(Dispatchers.Default) {
            while (isActive) {
                delay(1000)
                _currentTimeTicker.value = System.currentTimeMillis()
            }
        }

        // Initialize background alarms and clean stale records
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            repository.completeExpiredSessions(now)

            // Reschedule all enabled schedule alarms and verify active windows
            val enabledSchedules = repository.getEnabledSchedulesSync()
            lockEngine.rescheduleAllSchedules(enabledSchedules)

            // Check if app opened while inside an active schedule block
            for (schedule in enabledSchedules) {
                val window = lockEngine.isInsideScheduleWindow(schedule, now)
                if (window != null) {
                    val (startMillis, endMillis) = window
                    val latestActive = repository.getLatestActiveSession()
                    val hasActive = latestActive != null && lockEngine.isSessionActive(latestActive, now)
                    if (!hasActive) {
                        val enabledApps = repository.getEnabledAppRulesSync()
                        val enabledWebs = repository.getEnabledWebsiteRulesSync().size
                        val autoSession = LockSession(
                            name = schedule.name,
                            startedAt = startMillis,
                            endsAt = endMillis,
                            status = SessionStatus.ACTIVE,
                            blockedAppCount = enabledApps.size,
                            blockedWebsiteCount = enabledWebs
                        )
                        val id = repository.insertSession(autoSession)
                        lockEngine.scheduleSessionAlarms(autoSession.copy(id = id))
                        devicePolicyController.applyLockRestrictions(enabledApps.map { it.packageName })
                    }
                }
            }

            loadInstalledApps()
        }
    }

    fun loadInstalledApps() {
        if (_isLoadingApps.value) return
        viewModelScope.launch {
            _isLoadingApps.value = true
            val apps = installedAppsRepo.getLaunchableInstalledApps()
            _installedApps.value = apps
            _isLoadingApps.value = false
        }
    }

    fun startOneTimeLock(durationMinutes: Int, name: String = "One-Time Focus") {
        viewModelScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val endsAt = now + (durationMinutes * 60 * 1000L)

            val enabledApps = repository.getEnabledAppRulesSync()
            val enabledWebsites = repository.getEnabledWebsiteRulesSync().size

            val session = LockSession(
                name = name.ifBlank { "One-Time Focus" },
                startedAt = now,
                endsAt = endsAt,
                status = SessionStatus.ACTIVE,
                blockedAppCount = enabledApps.size,
                blockedWebsiteCount = enabledWebsites
            )

            val id = repository.insertSession(session)
            val created = session.copy(id = id)
            lockEngine.scheduleSessionAlarms(created)
            devicePolicyController.applyLockRestrictions(enabledApps.map { it.packageName })
        }
    }

    fun startCustomRangeLock(startEpochMillis: Long, endEpochMillis: Long, name: String = "Custom Focus") {
        viewModelScope.launch(Dispatchers.IO) {
            val enabledApps = repository.getEnabledAppRulesSync()
            val enabledWebsites = repository.getEnabledWebsiteRulesSync().size

            val session = LockSession(
                name = name.ifBlank { "Custom Focus" },
                startedAt = startEpochMillis,
                endsAt = endEpochMillis,
                status = SessionStatus.ACTIVE,
                blockedAppCount = enabledApps.size,
                blockedWebsiteCount = enabledWebsites
            )

            val id = repository.insertSession(session)
            val created = session.copy(id = id)
            lockEngine.scheduleSessionAlarms(created)
            devicePolicyController.applyLockRestrictions(enabledApps.map { it.packageName })
        }
    }

    fun emergencyCancelSession(sessionId: Long, enteredPassphrase: String): Boolean {
        val targetPassphrase = uiState.value.emergencyPassphrase
        if (enteredPassphrase.trim().equals(targetPassphrase.trim(), ignoreCase = false)) {
            viewModelScope.launch(Dispatchers.IO) {
                repository.updateSessionStatus(sessionId, SessionStatus.EMERGENCY_OVERRIDE)
                lockEngine.cancelSessionAlarms(sessionId)
                val enabledApps = repository.getEnabledAppRulesSync()
                devicePolicyController.clearLockRestrictions(enabledApps.map { it.packageName })
            }
            return true
        }
        return false
    }

    fun addAppRules(selectedApps: List<InstalledAppInfo>) {
        viewModelScope.launch(Dispatchers.IO) {
            val existingPackages = uiState.value.appRules.map { it.packageName }.toSet()
            val newRules = selectedApps
                .filterNot { existingPackages.contains(it.packageName) }
                .map {
                    AppRule(
                        packageName = it.packageName,
                        displayName = it.appName,
                        enabled = true
                    )
                }
            if (newRules.isNotEmpty()) {
                repository.insertAppRules(newRules)
            }
        }
    }

    fun toggleAppRule(rule: AppRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAppRule(rule.copy(enabled = !rule.enabled))
        }
    }

    fun deleteAppRule(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteAppRuleById(id)
        }
    }

    fun addWebsiteRule(domain: String, includeSubdomains: Boolean) {
        val cleanDomain = domain.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .trimEnd('/')

        if (cleanDomain.isBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            repository.insertWebsiteRule(
                WebsiteRule(
                    domain = cleanDomain,
                    includeSubdomains = includeSubdomains,
                    enabled = true
                )
            )
        }
    }

    fun toggleWebsiteRule(rule: WebsiteRule) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateWebsiteRule(rule.copy(enabled = !rule.enabled))
        }
    }

    fun deleteWebsiteRule(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteWebsiteRuleById(id)
        }
    }

    fun saveSchedule(schedule: Schedule) {
        viewModelScope.launch(Dispatchers.IO) {
            val scheduleId = if (schedule.id == 0L) {
                repository.insertSchedule(schedule)
            } else {
                repository.updateSchedule(schedule)
                schedule.id
            }

            val savedSchedule = schedule.copy(id = scheduleId)
            val now = System.currentTimeMillis()

            if (savedSchedule.enabled) {
                lockEngine.scheduleScheduleAlarm(savedSchedule)

                // If saved schedule window is active right now, flip session to ACTIVE
                val window = lockEngine.isInsideScheduleWindow(savedSchedule, now)
                if (window != null) {
                    val (startMillis, endMillis) = window
                    val latestActive = repository.getLatestActiveSession()
                    val hasActive = latestActive != null && lockEngine.isSessionActive(latestActive, now)

                    if (!hasActive) {
                        val enabledApps = repository.getEnabledAppRulesSync()
                        val enabledWebs = repository.getEnabledWebsiteRulesSync().size
                        val session = LockSession(
                            name = savedSchedule.name,
                            startedAt = startMillis,
                            endsAt = endMillis,
                            status = SessionStatus.ACTIVE,
                            blockedAppCount = enabledApps.size,
                            blockedWebsiteCount = enabledWebs
                        )
                        val createdId = repository.insertSession(session)
                        lockEngine.scheduleSessionAlarms(session.copy(id = createdId))
                        devicePolicyController.applyLockRestrictions(enabledApps.map { it.packageName })
                    }
                }
            } else {
                lockEngine.cancelScheduleAlarm(scheduleId)
            }
        }
    }

    fun toggleSchedule(schedule: Schedule) {
        val updated = schedule.copy(enabled = !schedule.enabled)
        saveSchedule(updated)
    }

    fun deleteSchedule(id: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            lockEngine.cancelScheduleAlarm(id)
            repository.deleteScheduleById(id)
        }
    }

    fun setThemeMode(mode: String) {
        viewModelScope.launch {
            preferencesRepo.setThemeMode(mode)
        }
    }

    fun setAppBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setAppBlockingEnabled(enabled)
        }
    }

    fun setWebsiteBlockingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setWebsiteBlockingEnabled(enabled)
        }
    }

    fun setInstallProtectionEnabled(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepo.setInstallProtectionEnabled(enabled)
        }
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAllSessions()
        }
    }
}
