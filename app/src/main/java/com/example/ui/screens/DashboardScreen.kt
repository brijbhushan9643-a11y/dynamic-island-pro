package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.IslandSettingsRepository.FeatureToggle
import com.example.model.IslandDisplayMode
import com.example.system.FormatUtils
import com.example.ui.components.DynamicIslandPill
import com.example.ui.components.FeatureSwitchRow
import com.example.ui.components.GlassCard
import com.example.ui.components.IslandCallbacks
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltraViolet
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.MainUiState

@Composable
fun DashboardScreen(
    uiState: MainUiState,
    onToggleIsland: (Boolean) -> Unit,
    onTogglePreviewExpand: () -> Unit,
    onSelectPreviewMode: (IslandDisplayMode?) -> Unit,
    onToggleFeature: (FeatureToggle, Boolean) -> Unit,
    onNavigateToStudio: () -> Unit,
    onNavigateToModules: () -> Unit,
    onNavigateToSetup: () -> Unit,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onOpenMediaApp: () -> Unit,
    onOpenNotification: (com.example.model.IslandNotificationItem) -> Unit,
    onDismissNotification: (String) -> Unit,
    onSetMediaVolume: (Int) -> Unit,
    onSetBrightness: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val settings = uiState.settings
    val telemetry = uiState.telemetry
    val perms = uiState.permissions

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack)
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Master Control Card
        item {
            Box(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_island_hero_bg_1795267857567),
                    contentDescription = "Dynamic Island Pro ambient backdrop",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    VoidBlack.copy(alpha = 0.45f),
                                    ObsidianSurface.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "DYNAMIC ISLAND PRO",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                letterSpacing = 1.6.sp,
                                color = ElectricCyan
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = if (uiState.isOverlayServiceRunning) {
                                    "Floating HUD Active"
                                } else {
                                    "Floating HUD Standby"
                                },
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = "${perms.androidVersionLabel} • ${settings.themePreset.displayName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                        Switch(
                            checked = settings.isIslandEnabled && uiState.isOverlayServiceRunning,
                            onCheckedChange = onToggleIsland,
                            modifier = Modifier.testTag("master_island_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VoidBlack,
                                checkedTrackColor = NeonEmerald,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = ElevatedObsidian
                            )
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatusBadgePill(
                            label = if (perms.hasOverlayPermission) "OVERLAY OK" else "OVERLAY NEEDED",
                            isPositive = perms.hasOverlayPermission
                        )
                        StatusBadgePill(
                            label = if (perms.hasNotificationAccess) "LISTENER OK" else "ACCESS NEEDED",
                            isPositive = perms.hasNotificationAccess
                        )
                    }

                    if (uiState.statusBannerMessage != null || !perms.hasOverlayPermission || !perms.hasNotificationAccess) {
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AmberPulse.copy(alpha = 0.14f))
                                .border(1.dp, AmberPulse.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .clickable(onClick = onNavigateToSetup)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = "Permission required",
                                tint = AmberPulse,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = uiState.statusBannerMessage
                                    ?: "Complete permission setup for full overlay, media & notification control.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Setup",
                                color = AmberPulse,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }

        // Live Interactive Island Preview
        item {
            GlassCard(
                modifier = Modifier.widthIn(max = 640.dp),
                accentGlow = Color(settings.glowColorHex)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Live Island Preview",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary
                        )
                        Text(
                            text = "Tap the capsule to expand or collapse",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary
                        )
                    }
                    StatusBadgePill(
                        label = (uiState.previewMode ?: telemetry.activeMode).name,
                        isPositive = true,
                        positiveColor = ElectricCyan
                    )
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF101826), VoidBlack)
                            )
                        )
                        .border(1.dp, GlassBorder, RoundedCornerShape(20.dp))
                        .padding(vertical = 22.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DynamicIslandPill(
                        settings = settings,
                        telemetry = telemetry,
                        isExpanded = uiState.isPreviewExpanded,
                        displayModeOverride = uiState.previewMode,
                        callbacks = IslandCallbacks(
                            onToggleExpand = onTogglePreviewExpand,
                            onPlayPause = onPlayPause,
                            onSkipNext = onSkipNext,
                            onSkipPrevious = onSkipPrev,
                            onOpenMediaApp = onOpenMediaApp,
                            onOpenNotification = onOpenNotification,
                            onDismissNotification = onDismissNotification,
                            onSetMediaVolume = onSetMediaVolume,
                            onSetBrightness = onSetBrightness
                        )
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.previewMode == null,
                        onClick = { onSelectPreviewMode(null) },
                        label = { Text("Auto Live") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                            selectedLabelColor = ElectricCyan
                        )
                    )
                    IslandDisplayMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.previewMode == mode,
                            onClick = { onSelectPreviewMode(mode) },
                            label = { Text(mode.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricCyan.copy(alpha = 0.22f),
                                selectedLabelColor = ElectricCyan
                            )
                        )
                    }
                }
            }
        }

        // Quick Actions Bar
        item {
            Row(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToStudio,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_open_studio"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricCyan,
                        contentColor = VoidBlack
                    )
                ) {
                    Icon(Icons.Filled.Palette, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Studio", fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onNavigateToModules,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_open_modules"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Tune, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Modules", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
                OutlinedButton(
                    onClick = onNavigateToSetup,
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("btn_open_setup"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.Filled.Security, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Setup", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Real-Time System Telemetry Grid
        item {
            Column(modifier = Modifier.widthIn(max = 640.dp)) {
                SectionHeader(
                    title = "Live System Telemetry",
                    subtitle = "Real-time Android hardware, media & network sensors"
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TelemetryMiniCard(
                        icon = Icons.Filled.BatteryChargingFull,
                        tint = if (telemetry.battery.isLow) CrimsonAlert else NeonEmerald,
                        title = "Battery",
                        primaryValue = if (telemetry.battery.hasValidReading) "${telemetry.battery.level}%" else "--%",
                        secondaryValue = "${telemetry.battery.pluggedLabel} • ${telemetry.battery.temperatureCelsius}°C",
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniCard(
                        icon = Icons.Filled.Wifi,
                        tint = if (telemetry.network.isConnected) ElectricCyan else AmberPulse,
                        title = "Network",
                        primaryValue = telemetry.network.transportLabel,
                        secondaryValue = "↓ ${FormatUtils.formatBytesPerSec(telemetry.network.deviceDownBytesPerSec)}",
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val volPct = ((telemetry.volumeBrightness.mediaVolume * 100f) /
                        telemetry.volumeBrightness.maxMediaVolume.coerceAtLeast(1)).toInt()
                    TelemetryMiniCard(
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        tint = AmberPulse,
                        title = "Audio / Display",
                        primaryValue = "Vol $volPct%",
                        secondaryValue = "Brightness ${telemetry.volumeBrightness.brightnessPercent}%",
                        modifier = Modifier.weight(1f)
                    )
                    TelemetryMiniCard(
                        icon = Icons.Filled.MusicNote,
                        tint = UltraViolet,
                        title = "Media Session",
                        primaryValue = if (telemetry.media.isActive) {
                            if (telemetry.media.isPlaying) "Playing" else "Paused"
                        } else "Idle",
                        secondaryValue = telemetry.media.title.ifBlank { "No active player" },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Feature Management Switches
        item {
            GlassCard(modifier = Modifier.widthIn(max = 640.dp)) {
                SectionHeader(
                    title = "Feature Management",
                    subtitle = "Enable or pause individual Dynamic Island modules"
                )
                Spacer(Modifier.height(6.dp))
                FeatureSwitchRow(
                    icon = Icons.Filled.MusicNote,
                    iconTint = ElectricCyan,
                    title = "Advanced Music Player",
                    subtitle = "Album art, waveform, play/pause & track controls",
                    checked = settings.musicPlayerEnabled,
                    testTag = "toggle_music",
                    onCheckedChange = { onToggleFeature(FeatureToggle.MUSIC, it) }
                )
                FeatureSwitchRow(
                    icon = Icons.Filled.BatteryChargingFull,
                    iconTint = NeonEmerald,
                    title = "Battery & Charging Pulse",
                    subtitle = "Charging animation, temperature, full & low alerts",
                    checked = settings.batteryMonitorEnabled,
                    testTag = "toggle_battery",
                    onCheckedChange = { onToggleFeature(FeatureToggle.BATTERY, it) }
                )
                FeatureSwitchRow(
                    icon = Icons.Filled.Call,
                    iconTint = NeonEmerald,
                    title = "Call Status HUD",
                    subtitle = "Incoming & active call indicator with duration timer",
                    checked = settings.callMonitorEnabled,
                    testTag = "toggle_calls",
                    onCheckedChange = { onToggleFeature(FeatureToggle.CALLS, it) }
                )
                FeatureSwitchRow(
                    icon = Icons.Filled.Notifications,
                    iconTint = UltraViolet,
                    title = "Smart Notification Center",
                    subtitle = "WhatsApp, Instagram, Telegram, SMS & custom apps",
                    checked = settings.notificationsEnabled,
                    testTag = "toggle_notifications",
                    onCheckedChange = { onToggleFeature(FeatureToggle.NOTIFICATIONS, it) }
                )
                FeatureSwitchRow(
                    icon = Icons.Filled.Wifi,
                    iconTint = ElectricCyan,
                    title = "Internet & Speed Monitor",
                    subtitle = "Wi-Fi/Mobile link, traffic rate & outage alerts",
                    checked = settings.networkMonitorEnabled,
                    testTag = "toggle_network",
                    onCheckedChange = { onToggleFeature(FeatureToggle.NETWORK, it) }
                )
                FeatureSwitchRow(
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconTint = AmberPulse,
                    title = "Volume & Brightness HUD",
                    subtitle = "Live media volume & display brightness indicator",
                    checked = settings.volumeBrightnessEnabled,
                    testTag = "toggle_volume_brightness",
                    onCheckedChange = { onToggleFeature(FeatureToggle.VOLUME_BRIGHTNESS, it) }
                )
                FeatureSwitchRow(
                    icon = Icons.Filled.AutoAwesome,
                    iconTint = UltraViolet,
                    title = "Haptic Touch Feedback",
                    subtitle = "Subtle tactile pulse when expanding the capsule",
                    checked = settings.hapticFeedbackEnabled,
                    testTag = "toggle_haptic",
                    onCheckedChange = { onToggleFeature(FeatureToggle.HAPTIC, it) }
                )
            }
        }
    }
}

@Composable
private fun TelemetryMiniCard(
    icon: ImageVector,
    tint: Color,
    title: String,
    primaryValue: String,
    secondaryValue: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier, accentGlow = tint.copy(alpha = 0.35f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(tint.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = tint,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextSecondary
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = primaryValue,
            fontFamily = SpaceGroteskFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = TextPrimary,
            maxLines = 1
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = secondaryValue,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            maxLines = 1
        )
    }
}
