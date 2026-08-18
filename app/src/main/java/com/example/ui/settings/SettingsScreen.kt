package com.example.ui.settings

import android.app.Activity
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.admin.DevicePolicyController
import com.example.service.AccessibilityHelper
import com.example.service.FocusVpnHelper
import com.example.ui.components.FocusCard
import com.example.ui.components.HairlineDivider
import com.example.ui.components.PrimaryButton
import com.example.ui.components.SecondaryButton
import com.example.ui.components.SectionHeader
import com.example.ui.components.SmallCardShape
import com.example.ui.components.StandardCardShape
import com.example.ui.theme.DangerRed
import com.example.ui.theme.SuccessGreen
import com.example.ui.viewmodel.FocusLockUiState

@Composable
fun SettingsScreen(
    uiState: FocusLockUiState,
    onSetThemeMode: (String) -> Unit,
    onToggleAppBlocking: (Boolean) -> Unit,
    onToggleWebsiteBlocking: (Boolean) -> Unit,
    onToggleInstallProtection: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val devicePolicyController = remember { DevicePolicyController(context) }

    // Real-time Accessibility Service State
    var isAccessibilityActive by remember {
        mutableStateOf(AccessibilityHelper.isAppAccessibilityServiceEnabled(context))
    }
    var showAccessibilityDisclosure by remember { mutableStateOf(false) }

    // Real-time VPN Service State
    val isVpnActive by FocusVpnHelper.isVpnRunning.collectAsStateWithLifecycle()
    var showVpnDisclosure by remember { mutableStateOf(false) }

    // Real-time Device Owner State
    var isDeviceOwnerActive by remember {
        mutableStateOf(devicePolicyController.isDeviceOwner())
    }
    var showDeviceAdminGuide by remember { mutableStateOf(false) }

    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            FocusVpnHelper.startVpnService(context)
            onToggleWebsiteBlocking(true)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isAccessibilityActive = AccessibilityHelper.isAppAccessibilityServiceEnabled(context)
                isDeviceOwnerActive = devicePolicyController.isDeviceOwner()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    var dialogTitle by remember { mutableStateOf<String?>(null) }
    var dialogMessage by remember { mutableStateOf<String?>(null) }

    var strictModeState by remember { mutableStateOf(uiState.strictMode) }
    var quotesState by remember { mutableStateOf(true) }
    var vibrateState by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header
        Column(modifier = Modifier.padding(top = 8.dp)) {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Enforcement protocols, preferences, and system parameters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Enforcement Status
        SectionHeader(title = "Enforcement Status")

        FocusCard(modifier = Modifier.testTag("enforcement_section_card")) {
            Column(modifier = Modifier.padding(16.dp)) {
                // App blocking item (Reflecting real AccessibilityService status)
                EnforcementItemRow(
                    title = "App blocking",
                    statusText = if (isAccessibilityActive) "App blocking: Active" else "App blocking: Not active — Enable",
                    buttonLabel = if (isAccessibilityActive) "Active" else "Enable",
                    isConfigured = isAccessibilityActive,
                    onAction = {
                        if (!isAccessibilityActive) {
                            showAccessibilityDisclosure = true
                        } else {
                            try {
                                context.startActivity(AccessibilityHelper.createAccessibilitySettingsIntent())
                            } catch (e: Exception) {
                                dialogTitle = "App Blocking Enforcement"
                                dialogMessage = "App blocking is active via FocusLock Accessibility Service."
                            }
                        }
                    },
                    onToggle = {
                        if (!isAccessibilityActive) {
                            showAccessibilityDisclosure = true
                        } else {
                            try {
                                context.startActivity(AccessibilityHelper.createAccessibilitySettingsIntent())
                            } catch (e: Exception) {
                                // Ignore
                            }
                        }
                    },
                    testTag = "app_blocking_enforcement"
                )

                HairlineDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Website blocking item (Reflecting real FocusVpnService running status)
                EnforcementItemRow(
                    title = "Website blocking",
                    statusText = if (isVpnActive) "Website blocking: Active" else "Website blocking: Not active — Enable",
                    buttonLabel = if (isVpnActive) "Active" else "Enable",
                    isConfigured = isVpnActive,
                    onAction = {
                        if (!isVpnActive) {
                            showVpnDisclosure = true
                        } else {
                            dialogTitle = "Local DNS Website Filter"
                            dialogMessage = "Website blocking is active using an on-device DNS filter. It blocks whole domain names at the DNS level during active focus sessions. It cannot inspect encrypted HTTPS URL paths (e.g. it can block instagram.com entirely, but cannot block youtube.com/shorts while allowing youtube.com). Zero browsing traffic is proxied off your device."
                        }
                    },
                    onToggle = {
                        if (!isVpnActive) {
                            showVpnDisclosure = true
                        } else {
                            FocusVpnHelper.stopVpnService(context)
                            onToggleWebsiteBlocking(false)
                        }
                    },
                    testTag = "website_blocking_enforcement"
                )

                HairlineDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Install/uninstall protection item (Reflecting real isDeviceOwner status)
                EnforcementItemRow(
                    title = "Install/uninstall protection",
                    statusText = if (isDeviceOwnerActive) "Install/uninstall protection: Active" else "Not configured — see setup guide",
                    buttonLabel = if (isDeviceOwnerActive) "Active" else "Guide",
                    isConfigured = isDeviceOwnerActive,
                    onAction = {
                        showDeviceAdminGuide = true
                    },
                    onToggle = {
                        showDeviceAdminGuide = true
                    },
                    testTag = "install_protection_enforcement"
                )
            }
        }

        // Section 2: Blocked Screen Behavior
        SectionHeader(title = "Blocked Screen Behavior")

        FocusCard(modifier = Modifier.testTag("behavior_section_card")) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Strict Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Disallows dismissing the block overlay until session expires",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = strictModeState,
                        onCheckedChange = { strictModeState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                HairlineDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Motivational Reminder",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Show calm mindfulness aphorisms on intercept screen",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = quotesState,
                        onCheckedChange = { quotesState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }

                HairlineDivider(modifier = Modifier.padding(vertical = 12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Haptic Vibration",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Subtle tactile click when a restricted app is closed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = vibrateState,
                        onCheckedChange = { vibrateState = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }

        // Section 3: Emergency Override
        SectionHeader(title = "Emergency Override")

        FocusCard(modifier = Modifier.testTag("emergency_override_card")) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "REQUIRED CONFIRMATION PHRASE",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = SmallCardShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Text(
                        text = uiState.emergencyPassphrase,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(12.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Emergency access allows breaking an active lock only after typing this phrase. The cancellation is logged permanently to History.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }

        // Section 4: Theme
        SectionHeader(title = "Theme")

        FocusCard(modifier = Modifier.testTag("theme_section_card")) {
            Column(modifier = Modifier.padding(16.dp)) {
                val themeOptions = listOf("SYSTEM" to "System Default", "LIGHT" to "Light", "DARK" to "Dark")

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    themeOptions.forEach { (modeKey, label) ->
                        val isSelected = uiState.themeMode == modeKey
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(SmallCardShape)
                                .clickable { onSetThemeMode(modeKey) },
                            shape = SmallCardShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 5: About
        SectionHeader(title = "About FocusLock")

        FocusCard(modifier = Modifier.testTag("about_card")) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "FocusLock v1.0",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "A zero-distraction self-control system utility designed to build healthy digital boundaries without external dependencies.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Website Blocking Scope: DNS filtering operates on entire domain names (e.g., blocks instagram.com). It cannot inspect encrypted HTTPS contents or filter individual paths (e.g., cannot block youtube.com/shorts while allowing youtube.com).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = SmallCardShape,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "100% Offline • No accounts • No trackers • No ads",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    // Honest In-App Accessibility Disclosure Dialog
    if (showAccessibilityDisclosure) {
        AccessibilityDisclosureDialog(
            onDismiss = { showAccessibilityDisclosure = false },
            onConfirm = {
                showAccessibilityDisclosure = false
                try {
                    context.startActivity(AccessibilityHelper.createAccessibilitySettingsIntent())
                } catch (e: Exception) {
                    dialogTitle = "Accessibility Settings"
                    dialogMessage = "Unable to open system settings automatically. Please open Android Settings -> Accessibility -> FocusLock to enable real-time app blocking."
                }
            }
        )
    }

    // Honest In-App VPN Website Blocking Disclosure Dialog
    if (showVpnDisclosure) {
        VpnDisclosureDialog(
            onDismiss = { showVpnDisclosure = false },
            onConfirm = {
                showVpnDisclosure = false
                val prepareIntent = FocusVpnHelper.checkVpnPrepareIntent(context)
                if (prepareIntent != null) {
                    vpnPrepareLauncher.launch(prepareIntent)
                } else {
                    FocusVpnHelper.startVpnService(context)
                    onToggleWebsiteBlocking(true)
                }
            }
        )
    }

    // Honest In-App Device Admin / Device Owner Setup Guide Dialog
    if (showDeviceAdminGuide) {
        DeviceAdminSetupGuideDialog(
            onDismiss = { showDeviceAdminGuide = false }
        )
    }

    // Generic Info Dialog
    if (dialogTitle != null && dialogMessage != null) {
        AlertDialog(
            onDismissRequest = {
                dialogTitle = null
                dialogMessage = null
            },
            title = {
                Text(
                    text = dialogTitle ?: "",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            },
            text = {
                Text(
                    text = dialogMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                PrimaryButton(
                    text = "Understood",
                    onClick = {
                        dialogTitle = null
                        dialogMessage = null
                    },
                    modifier = Modifier.width(120.dp)
                )
            },
            shape = StandardCardShape,
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
private fun EnforcementItemRow(
    title: String,
    statusText: String,
    buttonLabel: String = "Enable",
    isConfigured: Boolean,
    onAction: () -> Unit,
    onToggle: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = if (isConfigured) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SecondaryButton(
                text = buttonLabel,
                onClick = onAction,
                modifier = Modifier
                    .width(96.dp)
                    .height(36.dp)
            )

            Switch(
                checked = isConfigured,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}
