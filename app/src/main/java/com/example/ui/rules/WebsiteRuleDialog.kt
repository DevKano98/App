package com.example.ui.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SmallCardShape
import com.example.ui.components.StandardCardShape

@Composable
fun WebsiteRuleDialog(
    onDismiss: () -> Unit,
    onSaveWebsiteRule: (domain: String, includeSubdomains: Boolean) -> Unit
) {
    var domainText by remember { mutableStateOf("") }
    var includeSubdomains by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Add Website Rule",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Specify a domain name to restrict during focus sessions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = domainText,
                    onValueChange = {
                        domainText = it
                        if (isError) isError = false
                    },
                    label = { Text("Domain (e.g. reddit.com)") },
                    placeholder = { Text("example.com") },
                    singleLine = true,
                    isError = isError,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("website_domain_input"),
                    shape = SmallCardShape,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )

                if (isError) {
                    Text(
                        text = "Please enter a valid domain name.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Include Subdomains",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Block *.${domainText.ifBlank { "example.com" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = includeSubdomains,
                        onCheckedChange = { includeSubdomains = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("subdomains_switch")
                    )
                }
            }
        },
        confirmButton = {
            PrimaryButton(
                text = "Save Rule",
                onClick = {
                    val clean = domainText.trim()
                    if (clean.isNotBlank() && clean.contains(".")) {
                        onSaveWebsiteRule(clean, includeSubdomains)
                        onDismiss()
                    } else {
                        isError = true
                    }
                },
                modifier = Modifier.testTag("confirm_website_rule_button")
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
