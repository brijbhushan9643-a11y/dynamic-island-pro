package com.example.ui.components

import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.R
import com.example.model.AnimationStyle
import com.example.model.IconStyle
import com.example.model.IslandDisplayMode
import com.example.model.IslandNotificationItem
import com.example.model.IslandSettings
import com.example.model.SystemTelemetrySnapshot
import com.example.system.FormatUtils
import com.example.ui.theme.AmberPulse
import com.example.ui.theme.CrimsonAlert
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.PlusJakartaSansFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.UltraViolet
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class IslandCallbacks(
    val onToggleExpand: () -> Unit = {},
    val onPlayPause: () -> Unit = {},
    val onSkipNext: () -> Unit = {},
    val onSkipPrevious: () -> Unit = {},
    val onOpenMediaApp: () -> Unit = {},
    val onOpenNotification: (IslandNotificationItem) -> Unit = {},
    val onDismissNotification: (String) -> Unit = {},
    val onSetMediaVolume: (Int) -> Unit = {},
    val onSetBrightness: (Int) -> Unit = {}
)

@Composable
fun DynamicIslandPill(
    settings: IslandSettings,
    telemetry: SystemTelemetrySnapshot,
    isExpanded: Boolean,
    displayModeOverride: IslandDisplayMode? = null,
    callbacks: IslandCallbacks = IslandCallbacks(),
    modifier: Modifier = Modifier
) {
    val activeMode = displayModeOverride ?: telemetry.activeMode
    val bgColor = Color(settings.backgroundColorHex).copy(alpha = settings.transparency)
    val borderColor = Color(settings.borderColorHex)
    val glowColor = Color(settings.glowColorHex)

    val stiffness = when (settings.animationStyle) {
        AnimationStyle.FLUID_SPRING -> Spring.StiffnessMediumLow * settings.animationSpeed
        AnimationStyle.ELASTIC_BOUNCE -> Spring.StiffnessLow * settings.animationSpeed
        AnimationStyle.CRISP_SNAP -> Spring.StiffnessHigh * settings.animationSpeed
        AnimationStyle.MINIMAL_FADE -> Spring.StiffnessMedium * settings.animationSpeed
    }
    val dampingRatio = when (settings.animationStyle) {
        AnimationStyle.FLUID_SPRING -> Spring.DampingRatioLowBouncy
        AnimationStyle.ELASTIC_BOUNCE -> Spring.DampingRatioMediumBouncy
        AnimationStyle.CRISP_SNAP -> Spring.DampingRatioNoBouncy
        AnimationStyle.MINIMAL_FADE -> Spring.DampingRatioNoBouncy
    }

    val targetWidth: Dp = if (isExpanded) {
        336.dp
    } else {
        settings.pillWidthDp.dp
    }
    val targetHeight: Dp = if (isExpanded) {
        when (activeMode) {
            IslandDisplayMode.MUSIC -> 154.dp
            IslandDisplayMode.NOTIFICATION -> 142.dp
            IslandDisplayMode.VOLUME_BRIGHTNESS -> 148.dp
            IslandDisplayMode.BATTERY -> 128.dp
            IslandDisplayMode.CALL -> 124.dp
            IslandDisplayMode.NETWORK -> 128.dp
            IslandDisplayMode.IDLE -> 112.dp
        }
    } else {
        settings.pillHeightDp.dp
    }
    val targetCorner: Dp = if (isExpanded) {
        (settings.cornerRadiusDp + 6f).coerceIn(18f, 34f).dp
    } else {
        settings.cornerRadiusDp.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
        label = "island_width"
    )
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
        label = "island_height"
    )
    val animatedCorner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = spring(dampingRatio = dampingRatio, stiffness = stiffness),
        label = "island_corner"
    )

    val pillShape = RoundedCornerShape(animatedCorner)
    val cdPill = stringResource(R.string.cd_island_pill)

    Box(
        modifier = modifier
            .widthIn(min = 130.dp, max = 356.dp)
            .width(animatedWidth)
            .height(animatedHeight)
            .drawBehind {
                if (settings.glowIntensity > 0.02f) {
                    val cornerPx = animatedCorner.toPx()
                    val spread = (10.dp.toPx()) * settings.glowIntensity
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.42f * settings.glowIntensity),
                                glowColor.copy(alpha = 0.12f * settings.glowIntensity),
                                Color.Transparent
                            ),
                            center = center,
                            radius = (size.maxDimension * 0.72f) + spread
                        ),
                        cornerRadius = CornerRadius(cornerPx + spread, cornerPx + spread)
                    )
                    drawRoundRect(
                        color = glowColor.copy(alpha = 0.35f * settings.glowIntensity),
                        cornerRadius = CornerRadius(cornerPx, cornerPx),
                        style = Stroke(width = (1.5f + 2f * settings.glowIntensity).dp.toPx())
                    )
                }
            }
            .shadow(
                elevation = (8f + 12f * settings.glowIntensity).dp,
                shape = pillShape,
                ambientColor = glowColor,
                spotColor = glowColor
            )
            .clip(pillShape)
            .background(bgColor)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        borderColor.copy(alpha = 0.85f),
                        glowColor.copy(alpha = 0.45f * settings.glowIntensity + 0.15f)
                    )
                ),
                shape = pillShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = callbacks.onToggleExpand
            )
            .semantics { contentDescription = cdPill }
            .testTag("dynamic_island_pill")
            .padding(horizontal = if (isExpanded) 16.dp else 12.dp, vertical = if (isExpanded) 12.dp else 6.dp),
        contentAlignment = Alignment.Center
    ) {
        val transitionDuration = (240 / settings.animationSpeed).toInt().coerceIn(100, 450)
        AnimatedContent(
            targetState = Pair(isExpanded, activeMode),
            transitionSpec = {
                fadeIn(tween(transitionDuration)) togetherWith fadeOut(tween(transitionDuration / 2))
            },
            label = "island_content"
        ) { (expanded, mode) ->
            if (!expanded) {
                CollapsedPillContent(
                    mode = mode,
                    settings = settings,
                    telemetry = telemetry
                )
            } else {
                ExpandedPillContent(
                    mode = mode,
                    settings = settings,
                    telemetry = telemetry,
                    callbacks = callbacks
                )
            }
        }
    }
}

@Composable
private fun CollapsedPillContent(
    mode: IslandDisplayMode,
    settings: IslandSettings,
    telemetry: SystemTelemetrySnapshot
) {
    val textSize = settings.textSizeSp.sp
    val iconColor = when (mode) {
        IslandDisplayMode.CALL -> NeonEmerald
        IslandDisplayMode.BATTERY -> if (telemetry.battery.isLow) CrimsonAlert else NeonEmerald
        IslandDisplayMode.NETWORK -> if (telemetry.network.isConnected) ElectricCyan else AmberPulse
        IslandDisplayMode.MUSIC -> ElectricCyan
        IslandDisplayMode.NOTIFICATION -> UltraViolet
        IslandDisplayMode.VOLUME_BRIGHTNESS -> AmberPulse
        IslandDisplayMode.IDLE -> Color(settings.glowColorHex)
    }

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when (mode) {
            IslandDisplayMode.MUSIC -> {
                val media = telemetry.media
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (media.albumArt != null) {
                        Image(
                            bitmap = media.albumArt.asImageBitmap(),
                            contentDescription = "Album art",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        StyledModeIcon(
                            filled = Icons.Filled.MusicNote,
                            outlined = Icons.Outlined.MusicNote,
                            iconStyle = settings.iconStyle,
                            tint = iconColor,
                            contentDescription = "Music"
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = media.title.ifBlank { "No Active Audio" },
                        color = Color.White,
                        fontSize = textSize,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(Modifier.width(8.dp))
                AudioWaveformBars(
                    isAnimating = media.isPlaying,
                    accentColor = iconColor,
                    barCount = 4,
                    maxHeight = 16.dp
                )
            }

            IslandDisplayMode.BATTERY -> {
                val battery = telemetry.battery
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    ChargingPulseIndicator(
                        isCharging = battery.isCharging,
                        isLow = battery.isLow,
                        iconStyle = settings.iconStyle
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (battery.isCharging) "Charging" else if (battery.isLow) "Low Battery" else "Battery",
                        color = Color.White,
                        fontSize = textSize,
                        fontFamily = PlusJakartaSansFontFamily,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${battery.level}%",
                    color = if (battery.isLow) CrimsonAlert else NeonEmerald,
                    fontSize = textSize,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            IslandDisplayMode.CALL -> {
                val call = telemetry.call
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    PulsingDotBadge(color = NeonEmerald)
                    Spacer(Modifier.width(6.dp))
                    StyledModeIcon(
                        filled = Icons.Filled.Call,
                        outlined = Icons.Outlined.Call,
                        iconStyle = settings.iconStyle,
                        tint = NeonEmerald,
                        contentDescription = "Call"
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = call.callerName.ifBlank { "No Active Call" },
                        color = Color.White,
                        fontSize = textSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (call.isActive) {
                    LiveCallTimerText(startedAtElapsedMs = call.startedAtElapsedMs)
                }
            }

            IslandDisplayMode.NOTIFICATION -> {
                val latest = telemetry.notifications.firstOrNull()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    if (latest?.appIcon != null) {
                        DrawableIcon(drawable = latest.appIcon, size = 20.dp, description = latest.appName)
                    } else {
                        StyledModeIcon(
                            filled = Icons.Filled.Notifications,
                            outlined = Icons.Outlined.Notifications,
                            iconStyle = settings.iconStyle,
                            tint = UltraViolet,
                            contentDescription = "Notification"
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = latest?.title ?: telemetry.transientTitle.ifBlank { "No Notifications" },
                        color = Color.White,
                        fontSize = textSize,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (telemetry.notifications.size > 1) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(UltraViolet.copy(alpha = 0.25f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${telemetry.notifications.size}",
                            color = UltraViolet,
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            IslandDisplayMode.NETWORK -> {
                val net = telemetry.network
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    StyledModeIcon(
                        filled = if (net.isConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                        outlined = if (net.isConnected) Icons.Outlined.Wifi else Icons.Filled.WifiOff,
                        iconStyle = settings.iconStyle,
                        tint = iconColor,
                        contentDescription = "Network"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = net.transportLabel,
                        color = Color.White,
                        fontSize = textSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                }
                Text(
                    text = if (net.isConnected) FormatUtils.formatBytesPerSec(net.deviceDownBytesPerSec) else "Offline",
                    color = iconColor,
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Bold
                )
            }

            IslandDisplayMode.VOLUME_BRIGHTNESS -> {
                val vb = telemetry.volumeBrightness
                val volPct = ((vb.mediaVolume * 100f) / vb.maxMediaVolume.coerceAtLeast(1)).toInt()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Volume",
                        tint = AmberPulse,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = telemetry.transientTitle.ifBlank { "Vol $volPct%" },
                        color = Color.White,
                        fontSize = textSize,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${vb.brightnessPercent}%",
                    color = ElectricCyan,
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFontFamily
                )
            }

            IslandDisplayMode.IDLE -> {
                val timeText = rememberCurrentTimeLabel()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    PulsingDotBadge(color = Color(settings.glowColorHex))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = timeText,
                        color = Color.White,
                        fontSize = textSize,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "${telemetry.battery.level}%",
                    color = if (telemetry.battery.isCharging) NeonEmerald else Color(0xFFB5C0D4),
                    fontSize = 11.sp,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ExpandedPillContent(
    mode: IslandDisplayMode,
    settings: IslandSettings,
    telemetry: SystemTelemetrySnapshot,
    callbacks: IslandCallbacks
) {
    val glowColor = Color(settings.glowColorHex)
    when (mode) {
        IslandDisplayMode.MUSIC -> {
            val media = telemetry.media
            val progress = if (media.durationMs > 0L) {
                (media.positionMs.toFloat() / media.durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF161D2B))
                            .clickable(onClick = callbacks.onOpenMediaApp),
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
                                tint = ElectricCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (media.isActive) media.title else "No Active Media Session",
                            color = Color.White,
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = (settings.textSizeSp + 2f).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (media.isActive) {
                                media.artist.ifBlank { media.appName }
                            } else if (!media.hasMediaAccess) {
                                "Enable Notification Access in Setup"
                            } else {
                                "Play audio in Spotify, YT Music, etc."
                            },
                            color = Color(0xFF9AA7BD),
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    AudioWaveformBars(
                        isAnimating = media.isPlaying,
                        accentColor = glowColor,
                        barCount = 5,
                        maxHeight = 22.dp
                    )
                }

                Column {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(CircleShape),
                        color = glowColor,
                        trackColor = Color.White.copy(alpha = 0.14f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = FormatUtils.formatDurationMs(media.positionMs),
                            color = Color(0xFF8C99B0),
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = callbacks.onSkipPrevious,
                                enabled = media.isActive,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipPrevious,
                                    contentDescription = stringResource(R.string.cd_prev_track),
                                    tint = if (media.isActive) Color.White else Color.Gray
                                )
                            }
                            IconButton(
                                onClick = callbacks.onPlayPause,
                                enabled = media.isActive,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(glowColor.copy(alpha = 0.22f))
                            ) {
                                Icon(
                                    imageVector = if (media.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                    contentDescription = stringResource(R.string.cd_play_pause),
                                    tint = if (media.isActive) Color.White else Color.Gray
                                )
                            }
                            IconButton(
                                onClick = callbacks.onSkipNext,
                                enabled = media.isActive,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SkipNext,
                                    contentDescription = stringResource(R.string.cd_next_track),
                                    tint = if (media.isActive) Color.White else Color.Gray
                                )
                            }
                        }
                        Text(
                            text = FormatUtils.formatDurationMs(media.durationMs),
                            color = Color(0xFF8C99B0),
                            fontSize = 10.sp,
                            fontFamily = JetBrainsMonoFontFamily
                        )
                    }
                }
            }
        }

        IslandDisplayMode.BATTERY -> {
            val b = telemetry.battery
            val accent = if (b.isLow) CrimsonAlert else NeonEmerald
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ChargingPulseIndicator(
                            isCharging = b.isCharging,
                            isLow = b.isLow,
                            iconStyle = settings.iconStyle
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = when {
                                    b.isFull -> "Battery Full"
                                    b.isCharging -> "Charging Active"
                                    b.isLow -> "Low Battery Warning"
                                    else -> "Battery Status"
                                },
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "${b.pluggedLabel} • ${b.temperatureCelsius}°C • ${b.healthLabel}",
                                color = Color(0xFF9AA7BD),
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }
                    }
                    Text(
                        text = "${b.level}%",
                        color = accent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    )
                }
                LinearProgressIndicator(
                    progress = { (b.level / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.12f)
                )
            }
        }

        IslandDisplayMode.CALL -> {
            val call = telemetry.call
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NeonEmerald.copy(alpha = 0.18f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Call",
                                tint = NeonEmerald
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (call.isActive) call.callerName else "No Active Call",
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (call.isActive) {
                                    "${call.appName} • ${call.statusText}"
                                } else {
                                    "Incoming & active calls appear here automatically"
                                },
                                color = Color(0xFF9AA7BD),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (call.isActive) {
                        LiveCallTimerText(startedAtElapsedMs = call.startedAtElapsedMs)
                    }
                }
                Text(
                    text = "System-compliant HUD • Default phone controls preserved",
                    color = Color(0xFF7B889E),
                    fontSize = 10.sp
                )
            }
        }

        IslandDisplayMode.NOTIFICATION -> {
            val item = telemetry.notifications.firstOrNull()
            if (item == null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Notifications,
                        contentDescription = "No notifications",
                        tint = UltraViolet
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = if (telemetry.listenerConnected) "No active notifications" else "Enable Notification Access in Setup",
                        color = Color(0xFF9AA7BD),
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { callbacks.onOpenNotification(item) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (item.appIcon != null) {
                            DrawableIcon(drawable = item.appIcon, size = 38.dp, description = item.appName)
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = item.appName,
                                tint = UltraViolet,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.text.ifBlank { item.appName },
                                color = Color(0xFFB5C0D4),
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (item.isDismissible) {
                            IconButton(
                                onClick = { callbacks.onDismissNotification(item.key) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.cd_dismiss_notif),
                                    tint = Color(0xFF9AA7BD),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${item.appName} • Tap card to open",
                            color = ElectricCyan,
                            fontSize = 11.sp
                        )
                        if (telemetry.notifications.size > 1) {
                            Text(
                                text = "+${telemetry.notifications.size - 1} more",
                                color = Color(0xFF9AA7BD),
                                fontSize = 11.sp,
                                fontFamily = JetBrainsMonoFontFamily
                            )
                        }
                    }
                }
            }
        }

        IslandDisplayMode.NETWORK -> {
            val net = telemetry.network
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (net.isConnected) Icons.Filled.Wifi else Icons.Filled.WifiOff,
                            contentDescription = "Network",
                            tint = if (net.isConnected) ElectricCyan else AmberPulse
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${net.transportLabel} • ${if (net.isValidated) "Online" else if (net.isConnected) "Limited" else "Disconnected"}",
                                color = Color.White,
                                fontFamily = SpaceGroteskFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Device-wide traffic rate",
                                color = Color(0xFF8C99B0),
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "↓ ${FormatUtils.formatBytesPerSec(net.deviceDownBytesPerSec)}",
                        color = NeonEmerald,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "↑ ${FormatUtils.formatBytesPerSec(net.deviceUpBytesPerSec)}",
                        color = ElectricCyan,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "App ↓ ${FormatUtils.formatBytesPerSec(net.appDownBytesPerSec)}",
                        color = Color(0xFF9AA7BD),
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp
                    )
                }
            }
        }

        IslandDisplayMode.VOLUME_BRIGHTNESS -> {
            val vb = telemetry.volumeBrightness
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceEvenly
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = "Media Volume",
                        tint = AmberPulse,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = vb.mediaVolume.toFloat(),
                        onValueChange = { callbacks.onSetMediaVolume(it.toInt()) },
                        valueRange = 0f..vb.maxMediaVolume.coerceAtLeast(1).toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = AmberPulse,
                            activeTrackColor = AmberPulse
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${vb.mediaVolume}/${vb.maxMediaVolume}",
                        color = Color.White,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        modifier = Modifier.width(42.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.BrightnessHigh,
                        contentDescription = "Brightness",
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Slider(
                        value = vb.brightnessPercent.toFloat(),
                        onValueChange = {
                            if (vb.canWriteSettings) callbacks.onSetBrightness(it.toInt())
                        },
                        enabled = vb.canWriteSettings,
                        valueRange = 1f..100f,
                        colors = SliderDefaults.colors(
                            thumbColor = ElectricCyan,
                            activeTrackColor = ElectricCyan
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (vb.canWriteSettings) "${vb.brightnessPercent}%" else "Read",
                        color = Color.White,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        modifier = Modifier.width(42.dp)
                    )
                }
            }
        }

        IslandDisplayMode.IDLE -> {
            val timeText = rememberCurrentTimeLabel()
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = timeText,
                        color = Color.White,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "${telemetry.network.transportLabel} • ${telemetry.battery.pluggedLabel}",
                        color = Color(0xFF9AA7BD),
                        fontSize = 12.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${telemetry.battery.level}%",
                        color = if (telemetry.battery.isCharging) NeonEmerald else ElectricCyan,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "Tap to collapse",
                        color = Color(0xFF7B889E),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StyledModeIcon(
    filled: ImageVector,
    outlined: ImageVector,
    iconStyle: IconStyle,
    tint: Color,
    contentDescription: String
) {
    val vector = if (iconStyle == IconStyle.OUTLINED) outlined else filled
    if (iconStyle == IconStyle.DUOTONE_NEON) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = vector,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(15.dp)
            )
        }
    } else {
        Icon(
            imageVector = vector,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun AudioWaveformBars(
    isAnimating: Boolean,
    accentColor: Color,
    barCount: Int = 5,
    maxHeight: Dp = 18.dp
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (Math.PI * 2).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        modifier = Modifier.height(maxHeight)
    ) {
        for (i in 0 until barCount) {
            val fraction = if (isAnimating) {
                0.35f + 0.65f * ((kotlin.math.sin(phase + i * 1.15f) + 1f) / 2f)
            } else {
                0.28f
            }
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height(maxHeight * fraction)
                    .clip(CircleShape)
                    .background(accentColor)
            )
        }
    }
}

@Composable
private fun ChargingPulseIndicator(
    isCharging: Boolean,
    isLow: Boolean,
    iconStyle: IconStyle
) {
    val transition = rememberInfiniteTransition(label = "charge_pulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(950, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )
    val tint = when {
        isLow -> CrimsonAlert
        isCharging -> NeonEmerald.copy(alpha = pulseAlpha)
        else -> ElectricCyan
    }
    val vector = when {
        isCharging -> Icons.Filled.BatteryChargingFull
        isLow -> Icons.Filled.BatteryStd
        iconStyle == IconStyle.OUTLINED -> Icons.Outlined.BatteryStd
        else -> Icons.Filled.BatteryFull
    }
    Icon(
        imageVector = vector,
        contentDescription = "Battery",
        tint = tint,
        modifier = Modifier.size(19.dp)
    )
}

@Composable
private fun PulsingDotBadge(color: Color) {
    val transition = rememberInfiniteTransition(label = "dot_pulse")
    val scaleAlpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot_alpha"
    )
    Canvas(modifier = Modifier.size(8.dp)) {
        drawCircle(
            color = color.copy(alpha = scaleAlpha),
            radius = size.minDimension / 2f
        )
    }
}

@Composable
private fun LiveCallTimerText(startedAtElapsedMs: Long) {
    var elapsedSec by remember(startedAtElapsedMs) {
        mutableLongStateOf(((SystemClock.elapsedRealtime() - startedAtElapsedMs) / 1000L).coerceAtLeast(0L))
    }
    LaunchedEffect(startedAtElapsedMs) {
        while (true) {
            elapsedSec = ((SystemClock.elapsedRealtime() - startedAtElapsedMs) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }
    Text(
        text = FormatUtils.formatElapsedSeconds(elapsedSec),
        color = NeonEmerald,
        fontFamily = JetBrainsMonoFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )
}

@Composable
private fun DrawableIcon(drawable: Drawable, size: Dp, description: String) {
    val bitmap = remember(drawable) {
        runCatching { drawable.toBitmap(96, 96).asImageBitmap() }.getOrNull()
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = description,
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(8.dp))
        )
    } else {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = description,
            tint = UltraViolet,
            modifier = Modifier.size(size)
        )
    }
}

@Composable
private fun rememberCurrentTimeLabel(): String {
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowMs = System.currentTimeMillis()
            delay(15_000L)
        }
    }
    val formatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    return formatter.format(Date(nowMs))
}
