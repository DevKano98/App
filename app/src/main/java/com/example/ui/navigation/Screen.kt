package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Rule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Rules : Screen(
        route = "rules",
        title = "Rules",
        selectedIcon = Icons.Filled.Rule,
        unselectedIcon = Icons.Outlined.Rule
    )

    data object History : Screen(
        route = "history",
        title = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object ActiveLock : Screen(
        route = "active_lock",
        title = "Active Lock",
        selectedIcon = Icons.Filled.Block,
        unselectedIcon = Icons.Outlined.Block
    )

    data object ScheduleBuilder : Screen(
        route = "schedule_builder",
        title = "New Schedule",
        selectedIcon = Icons.Filled.Rule,
        unselectedIcon = Icons.Outlined.Rule
    )

    companion object {
        val bottomNavItems: List<Screen>
            get() = listOf(Home, Rules, History, Settings)
    }
}
