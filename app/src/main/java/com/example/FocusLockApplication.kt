package com.example

import android.app.Application
import com.example.admin.DevicePolicyController
import com.example.data.local.FocusLockDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FocusLockRepository
import com.example.data.repository.InstalledAppsRepository
import com.example.engine.LockEngine

class FocusLockApplication : Application() {

    val database: FocusLockDatabase by lazy { FocusLockDatabase.getInstance(this) }

    val repository: FocusLockRepository by lazy {
        FocusLockRepository(
            appRuleDao = database.appRuleDao(),
            websiteRuleDao = database.websiteRuleDao(),
            scheduleDao = database.scheduleDao(),
            lockSessionDao = database.lockSessionDao()
        )
    }

    val installedAppsRepository: InstalledAppsRepository by lazy {
        InstalledAppsRepository(this)
    }

    val userPreferencesRepository: UserPreferencesRepository by lazy {
        UserPreferencesRepository(this)
    }

    val lockEngine: LockEngine by lazy {
        LockEngine(this)
    }

    val devicePolicyController: DevicePolicyController by lazy {
        DevicePolicyController(this)
    }

    override fun onCreate() {
        super.onCreate()
    }
}
