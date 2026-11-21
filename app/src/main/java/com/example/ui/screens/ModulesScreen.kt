package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.model.IslandNotificationItem
import com.example.system.FormatUtils
import com.example.system.PermissionHelper
import com.example.ui.components.AudioWaveformBars
import com.example.ui.components.GlassCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadgePill
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.ElevatedObsidian
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.UltraViolet
import com.example.ui.theme.VoidBlack
import com.example.ui.viewmodel.MainUiState

@Composable
fun ModulesScreen(
    uiState: MainUiState,
    onPlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrev: () -> Unit,
    onOpenMediaApp: () -> Unit,
    onSetMediaVolume: (Int) -> Unit,
    onSetBrightness: (Int) -> Unit,
    onOpenNotification: (IslandNotificationItem) -> Unit,
    onDismissNotification: (String) -> Unit,
    onToggleMonitoredPackage: (String, Boolean) -> Unit,
    onNavigateToSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val telemetry = uiState.telemetry
    val media = telemetry.media
    val net = telemetry.network
    val vb = telemetry.volumeBrightness

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(VoidBlack),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Active Media Session Deck
        item {
            GlassCard(
                modifier = Modifier.widthIn(max = 640.dp),
                accentGlow = if (media.isPlaying) ElectricCyan else null
            ) {
                SectionHeader(
                    title = "Media Session Controller",
                    subtitle = if (media.hasMediaAccess) {
                        "Connected to Android MediaSessionManager"
                    } else {
                        "Notification access required to read active players"
                    },
                    trailing = {
                        StatusBadgePill(
                            label = if (media.isPlaying) "PLAYING" else if (media.isActive) "PAUSED" else "IDLE",
                            isPositive = media.isActive,
                            positiveColor = ElectricCyan
                        )
                    }
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElevatedObsidian)
                            .clickable(enabled = media.isActive, onClick = onOpenMediaApp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (media.albumArt != null) {
                            Image(
                                bitmap = media.albumArt.asImageBitmap(),
                                contentDescription = "Album art",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.MusicNote,
                                contentDescription = "Music",
                                tint = ElectricCyan
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (media.isActive) media.title else "No Media Playing",
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (media.isActive) {
                                "${media.artist} • ${media.appName}"
                            } else {
                                "Start playback in Spotify, YT Music, or a podcast app"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AudioWaveformBars(
                        isAnimating = media.isPlaying,
                        accentColor = ElectricCyan,
                        barCount = 5,
                        maxHeight = 22.dp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onSkipPrev, enabled = media.isActive) {
                        Icon(
                            imageVector = Icons.Filled.SkipPrevious,
                            contentDescription = "Previous track",
                            tint = if (media.isActive) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(
                        onClick = onPlayPause,
                        enabled = media.isActive,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(ElectricCyan.copy(alpha = if (media.isActive) 0.22f else 0.08f))
                    ) {
                        Icon(
                            imageVector = if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Play or pause",
                            tint = if (media.isActive) ElectricCyan else TextSecondary
                        )
                    }
                    IconButton(onClick = onSkipNext, enabled = media.isActive) {
                        Icon(
                            imageVector = Icons.Filled.SkipNext,
                            contentDescription = "Next track",
                            tint = if (media.isActive) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                        )
                    }
                }
            }
        }

        // Internet & Network Monitor
        item {
            GlassCard(
                modifier = Modifier.widthIn(max = 640.dp),
                accentGlow = if (net.isConnected) NeonEmerald.copy(alpha = 0.45f) else AmberPulse
            ) {
                SectionHeader(
                    title = "Internet & Network Monitor",
                    subtitle = "ConnectivityManager & TrafficStats live telemetry",
                    trailing = {
                        StatusBadgePill(
                            label = net.transportLabel.uppercase(),
                            isPositive = net.isConnected,
                            positiveColor = NeonEmerald,
                            negativeColor = AmberPulse
                        )
                    }
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElevatedObsidian)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (net.isConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "DEVICE-WIDE",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "↓ ${FormatUtils.formatBytesPerSec(net.deviceDownBytesPerSec)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NeonEmerald
                        )
                        Text(
                            text = "↑ ${FormatUtils.formatBytesPerSec(net.deviceUpBytesPerSec)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = ElectricCyan
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(ElevatedObsidian)
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Speed,
                                contentDescription = null,
                                tint = UltraViolet,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "THIS APP ONLY",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                color = TextSecondary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "↓ ${FormatUtils.formatBytesPerSec(net.appDownBytesPerSec)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "↑ ${FormatUtils.formatBytesPerSec(net.appUpBytesPerSec)}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                if (net.lastEventMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Last network transition: ${net.lastEventMessage}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Volume & Brightness Controls
        item {
            GlassCard(modifier = Modifier.widthIn(max = 640.dp)) {
                SectionHeader(
                    title = "Volume & Display Brightness",
                    subtitle = "Legitimate AudioManager & Settings.System controls"
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Media Volume",
                        tint = AmberPulse
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Media Volume",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${vb.mediaVolume} / ${vb.maxMediaVolume}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = AmberPulse
                    )
                }
                Slider(
                    value = vb.mediaVolume.toFloat(),
                    onValueChange = { onSetMediaVolume(it.toInt()) },
                    valueRange = 0f..vb.maxMediaVolume.coerceAtLeast(1).toFloat(),
                    modifier = Modifier.testTag("slider_media_volume"),
                    colors = SliderDefaults.colors(
                        thumbColor = AmberPulse,
                        activeTrackColor = AmberPulse,
                        inactiveTrackColor = GlassBorder
                    )
                )

                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.BrightnessHigh,
                        contentDescription = "Display Brightness",
                        tint = ElectricCyan
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Screen Brightness",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${vb.brightnessPercent}%${if (vb.isAutoBrightness) " (Auto)" else ""}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = ElectricCyan
                    )
                }
                Slider(
                    value = vb.brightnessPercent.toFloat(),
                    onValueChange = { onSetBrightness(it.toInt()) },
                    enabled = vb.canWriteSettings,
                    valueRange = 1f..100f,
                    modifier = Modifier.testTag("slider_brightness"),
                    colors = SliderDefaults.colors(
                        thumbColor = ElectricCyan,
                        activeTrackColor = ElectricCyan,
                        inactiveTrackColor = GlassBorder
                    )
                )
                if (!vb.canWriteSettings) {
                    Spacer(Modifier.height(6.dp))
                    Button(
                        onClick = { PermissionHelper.openWriteSettings(context) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = ElectricCyan.copy(alpha = 0.18f),
                            contentColor = ElectricCyan
                        )
                    ) {
                        Text("Allow Modify System Settings to Adjust Brightness")
                    }
                }
            }
        }

        // Notification Center Live Feed
        item {
            GlassCard(modifier = Modifier.widthIn(max = 640.dp)) {
                SectionHeader(
                    title = "Live Notification Center",
                    subtitle = if (uiState.permissions.hasNotificationAccess) {
                        "Tap any notification to open its app or dismiss it"
                    } else {
                        "Enable Notification Access to display WhatsApp, Telegram, Instagram & SMS"
                    },
                    trailing = {
                        StatusBadgePill(
                            label = "${telemetry.notifications.size} ACTIVE",
                            isPositive = telemetry.notifications.isNotEmpty(),
                            positiveColor = UltraViolet
                        )
                    }
                )
                Spacer(Modifier.height(10.dp))
                if (!uiState.permissions.hasNotificationAccess) {
                    Button(
                        onClick = onNavigateToSetup,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = UltraViolet,
                            contentColor = Color.White
                        )
                    ) {
                        Text("Grant Notification Access")
                    }
                } else if (telemetry.notifications.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(ElevatedObsidian)
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = null,
                                tint = UltraViolet,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "No active filtered notifications right now.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        telemetry.notifications.forEach { item ->
                            NotificationFeedRow(
                                item = item,
                                onOpen = { onOpenNotification(item) },
                                onDismiss = { onDismissNotification(item.key) }
                            )
                        }
                    }
                }
            }
        }

        // Per-App Notification Filters
        item {
            GlassCard(modifier = Modifier.widthIn(max = 640.dp)) {
                SectionHeader(
                    title = "Per-App Island Filters",
                    subtitle = "Choose which messaging, social, and media apps trigger the pill"
                )
                Spacer(Modifier.height(8.dp))
                uiState.monitoredApps.forEachIndexed { idx, appOption ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onToggleMonitoredPackage(appOption.packageName, !appOption.isEnabled)
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = appOption.displayName,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = TextPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = if (appOption.isInstalled) "Installed" else "Not installed",
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontSize = 10.sp,
                                    color = if (appOption.isInstalled) NeonEmerald else TextSecondary
                                )
                            }
                            Text(
                                text = "${appOption.category} • ${appOption.packageName}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Switch(
                            checked = appOption.isEnabled,
                            onCheckedChange = {
                                onToggleMonitoredPackage(appOption.packageName, it)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = VoidBlack,
                                checkedTrackColor = ElectricCyan
                            )
                        )
                    }
                    if (idx < uiState.monitoredApps.lastIndex) {
                        HorizontalDivider(color = GlassBorder.copy(alpha = 0.45f))
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationFeedRow(
    item: IslandNotificationItem,
    onOpen: () -> Unit,
    onDismiss: () -> Unit
) {
    val iconBitmap = remember(item.appIcon) {
        item.appIcon?.let { runCatching { it.toBitmap(96, 96).asImageBitmap() }.getOrNull() }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(ElevatedObsidian)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = item.appName,
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Notifications,
                contentDescription = item.appName,
                tint = UltraViolet,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.text.ifBlank { item.appName },
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onOpen) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open app",
                tint = ElectricCyan,
                modifier = Modifier.size(18.dp)
            )
        }
        if (item.isDismissible) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss notification",
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
