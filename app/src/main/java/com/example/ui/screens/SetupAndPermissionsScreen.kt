package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatterySaver
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.BuildConfig
import com.example.data.IslandSettingsRepository.FeatureToggle
import com.example.system.PermissionHelper
import com.example.ui.components.FeatureSwitchRow
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltraViolet
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.MainUiState

@Composable
fun SetupAndPermissionsScreen(
    uiState: MainUiState,
    onRefreshPermissions: () -> Unit,
    onToggleFeature: (FeatureToggle, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val perms = uiState.permissions
    val settings = uiState.settings

    val notificationPermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        onRefreshPermissions()
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Permission Manager Header & Refresh
        item {
            GlassCard(
                modifier = Modifier.widthIn(max = 640.dp),
                accentGlow = ElectricCyan
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permission Setup Manager",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Every permission is explained clearly before opening Android Settings.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    OutlinedButton(
                        onClick = onRefreshPermissions,
                        modifier = Modifier.testTag("btn_refresh_permissions"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh status",
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Check", color = ElectricCyan)
                    }
                }
            }
        }

        // Individual Permissions
        item {
            Column(
                modifier = Modifier.widthIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                PermissionActionCard(
                    icon = Icons.Filled.Layers,
                    tint = ElectricCyan,
                    title = "1. Display Over Other Apps (Required)",
                    explanation = "Allows Dynamic Island Pro to render the floating capsule at the top of your screen over other apps using Android WindowManager.",
                    isGranted = perms.hasOverlayPermission,
                    buttonLabel = if (perms.hasOverlayPermission) "Manage Overlay" else "Grant Overlay Access",
                    testTag = "btn_perm_overlay",
                    onClick = { PermissionHelper.openOverlaySettings(context) }
                )

                PermissionActionCard(
                    icon = Icons.Filled.NotificationsActive,
                    tint = UltraViolet,
                    title = "2. Notification & Media Session Access (Recommended)",
                    explanation = "Required by Android to read live MediaSession playback (title, artist, album art, play/pause) and display WhatsApp, Instagram, Telegram, SMS & call alerts.",
                    isGranted = perms.hasNotificationAccess,
                    buttonLabel = if (perms.hasNotificationAccess) "Listener Settings" else "Enable Notification Access",
                    testTag = "btn_perm_listener",
                    onClick = { PermissionHelper.openNotificationListenerSettings(context) }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    PermissionActionCard(
                        icon = Icons.Filled.NotificationsActive,
                        tint = NeonEmerald,
                        title = "3. Post Notifications (Android 13+)",
                        explanation = "Required on Android 13, 14, 15 and 16+ so the persistent foreground overlay service and battery full/low alerts can notify reliably.",
                        isGranted = perms.hasPostNotifications,
                        buttonLabel = if (perms.hasPostNotifications) "Notification Settings" else "Allow Notifications",
                        testTag = "btn_perm_post_notif",
                        onClick = {
                            if (perms.hasPostNotifications) {
                                PermissionHelper.openAppNotificationSettings(context)
                            } else {
                                notificationPermLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                }

                PermissionActionCard(
                    icon = Icons.Filled.BrightnessHigh,
                    tint = AmberPulse,
                    title = "4. Modify System Settings (Optional)",
                    explanation = "Only needed if you want to adjust device screen brightness directly from the expanded Dynamic Island slider.",
                    isGranted = perms.hasWriteSettings,
                    buttonLabel = if (perms.hasWriteSettings) "Manage Write Settings" else "Allow Brightness Control",
                    testTag = "btn_perm_write_settings",
                    onClick = { PermissionHelper.openWriteSettings(context) }
                )

                PermissionActionCard(
                    icon = Icons.Filled.BatterySaver,
                    tint = NeonEmerald,
                    title = "5. Unrestricted Battery (Redmi / MIUI / HyperOS)",
                    explanation = "Prevents Android 14 / HyperOS / MIUI battery saver from killing the floating Dynamic Island service while in the background.",
                    isGranted = perms.isIgnoringBatteryOptimizations,
                    buttonLabel = "Open Battery Settings",
                    testTag = "btn_perm_battery",
                    onClick = { PermissionHelper.openBatteryOptimizationSettings(context) }
                )
            }
        }

        // Privacy & Security Controls
        item {
            GlassCard(modifier = Modifier.widthIn(max = 640.dp)) {
                SectionHeader(
                    title = "Privacy & Security Controls",
                    subtitle = "100% local processing • Zero cloud uploads • No tracking",
                    trailing = {
                        Icon(
                            imageVector = Icons.Filled.VerifiedUser,
                            contentDescription = "Local privacy",
                            tint = NeonEmerald
                        )
                    }
                )
                Spacer(Modifier.height(6.dp))
                FeatureSwitchRow(
                    icon = Icons.Filled.PrivacyTip,
                    iconTint = ElectricCyan,
                    title = "Show Notification Content",
                    subtitle = "When off, only the app name appears in the Dynamic Island",
                    checked = settings.showNotificationContent,
                    testTag = "toggle_show_notif_content",
                    onCheckedChange = { onToggleFeature(FeatureToggle.SHOW_CONTENT, it) }
                )
                FeatureSwitchRow(
                    icon = Icons.Filled.VisibilityOff,
                    iconTint = UltraViolet,
                    title = "Hide Sensitive Text on Lock Screen",
                    subtitle = "Automatically conceals message previews while screen is locked",
                    checked = settings.hideSensitiveOnLock,
                    testTag = "toggle_hide_on_lock",
                    onCheckedChange = { onToggleFeature(FeatureToggle.HIDE_SENSITIVE_LOCK, it) }
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElevatedObsidian)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = null,
                        tint = NeonEmerald,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "All notifications, media metadata, and battery readings remain strictly in device memory. Settings are stored locally via Jetpack DataStore.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Device Compatibility & App Information
        item {
            GlassCard(modifier = Modifier.widthIn(max = 640.dp)) {
                SectionHeader(
                    title = "Device Diagnostics & App Info",
                    subtitle = "Compatibility & Redmi 12C / Android 14 installation guide",
                    trailing = {
                        Icon(
                            imageVector = Icons.Filled.PhoneAndroid,
                            contentDescription = null,
                            tint = ElectricCyan
                        )
                    }
                )
                Spacer(Modifier.height(10.dp))
                InfoKeyValueRow("Application", "Dynamic Island Pro v${BuildConfig.VERSION_NAME}")
                InfoKeyValueRow("Package ID", BuildConfig.APPLICATION_ID)
                InfoKeyValueRow("Device Model", perms.deviceModel)
                InfoKeyValueRow("OS Runtime", perms.androidVersionLabel)
                InfoKeyValueRow("Supported SDKs", "Android 10 (API 29) – Android 16 (API 36)")

                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(ElevatedObsidian)
                        .border(1.dp, GlassBorder, RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Redmi 12C (Android 14 / HyperOS / MIUI) Tips",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "1. On Android 14 MIUI/HyperOS, if 'Notification Access' or 'Display over other apps' shows Restricted Setting, open Settings > Apps > Dynamic Island Pro > 3-dot menu > Allow restricted settings.\n" +
                                "2. Set Battery Saver to 'No restrictions' and enable 'Autostart' in Security > Permissions so the overlay stays active after screen off.\n" +
                                "3. Brightness control requires 'Modify system settings'; media/call controls rely on standard Android MediaSession & NotificationListener APIs without altering the default Phone app.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionActionCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    explanation: String,
    isGranted: Boolean,
    buttonLabel: String,
    testTag: String,
    onClick: () -> Unit
) {
    GlassCard(accentGlow = if (isGranted) NeonEmerald.copy(alpha = 0.45f) else tint.copy(alpha = 0.35f)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(tint.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = tint,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Spacer(Modifier.width(8.dp))
            StatusBadgePill(
                label = if (isGranted) "GRANTED" else "REQUIRED",
                isPositive = isGranted,
                positiveColor = NeonEmerald,
                negativeColor = AmberPulse
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = explanation,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) ElevatedObsidian else tint,
                contentColor = if (isGranted) TextPrimary else VoidBlack
            )
        ) {
            Text(text = buttonLabel, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun InfoKeyValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Text(
            text = value,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 12.sp,
            color = TextPrimary
        )
    }
}
