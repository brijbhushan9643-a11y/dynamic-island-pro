package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AnimationStyle
import com.example.model.IconStyle
import com.example.model.IslandSettings
import com.example.model.IslandThemePreset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.islandDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "dynamic_island_pro_prefs"
)

class IslandSettingsRepository(private val context: Context) {

    private object Keys {
        val ISLAND_ENABLED = booleanPreferencesKey("island_enabled")
        val THEME_PRESET = stringPreferencesKey("theme_preset")
        val PILL_WIDTH_DP = floatPreferencesKey("pill_width_dp")
        val PILL_HEIGHT_DP = floatPreferencesKey("pill_height_dp")
        val OFFSET_X_FRACTION = floatPreferencesKey("offset_x_fraction")
        val OFFSET_Y_DP = floatPreferencesKey("offset_y_dp")
        val CORNER_RADIUS_DP = floatPreferencesKey("corner_radius_dp")
        val TRANSPARENCY = floatPreferencesKey("transparency")
        val GLOW_INTENSITY = floatPreferencesKey("glow_intensity")
        val BG_COLOR_HEX = longPreferencesKey("bg_color_hex")
        val BORDER_COLOR_HEX = longPreferencesKey("border_color_hex")
        val GLOW_COLOR_HEX = longPreferencesKey("glow_color_hex")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val ANIMATION_STYLE = stringPreferencesKey("animation_style")
        val TEXT_SIZE_SP = floatPreferencesKey("text_size_sp")
        val ICON_STYLE = stringPreferencesKey("icon_style")
        val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
        val BATTERY_ENABLED = booleanPreferencesKey("battery_enabled")
        val CALL_ENABLED = booleanPreferencesKey("call_enabled")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val NETWORK_ENABLED = booleanPreferencesKey("network_enabled")
        val VOLUME_BRIGHTNESS_ENABLED = booleanPreferencesKey("volume_brightness_enabled")
        val HAPTIC_ENABLED = booleanPreferencesKey("haptic_enabled")
        val SHOW_NOTIF_CONTENT = booleanPreferencesKey("show_notif_content")
        val HIDE_SENSITIVE_LOCK = booleanPreferencesKey("hide_sensitive_lock")
        val MONITORED_PACKAGES = stringSetPreferencesKey("monitored_packages")
    }

    val settingsFlow: Flow<IslandSettings> = context.islandDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val preset = prefs[Keys.THEME_PRESET]
                ?.let { runCatching { IslandThemePreset.valueOf(it) }.getOrNull() }
                ?: IslandThemePreset.AMOLED_BLACK
            val animStyle = prefs[Keys.ANIMATION_STYLE]
                ?.let { runCatching { AnimationStyle.valueOf(it) }.getOrNull() }
                ?: AnimationStyle.FLUID_SPRING
            val iconStyle = prefs[Keys.ICON_STYLE]
                ?.let { runCatching { IconStyle.valueOf(it) }.getOrNull() }
                ?: IconStyle.DUOTONE_NEON

            IslandSettings(
                isIslandEnabled = prefs[Keys.ISLAND_ENABLED] ?: false,
                themePreset = preset,
                pillWidthDp = prefs[Keys.PILL_WIDTH_DP] ?: 196f,
                pillHeightDp = prefs[Keys.PILL_HEIGHT_DP] ?: 38f,
                offsetXFraction = prefs[Keys.OFFSET_X_FRACTION] ?: 0f,
                offsetYDp = prefs[Keys.OFFSET_Y_DP] ?: 10f,
                cornerRadiusDp = prefs[Keys.CORNER_RADIUS_DP] ?: 24f,
                transparency = prefs[Keys.TRANSPARENCY] ?: 0.96f,
                glowIntensity = prefs[Keys.GLOW_INTENSITY] ?: 0.40f,
                backgroundColorHex = prefs[Keys.BG_COLOR_HEX] ?: preset.backgroundColorHex,
                borderColorHex = prefs[Keys.BORDER_COLOR_HEX] ?: preset.borderColorHex,
                glowColorHex = prefs[Keys.GLOW_COLOR_HEX] ?: preset.glowColorHex,
                animationSpeed = prefs[Keys.ANIMATION_SPEED] ?: 1.0f,
                animationStyle = animStyle,
                textSizeSp = prefs[Keys.TEXT_SIZE_SP] ?: 13f,
                iconStyle = iconStyle,
                musicPlayerEnabled = prefs[Keys.MUSIC_ENABLED] ?: true,
                batteryMonitorEnabled = prefs[Keys.BATTERY_ENABLED] ?: true,
                callMonitorEnabled = prefs[Keys.CALL_ENABLED] ?: true,
                notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
                networkMonitorEnabled = prefs[Keys.NETWORK_ENABLED] ?: true,
                volumeBrightnessEnabled = prefs[Keys.VOLUME_BRIGHTNESS_ENABLED] ?: true,
                hapticFeedbackEnabled = prefs[Keys.HAPTIC_ENABLED] ?: true,
                showNotificationContent = prefs[Keys.SHOW_NOTIF_CONTENT] ?: true,
                hideSensitiveOnLock = prefs[Keys.HIDE_SENSITIVE_LOCK] ?: true,
                monitoredPackages = prefs[Keys.MONITORED_PACKAGES]
                    ?: IslandSettings.DEFAULT_MONITORED_PACKAGES
            )
        }

    suspend fun setIslandEnabled(enabled: Boolean) {
        context.islandDataStore.edit { it[Keys.ISLAND_ENABLED] = enabled }
    }

    suspend fun applyThemePreset(preset: IslandThemePreset) {
        context.islandDataStore.edit { prefs ->
            prefs[Keys.THEME_PRESET] = preset.name
            if (preset != IslandThemePreset.CUSTOM) {
                prefs[Keys.BG_COLOR_HEX] = preset.backgroundColorHex
                prefs[Keys.BORDER_COLOR_HEX] = preset.borderColorHex
                prefs[Keys.GLOW_COLOR_HEX] = preset.glowColorHex
                prefs[Keys.TRANSPARENCY] = preset.transparency
                prefs[Keys.GLOW_INTENSITY] = preset.glowIntensity
            }
        }
    }

    suspend fun updateDimensions(
        widthDp: Float? = null,
        heightDp: Float? = null,
        offsetXFraction: Float? = null,
        offsetYDp: Float? = null,
        cornerRadiusDp: Float? = null
    ) {
        context.islandDataStore.edit { prefs ->
            widthDp?.let { prefs[Keys.PILL_WIDTH_DP] = it.coerceIn(130f, 340f) }
            heightDp?.let { prefs[Keys.PILL_HEIGHT_DP] = it.coerceIn(30f, 64f) }
            offsetXFraction?.let { prefs[Keys.OFFSET_X_FRACTION] = it.coerceIn(-0.35f, 0.35f) }
            offsetYDp?.let { prefs[Keys.OFFSET_Y_DP] = it.coerceIn(0f, 96f) }
            cornerRadiusDp?.let { prefs[Keys.CORNER_RADIUS_DP] = it.coerceIn(10f, 36f) }
        }
    }

    suspend fun updateAppearance(
        transparency: Float? = null,
        glowIntensity: Float? = null,
        backgroundColorHex: Long? = null,
        borderColorHex: Long? = null,
        glowColorHex: Long? = null,
        textSizeSp: Float? = null,
        iconStyle: IconStyle? = null
    ) {
        context.islandDataStore.edit { prefs ->
            transparency?.let { prefs[Keys.TRANSPARENCY] = it.coerceIn(0.25f, 1.0f) }
            glowIntensity?.let { prefs[Keys.GLOW_INTENSITY] = it.coerceIn(0f, 1.0f) }
            backgroundColorHex?.let {
                prefs[Keys.BG_COLOR_HEX] = it
                prefs[Keys.THEME_PRESET] = IslandThemePreset.CUSTOM.name
            }
            borderColorHex?.let {
                prefs[Keys.BORDER_COLOR_HEX] = it
                prefs[Keys.THEME_PRESET] = IslandThemePreset.CUSTOM.name
            }
            glowColorHex?.let {
                prefs[Keys.GLOW_COLOR_HEX] = it
                prefs[Keys.THEME_PRESET] = IslandThemePreset.CUSTOM.name
            }
            textSizeSp?.let { prefs[Keys.TEXT_SIZE_SP] = it.coerceIn(10f, 18f) }
            iconStyle?.let { prefs[Keys.ICON_STYLE] = it.name }
        }
    }

    suspend fun updateAnimation(
        speed: Float? = null,
        style: AnimationStyle? = null,
        hapticEnabled: Boolean? = null
    ) {
        context.islandDataStore.edit { prefs ->
            speed?.let { prefs[Keys.ANIMATION_SPEED] = it.coerceIn(0.5f, 2.0f) }
            style?.let { prefs[Keys.ANIMATION_STYLE] = it.name }
            hapticEnabled?.let { prefs[Keys.HAPTIC_ENABLED] = it }
        }
    }

    suspend fun updateFeatureToggle(feature: FeatureToggle, enabled: Boolean) {
        context.islandDataStore.edit { prefs ->
            when (feature) {
                FeatureToggle.MUSIC -> prefs[Keys.MUSIC_ENABLED] = enabled
                FeatureToggle.BATTERY -> prefs[Keys.BATTERY_ENABLED] = enabled
                FeatureToggle.CALLS -> prefs[Keys.CALL_ENABLED] = enabled
                FeatureToggle.NOTIFICATIONS -> prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
                FeatureToggle.NETWORK -> prefs[Keys.NETWORK_ENABLED] = enabled
                FeatureToggle.VOLUME_BRIGHTNESS -> prefs[Keys.VOLUME_BRIGHTNESS_ENABLED] = enabled
                FeatureToggle.HAPTIC -> prefs[Keys.HAPTIC_ENABLED] = enabled
                FeatureToggle.SHOW_CONTENT -> prefs[Keys.SHOW_NOTIF_CONTENT] = enabled
                FeatureToggle.HIDE_SENSITIVE_LOCK -> prefs[Keys.HIDE_SENSITIVE_LOCK] = enabled
            }
        }
    }

    suspend fun toggleMonitoredPackage(packageName: String, enabled: Boolean) {
        context.islandDataStore.edit { prefs ->
            val current = (prefs[Keys.MONITORED_PACKAGES]
                ?: IslandSettings.DEFAULT_MONITORED_PACKAGES).toMutableSet()
            if (enabled) current.add(packageName) else current.remove(packageName)
            prefs[Keys.MONITORED_PACKAGES] = current
        }
    }

    suspend fun resetToDefaults() {
        context.islandDataStore.edit { prefs ->
            val wasEnabled = prefs[Keys.ISLAND_ENABLED] ?: false
            prefs.clear()
            prefs[Keys.ISLAND_ENABLED] = wasEnabled
        }
    }

    enum class FeatureToggle {
        MUSIC,
        BATTERY,
        CALLS,
        NOTIFICATIONS,
        NETWORK,
        VOLUME_BRIGHTNESS,
        HAPTIC,
        SHOW_CONTENT,
        HIDE_SENSITIVE_LOCK
    }
}
