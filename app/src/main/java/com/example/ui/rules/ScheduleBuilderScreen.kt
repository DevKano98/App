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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppRule
import com.example.data.model.FocusDayOfWeek
import com.example.data.model.Schedule
import com.example.data.model.WebsiteRule
import com.example.ui.components.FocusCard
import com.example.ui.components.HairlineDivider
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SmallCardShape
import com.example.ui.components.StandardCardShape

@Composable
fun ScheduleBuilderScreen(
    appRules: List<AppRule>,
    websiteRules: List<WebsiteRule>,
    onBack: () -> Unit,
    onSaveSchedule: (Schedule) -> Unit,
    modifier: Modifier = Modifier
) {
    var scheduleName by remember { mutableStateOf("") }
    var repeatMode by remember { mutableIntStateOf(0) } // 0 = Weekdays, 1 = Daily, 2 = Custom

    val customDays = remember { mutableStateListOf<FocusDayOfWeek>().apply { addAll(FocusDayOfWeek.WEEKDAYS) } }

    var startHour by remember { mutableStateOf("09") }
    var startMin by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("17") }
    var endMin by remember { mutableStateOf("00") }

    val attachedAppIds = remember { mutableStateListOf<Long>().apply { addAll(appRules.map { it.id }) } }
    val attachedWebsiteIds = remember { mutableStateListOf<Long>().apply { addAll(websiteRules.map { it.id }) } }

    var isError by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "New Schedule",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Schedule Name
        FocusCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "SCHEDULE NAME",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = scheduleName,
                    onValueChange = {
                        scheduleName = it
                        if (isError) isError = false
                    },
                    placeholder = { Text("e.g. Deep Work, Evening Offline") },
                    singleLine = true,
                    isError = isError && scheduleName.isBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_name_input"),
                    shape = SmallCardShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
            }
        }

        // Schedule Repeat Days
        FocusCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "REPEAT SCHEDULE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RepeatChip(
                        label = "Weekdays",
                        isSelected = repeatMode == 0,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            repeatMode = 0
                            customDays.clear()
                            customDays.addAll(FocusDayOfWeek.WEEKDAYS)
                        }
                    )
                    RepeatChip(
                        label = "Daily",
                        isSelected = repeatMode == 1,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            repeatMode = 1
                            customDays.clear()
                            customDays.addAll(FocusDayOfWeek.ALL_DAYS)
                        }
                    )
                    RepeatChip(
                        label = "Custom",
                        isSelected = repeatMode == 2,
                        modifier = Modifier.weight(1f),
                        onClick = { repeatMode = 2 }
                    )
                }

                if (repeatMode == 2) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "SELECT DAYS",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        FocusDayOfWeek.entries.forEach { day ->
                            val isSelected = customDays.contains(day)
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable {
                                        if (isSelected) customDays.remove(day)
                                        else customDays.add(day)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.shortName.take(1),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Time Range
        FocusCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "ACTIVE TIME WINDOW",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Start Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = startHour,
                                onValueChange = { if (it.length <= 2) startHour = it },
                                label = { Text("HH") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = SmallCardShape
                            )
                            OutlinedTextField(
                                value = startMin,
                                onValueChange = { if (it.length <= 2) startMin = it },
                                label = { Text("MM") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = SmallCardShape
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "End Time",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            OutlinedTextField(
                                value = endHour,
                                onValueChange = { if (it.length <= 2) endHour = it },
                                label = { Text("HH") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = SmallCardShape
                            )
                            OutlinedTextField(
                                value = endMin,
                                onValueChange = { if (it.length <= 2) endMin = it },
                                label = { Text("MM") },
                                modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                shape = SmallCardShape
                            )
                        }
                    }
                }
            }
        }

        // Attach App Rules
        if (appRules.isNotEmpty()) {
            FocusCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ATTACHED APP RULES (${attachedAppIds.size}/${appRules.size})",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    appRules.forEach { rule ->
                        val isChecked = attachedAppIds.contains(rule.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) attachedAppIds.remove(rule.id)
                                    else attachedAppIds.add(rule.id)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rule.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) attachedAppIds.add(rule.id)
                                    else attachedAppIds.remove(rule.id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }
            }
        }

        // Attach Website Rules
        if (websiteRules.isNotEmpty()) {
            FocusCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ATTACHED WEBSITE RULES (${attachedWebsiteIds.size}/${websiteRules.size})",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    websiteRules.forEach { rule ->
                        val isChecked = attachedWebsiteIds.contains(rule.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isChecked) attachedWebsiteIds.remove(rule.id)
                                    else attachedWebsiteIds.add(rule.id)
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = rule.domain,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = {
                                    if (it) attachedWebsiteIds.add(rule.id)
                                    else attachedWebsiteIds.remove(rule.id)
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = MaterialTheme.colorScheme.primary,
                                    uncheckedColor = MaterialTheme.colorScheme.outline
                                )
                            )
                        }
                    }
                }
            }
        }

        // Save Button
        PrimaryButton(
            text = "Save Schedule",
            onClick = {
                val finalDays = when (repeatMode) {
                    0 -> FocusDayOfWeek.WEEKDAYS
                    1 -> FocusDayOfWeek.ALL_DAYS
                    else -> customDays.toSet()
                }

                val sh = (startHour.toIntOrNull() ?: 9).coerceIn(0, 23)
                val sm = (startMin.toIntOrNull() ?: 0).coerceIn(0, 59)
                val eh = (endHour.toIntOrNull() ?: 17).coerceIn(0, 23)
                val em = (endMin.toIntOrNull() ?: 0).coerceIn(0, 59)

                val formattedStart = String.format("%02d:%02d", sh, sm)
                val formattedEnd = String.format("%02d:%02d", eh, em)

                if (scheduleName.isNotBlank() && finalDays.isNotEmpty()) {
                    onSaveSchedule(
                        Schedule(
                            name = scheduleName.trim(),
                            startTime = formattedStart,
                            endTime = formattedEnd,
                            repeatDays = finalDays,
                            enabled = true,
                            appRuleIds = attachedAppIds.toList(),
                            websiteRuleIds = attachedWebsiteIds.toList()
                        )
                    )
                    onBack()
                } else {
                    isError = true
                }
            },
            modifier = Modifier.testTag("save_schedule_button")
        )

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun RepeatChip(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .clip(SmallCardShape)
            .clickable(onClick = onClick),
        shape = SmallCardShape,
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
        )
    ) {
        Box(
            modifier = Modifier.padding(vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
