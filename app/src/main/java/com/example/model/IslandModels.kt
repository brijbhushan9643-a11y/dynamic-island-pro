package com.example.model

import android.app.PendingIntent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable

enum class IslandThemePreset(
    val displayName: String,
    val backgroundColorHex: Long,
    val borderColorHex: Long,
    val glowColorHex: Long,
    val transparency: Float,
    val glowIntensity: Float
) {
    AMOLED_BLACK(
        displayName = "AMOLED Black",
        backgroundColorHex = 0xFF000000,
        borderColorHex = 0xFF1F2937,
        glowColorHex = 0xFF00E5FF,
        transparency = 0.98f,
        glowIntensity = 0.28f
    ),
    PREMIUM_DARK(
        displayName = "Premium Dark",
        backgroundColorHex = 0xFF0D1119,
        borderColorHex = 0xFF2B374F,
        glowColorHex = 0xFF7C4DFF,
        transparency = 0.94f,
        glowIntensity = 0.45f
    ),
    TRANSPARENT(
        displayName = "Transparent Glass",
        backgroundColorHex = 0xFF0B1018,
        borderColorHex = 0xFF4CC9F0,
        glowColorHex = 0xFF00E5FF,
        transparency = 0.68f,
        glowIntensity = 0.38f
    ),
    NEON(
        displayName = "Neon Pulse",
        backgroundColorHex = 0xFF05070E,
        borderColorHex = 0xFF00E5FF,
        glowColorHex = 0xFF00E5FF,
        transparency = 0.95f,
        glowIntensity = 0.88f
    ),
    CUSTOM(
        displayName = "Custom",
        backgroundColorHex = 0xFF07090F,
        borderColorHex = 0xFF00E676,
        glowColorHex = 0xFF00E676,
        transparency = 0.92f,
        glowIntensity = 0.60f
    )
}

enum class AnimationStyle(val displayName: String) {
    FLUID_SPRING("Fluid Spring"),
    ELASTIC_BOUNCE("Elastic Bounce"),
    CRISP_SNAP("Crisp Snap"),
    MINIMAL_FADE("Minimal Fade")
}

enum class IconStyle(val displayName: String) {
    FILLED("Filled"),
    OUTLINED("Outlined"),
    DUOTONE_NEON("Neon Glow")
}

enum class IslandDisplayMode(val label: String) {
    IDLE("Clock & Status"),
    MUSIC("Media Player"),
    BATTERY("Battery & Charge"),
    CALL("Call HUD"),
    NOTIFICATION("Notifications"),
    NETWORK("Network Speed"),
    VOLUME_BRIGHTNESS("Volume & Bright")
}

data class IslandSettings(
    val isIslandEnabled: Boolean = false,
    val themePreset: IslandThemePreset = IslandThemePreset.AMOLED_BLACK,
    val pillWidthDp: Float = 196f,
    val pillHeightDp: Float = 38f,
    val offsetXFraction: Float = 0f,
    val offsetYDp: Float = 10f,
    val cornerRadiusDp: Float = 24f,
    val transparency: Float = 0.96f,
    val glowIntensity: Float = 0.40f,
    val backgroundColorHex: Long = 0xFF000000,
    val borderColorHex: Long = 0xFF1F2937,
    val glowColorHex: Long = 0xFF00E5FF,
    val animationSpeed: Float = 1.0f,
    val animationStyle: AnimationStyle = AnimationStyle.FLUID_SPRING,
    val textSizeSp: Float = 13f,
    val iconStyle: IconStyle = IconStyle.DUOTONE_NEON,
    val musicPlayerEnabled: Boolean = true,
    val batteryMonitorEnabled: Boolean = true,
    val callMonitorEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val networkMonitorEnabled: Boolean = true,
    val volumeBrightnessEnabled: Boolean = true,
    val hapticFeedbackEnabled: Boolean = true,
    val showNotificationContent: Boolean = true,
    val hideSensitiveOnLock: Boolean = true,
    val monitoredPackages: Set<String> = DEFAULT_MONITORED_PACKAGES
) {
    companion object {
        val DEFAULT_MONITORED_PACKAGES = setOf(
            "com.whatsapp",
            "com.instagram.android",
            "org.telegram.messenger",
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.google.android.gm",
            "com.discord"
        )
    }
}

data class BatteryState(
    val level: Int = 0,
    val isCharging: Boolean = false,
    val isFull: Boolean = false,
    val isLow: Boolean = false,
    val temperatureCelsius: Float = 0f,
    val voltageMv: Int = 0,
    val pluggedLabel: String = "On Battery",
    val healthLabel: String = "Good",
    val hasValidReading: Boolean = false
)

data class MediaState(
    val isActive: Boolean = false,
    val isPlaying: Boolean = false,
    val title: String = "",
    val artist: String = "",
    val packageName: String = "",
    val appName: String = "",
    val albumArt: Bitmap? = null,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val canPlayPause: Boolean = false,
    val canSkipNext: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val hasMediaAccess: Boolean = false
)

data class CallState(
    val isActive: Boolean = false,
    val isIncoming: Boolean = false,
    val callerName: String = "",
    val appName: String = "",
    val packageName: String = "",
    val startedAtElapsedMs: Long = 0L,
    val statusText: String = ""
)

data class IslandNotificationItem(
    val key: String,
    val packageName: String,
    val appName: String,
    val title: String,
    val text: String,
    val timestampMs: Long,
    val appIcon: Drawable? = null,
    val contentIntent: PendingIntent? = null,
    val isContentHidden: Boolean = false,
    val isDismissible: Boolean = true
)

data class NetworkState(
    val isConnected: Boolean = false,
    val isValidated: Boolean = false,
    val transportLabel: String = "Offline",
    val isWifi: Boolean = false,
    val isCellular: Boolean = false,
    val deviceDownBytesPerSec: Long = 0L,
    val deviceUpBytesPerSec: Long = 0L,
    val appDownBytesPerSec: Long = 0L,
    val appUpBytesPerSec: Long = 0L,
    val lastEventMessage: String? = null
)

data class VolumeBrightnessState(
    val mediaVolume: Int = 0,
    val maxMediaVolume: Int = 15,
    val ringVolume: Int = 0,
    val maxRingVolume: Int = 7,
    val brightnessRaw: Int = 128,
    val brightnessPercent: Int = 50,
    val isAutoBrightness: Boolean = false,
    val canWriteSettings: Boolean = false
)

data class PermissionStatus(
    val hasOverlayPermission: Boolean = false,
    val hasNotificationAccess: Boolean = false,
    val hasPostNotifications: Boolean = false,
    val hasWriteSettings: Boolean = false,
    val isIgnoringBatteryOptimizations: Boolean = false,
    val androidVersionLabel: String = "",
    val sdkInt: Int = 0,
    val deviceModel: String = ""
)

data class MonitoredAppOption(
    val packageName: String,
    val displayName: String,
    val category: String,
    val isInstalled: Boolean,
    val isEnabled: Boolean
)

data class SystemTelemetrySnapshot(
    val battery: BatteryState = BatteryState(),
    val media: MediaState = MediaState(),
    val call: CallState = CallState(),
    val notifications: List<IslandNotificationItem> = emptyList(),
    val network: NetworkState = NetworkState(),
    val volumeBrightness: VolumeBrightnessState = VolumeBrightnessState(),
    val activeMode: IslandDisplayMode = IslandDisplayMode.IDLE,
    val transientMode: IslandDisplayMode? = null,
    val transientTitle: String = "",
    val transientSubtitle: String = "",
    val listenerConnected: Boolean = false
)
