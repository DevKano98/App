package com.example.ui.activelock

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SmallCardShape
import com.example.ui.components.StandardCardShape
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedBg
import com.example.ui.theme.DangerRedBorder

@Composable
fun EmergencyOverrideDialog(
    requiredPassphrase: String,
    onDismiss: () -> Unit,
    onConfirmOverride: (enteredPassphrase: String) -> Unit
) {
    var enteredText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val matches = enteredText.trim() == requiredPassphrase.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(DangerRedBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = DangerRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "Emergency Override",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Early session cancellation should only be used in urgent circumstances. This action is permanently logged to your history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SmallCardShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "TYPE EXACT PHRASE TO CONFIRM:",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = requiredPassphrase,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                OutlinedTextField(
                    value = enteredText,
                    onValueChange = {
                        enteredText = it
                        if (isError) isError = false
                    },
                    label = { Text("Confirmation Phrase") },
                    isError = isError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emergency_override_input"),
                    shape = SmallCardShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DangerRed,
                        unfocusedBorderColor = if (isError) DangerRed else MaterialTheme.colorScheme.outline,
                        focusedLabelColor = DangerRed
                    )
                )

                if (isError) {
                    Text(
                        text = "Phrase does not match. Please enter precisely.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DangerRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (matches) {
                        onConfirmOverride(enteredText)
                    } else {
                        isError = true
                    }
                },
                enabled = matches,
                shape = SmallCardShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.testTag("confirm_emergency_override_button")
            ) {
                Text(
                    text = "End Session",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                shape = SmallCardShape
            ) {
                Text(
                    text = "Keep Focusing",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        shape = StandardCardShape,
        containerColor = MaterialTheme.colorScheme.surface
    )
}
