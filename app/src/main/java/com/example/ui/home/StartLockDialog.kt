package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HairlineDivider
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SmallCardShape
import com.example.ui.components.StandardCardShape
import java.util.Calendar

@Composable
fun StartLockDialog(
    onDismiss: () -> Unit,
    onStartDurationLock: (minutes: Int, name: String) -> Unit,
    onStartTimeRangeLock: (startMillis: Long, endMillis: Long, name: String) -> Unit
) {
    var lockTypeTab by remember { mutableIntStateOf(0) } // 0 = Duration, 1 = Time Range
    var sessionName by remember { mutableStateOf("Focus Session") }

    // Duration state
    var selectedPresetMinutes by remember { mutableIntStateOf(45) }
    var customDurationText by remember { mutableStateOf("") }

    // Time Range state (HH:mm)
    val nowCal = remember { Calendar.getInstance() }
    val defaultStartHour = nowCal.get(Calendar.HOUR_OF_DAY)
    val defaultStartMin = nowCal.get(Calendar.MINUTE)
    var startHourText by remember { mutableStateOf(String.format("%02d", defaultStartHour)) }
    var startMinText by remember { mutableStateOf(String.format("%02d", defaultStartMin)) }

    nowCal.add(Calendar.HOUR_OF_DAY, 1)
    val defaultEndHour = nowCal.get(Calendar.HOUR_OF_DAY)
    val defaultEndMin = nowCal.get(Calendar.MINUTE)
    var endHourText by remember { mutableStateOf(String.format("%02d", defaultEndHour)) }
    var endMinText by remember { mutableStateOf(String.format("%02d", defaultEndMin)) }

    val presetList = listOf(15, 25, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Start Lock Session",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Segmented tab switch
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SmallCardShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SmallCardShape)
                                .background(
                                    if (lockTypeTab == 0) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { lockTypeTab = 0 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Duration",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (lockTypeTab == 0) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SmallCardShape)
                                .background(
                                    if (lockTypeTab == 1) MaterialTheme.colorScheme.surface
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .clickable { lockTypeTab = 1 }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Time Range",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = if (lockTypeTab == 1) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                // Session Name
                OutlinedTextField(
                    value = sessionName,
                    onValueChange = { sessionName = it },
                    label = { Text("Session Name") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_name_input"),
                    shape = SmallCardShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (lockTypeTab == 0) {
                    // Presets
                    Text(
                        text = "SELECT DURATION",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetList.take(3).forEach { minutes ->
                            val isSelected = selectedPresetMinutes == minutes && customDurationText.isBlank()
                            PresetChip(
                                label = "${minutes}m",
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedPresetMinutes = minutes
                                    customDurationText = ""
                                }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        presetList.drop(3).forEach { minutes ->
                            val isSelected = selectedPresetMinutes == minutes && customDurationText.isBlank()
                            PresetChip(
                                label = if (minutes >= 60) "${minutes / 60}h" + if (minutes % 60 > 0) " ${minutes % 60}m" else "" else "${minutes}m",
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    selectedPresetMinutes = minutes
                                    customDurationText = ""
                                }
                            )
                        }
                    }

                    // Custom minutes input
                    OutlinedTextField(
                        value = customDurationText,
                        onValueChange = { customDurationText = it },
                        label = { Text("Custom Duration (Minutes)") },
                        placeholder = { Text("e.g. 30") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("custom_duration_input"),
                        shape = SmallCardShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        )
                    )
                } else {
                    // Time Range (Today HH:MM to HH:MM)
                    Text(
                        text = "TODAY'S TIME WINDOW",
                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                                    value = startHourText,
                                    onValueChange = { if (it.length <= 2) startHourText = it },
                                    label = { Text("HH") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = SmallCardShape
                                )
                                OutlinedTextField(
                                    value = startMinText,
                                    onValueChange = { if (it.length <= 2) startMinText = it },
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
                                    value = endHourText,
                                    onValueChange = { if (it.length <= 2) endHourText = it },
                                    label = { Text("HH") },
                                    modifier = Modifier.weight(1f),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = SmallCardShape
                                )
                                OutlinedTextField(
                                    value = endMinText,
                                    onValueChange = { if (it.length <= 2) endMinText = it },
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
        },
        confirmButton = {
            PrimaryButton(
                text = "Start Lock",
                onClick = {
                    if (lockTypeTab == 0) {
                        val minutes = customDurationText.toIntOrNull() ?: selectedPresetMinutes
                        if (minutes > 0) {
                            onStartDurationLock(minutes, sessionName)
                            onDismiss()
                        }
                    } else {
                        val sh = startHourText.toIntOrNull() ?: 0
                        val sm = startMinText.toIntOrNull() ?: 0
                        val eh = endHourText.toIntOrNull() ?: 0
                        val em = endMinText.toIntOrNull() ?: 0

                        val startCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, sh.coerceIn(0, 23))
                            set(Calendar.MINUTE, sm.coerceIn(0, 59))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }

                        val endCal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, eh.coerceIn(0, 23))
                            set(Calendar.MINUTE, em.coerceIn(0, 59))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                            if (timeInMillis <= startCal.timeInMillis) {
                                add(Calendar.DAY_OF_YEAR, 1)
                            }
                        }

                        onStartTimeRangeLock(startCal.timeInMillis, endCal.timeInMillis, sessionName)
                        onDismiss()
                    }
                },
                modifier = Modifier.testTag("confirm_start_lock_button")
            )
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = SmallCardShape
            ) {
                Text(
                    text = "Cancel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape = StandardCardShape,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
private fun PresetChip(
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
