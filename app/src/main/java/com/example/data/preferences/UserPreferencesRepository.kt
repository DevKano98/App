package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "focus_lock_preferences")

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "LIGHT", "DARK"
        val STRICT_MODE = booleanPreferencesKey("strict_mode")
        val SHOW_MOTIVATIONAL_QUOTES = booleanPreferencesKey("show_motivational_quotes")
        val VIBRATE_ON_BLOCK = booleanPreferencesKey("vibrate_on_block")
        val EMERGENCY_PASSPHRASE = stringPreferencesKey("emergency_passphrase")
        val APP_BLOCKING_ENABLED = booleanPreferencesKey("app_blocking_enabled")
        val WEBSITE_BLOCKING_ENABLED = booleanPreferencesKey("website_blocking_enabled")
        val INSTALL_PROTECTION_ENABLED = booleanPreferencesKey("install_protection_enabled")
    }

    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
    }

    val strictMode: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.STRICT_MODE] ?: true
    }

    val showMotivationalQuotes: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHOW_MOTIVATIONAL_QUOTES] ?: true
    }

    val vibrateOnBlock: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.VIBRATE_ON_BLOCK] ?: true
    }

    val emergencyPassphrase: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EMERGENCY_PASSPHRASE] ?: "END FOCUS SESSION"
    }

    val appBlockingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.APP_BLOCKING_ENABLED] ?: false
    }

    val websiteBlockingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.WEBSITE_BLOCKING_ENABLED] ?: false
    }

    val installProtectionEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.INSTALL_PROTECTION_ENABLED] ?: false
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode
        }
    }

    suspend fun setStrictMode(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.STRICT_MODE] = enabled
        }
    }

    suspend fun setShowMotivationalQuotes(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHOW_MOTIVATIONAL_QUOTES] = enabled
        }
    }

    suspend fun setVibrateOnBlock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.VIBRATE_ON_BLOCK] = enabled
        }
    }

    suspend fun setAppBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_BLOCKING_ENABLED] = enabled
        }
    }

    suspend fun setWebsiteBlockingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.WEBSITE_BLOCKING_ENABLED] = enabled
        }
    }

    suspend fun setInstallProtectionEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INSTALL_PROTECTION_ENABLED] = enabled
        }
    }
}
