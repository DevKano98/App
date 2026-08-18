package com.example.ui.rules

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppRule
import com.example.data.model.FocusDayOfWeek
import com.example.data.model.InstalledAppInfo
import com.example.data.model.Schedule
import com.example.data.model.WebsiteRule
import com.example.ui.components.FocusCard
import com.example.ui.components.HairlineDivider
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SmallCardShape
import com.example.ui.theme.DangerRed
import com.example.ui.viewmodel.FocusLockUiState

@Composable
fun RulesScreen(
    uiState: FocusLockUiState,
    onToggleAppRule: (AppRule) -> Unit,
    onDeleteAppRule: (Long) -> Unit,
    onAddAppRules: (List<InstalledAppInfo>) -> Unit,
    onToggleWebsiteRule: (WebsiteRule) -> Unit,
    onDeleteWebsiteRule: (Long) -> Unit,
    onAddWebsiteRule: (String, Boolean) -> Unit,
    onToggleSchedule: (Schedule) -> Unit,
    onDeleteSchedule: (Long) -> Unit,
    onNavigateToScheduleBuilder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Schedules, 1 = Apps, 2 = Websites
    var showAddAppsDialog by remember { mutableStateOf(false) }
    var showAddWebsiteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = "Rules & Schedules",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Configure restrictions and automated focus windows",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Tabs
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = SmallCardShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf("Schedules", "Apps", "Websites")
                tabs.forEachIndexed { index, tabTitle ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(SmallCardShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.surface
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tabTitle,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Content by Tab
        when (selectedTab) {
            0 -> {
                // Schedules Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SCHEDULES (${uiState.schedules.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SecondaryButton(
                        text = "Add",
                        onClick = onNavigateToScheduleBuilder,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .width(88.dp)
                            .height(36.dp)
                            .testTag("add_schedule_button")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.schedules.isEmpty()) {
                    EmptySectionPlaceholder(
                        title = "No recurring schedules",
                        subtitle = "Create automated time blocks for weekdays or custom routines."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.schedules, key = { it.id }) { schedule ->
                            ScheduleItemCard(
                                schedule = schedule,
                                onToggle = { onToggleSchedule(schedule) },
                                onDelete = { onDeleteSchedule(schedule.id) }
                            )
                        }
                    }
                }
            }

            1 -> {
                // Apps Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RESTRICTED APPS (${uiState.appRules.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SecondaryButton(
                        text = "Add",
                        onClick = { showAddAppsDialog = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .width(88.dp)
                            .height(36.dp)
                            .testTag("add_apps_button")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.appRules.isEmpty()) {
                    EmptySectionPlaceholder(
                        title = "No restricted apps",
                        subtitle = "Select installed applications to restrict during active sessions."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.appRules, key = { it.id }) { rule ->
                            AppRuleItemCard(
                                rule = rule,
                                onToggle = { onToggleAppRule(rule) },
                                onDelete = { onDeleteAppRule(rule.id) }
                            )
                        }
                    }
                }
            }

            2 -> {
                // Websites Tab
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "WEBSITE RULES (${uiState.websiteRules.size})",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    SecondaryButton(
                        text = "Add",
                        onClick = { showAddWebsiteDialog = true },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .width(88.dp)
                            .height(36.dp)
                            .testTag("add_website_button")
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.websiteRules.isEmpty()) {
                    EmptySectionPlaceholder(
                        title = "No website rules",
                        subtitle = "Enter domains (e.g. reddit.com) to restrict during focus hours."
                    )
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(uiState.websiteRules, key = { it.id }) { rule ->
                            WebsiteRuleItemCard(
                                rule = rule,
                                onToggle = { onToggleWebsiteRule(rule) },
                                onDelete = { onDeleteWebsiteRule(rule.id) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddAppsDialog) {
        InstalledAppsDialog(
            installedApps = uiState.installedApps,
            isLoading = uiState.isLoadingInstalledApps,
            existingRules = uiState.appRules,
            onDismiss = { showAddAppsDialog = false },
            onAddSelectedApps = onAddAppRules
        )
    }

    if (showAddWebsiteDialog) {
        WebsiteRuleDialog(
            onDismiss = { showAddWebsiteDialog = false },
            onSaveWebsiteRule = onAddWebsiteRule
        )
    }
}

@Composable
private fun ScheduleItemCard(
    schedule: Schedule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    FocusCard(modifier = Modifier.testTag("schedule_item_${schedule.id}")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${schedule.startTime} - ${schedule.endTime}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                val daysText = when (schedule.repeatDays) {
                    FocusDayOfWeek.ALL_DAYS -> "Every day"
                    FocusDayOfWeek.WEEKDAYS -> "Weekdays (Mon-Fri)"
                    FocusDayOfWeek.WEEKENDS -> "Weekends (Sat-Sun)"
                    else -> schedule.repeatDays.joinToString(", ") { it.shortName }
                }
                Text(
                    text = daysText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = schedule.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete schedule",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AppRuleItemCard(
    rule: AppRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    FocusCard(modifier = Modifier.testTag("app_rule_${rule.id}")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.displayName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = rule.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }

            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete rule",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun WebsiteRuleItemCard(
    rule: WebsiteRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    FocusCard(modifier = Modifier.testTag("website_rule_${rule.id}")) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = rule.domain,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (rule.includeSubdomains) "Includes all subdomains (*.${rule.domain})" else "Exact domain only",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(
                checked = rule.enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = "Delete rule",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun EmptySectionPlaceholder(
    title: String,
    subtitle: String
) {
    FocusCard(modifier = Modifier.padding(top = 16.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }
    }
}
