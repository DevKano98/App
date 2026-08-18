package com.example.ui.blocked

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.FocusLockApplication
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BlockedAppActivity : ComponentActivity() {

    companion object {
        const val EXTRA_APP_NAME = "extra_app_name"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_ENDS_AT = "extra_ends_at"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appName = intent.getStringExtra(EXTRA_APP_NAME) ?: "This app"
        val endsAt = intent.getLongExtra(EXTRA_ENDS_AT, 0L)

        val app = application as FocusLockApplication
        val themeMode = runBlocking {
            app.userPreferencesRepository.themeMode.first()
        }

        setContent {
            val isDarkTheme = when (themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                BlockedScreen(
                    appName = appName,
                    endsAtMillis = endsAt,
                    onGoBack = {
                        finish()
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
