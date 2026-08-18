package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.activelock.ActiveLockScreen
import com.example.ui.history.HistoryScreen
import com.example.ui.home.HomeScreen
import com.example.ui.navigation.Screen
import com.example.ui.rules.RulesScreen
import com.example.ui.rules.ScheduleBuilderScreen
import com.example.ui.settings.SettingsScreen
import com.example.ui.theme.BorderLight
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.FocusLockViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: FocusLockViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            val isDarkTheme = when (uiState.themeMode) {
                "LIGHT" -> false
                "DARK" -> true
                else -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = isDarkTheme) {
                FocusLockApp(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun FocusLockApp(
    viewModel: FocusLockViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val shouldShowBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Rules.route,
        Screen.History.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .navigationBarsPadding()
                            .testTag("main_bottom_nav")
                    ) {
                        Screen.bottomNavItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title,
                                        modifier = Modifier.size(20.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 11.sp
                                        )
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                    indicatorColor = Color.Transparent,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${screen.route}")
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    uiState = uiState,
                    onStartDurationLock = { duration, name ->
                        viewModel.startOneTimeLock(duration, name)
                    },
                    onStartTimeRangeLock = { startMillis, endMillis, name ->
                        viewModel.startCustomRangeLock(startMillis, endMillis, name)
                    },
                    onNavigateToActiveLock = {
                        navController.navigate(Screen.ActiveLock.route)
                    },
                    onNavigateToRules = {
                        navController.navigate(Screen.Rules.route)
                    }
                )
            }

            composable(Screen.Rules.route) {
                RulesScreen(
                    uiState = uiState,
                    onToggleAppRule = viewModel::toggleAppRule,
                    onDeleteAppRule = viewModel::deleteAppRule,
                    onAddAppRules = viewModel::addAppRules,
                    onToggleWebsiteRule = viewModel::toggleWebsiteRule,
                    onDeleteWebsiteRule = viewModel::deleteWebsiteRule,
                    onAddWebsiteRule = viewModel::addWebsiteRule,
                    onToggleSchedule = viewModel::toggleSchedule,
                    onDeleteSchedule = viewModel::deleteSchedule,
                    onNavigateToScheduleBuilder = {
                        navController.navigate(Screen.ScheduleBuilder.route)
                    }
                )
            }

            composable(Screen.History.route) {
                HistoryScreen(
                    uiState = uiState,
                    onClearHistory = viewModel::clearHistory
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    uiState = uiState,
                    onSetThemeMode = viewModel::setThemeMode,
                    onToggleAppBlocking = viewModel::setAppBlockingEnabled,
                    onToggleWebsiteBlocking = viewModel::setWebsiteBlockingEnabled,
                    onToggleInstallProtection = viewModel::setInstallProtectionEnabled
                )
            }

            composable(Screen.ActiveLock.route) {
                ActiveLockScreen(
                    uiState = uiState,
                    onBack = { navController.popBackStack() },
                    onEmergencyOverride = { sessionId, passphrase ->
                        viewModel.emergencyCancelSession(sessionId, passphrase)
                    }
                )
            }

            composable(Screen.ScheduleBuilder.route) {
                ScheduleBuilderScreen(
                    appRules = uiState.appRules,
                    websiteRules = uiState.websiteRules,
                    onBack = { navController.popBackStack() },
                    onSaveSchedule = viewModel::saveSchedule
                )
            }
        }
    }
}
