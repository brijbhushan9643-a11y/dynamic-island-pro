package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.DynamicIslandApp
import com.example.data.IslandSettingsRepository
import com.example.model.AnimationStyle
import com.example.model.IconStyle
import com.example.model.IslandDisplayMode
import com.example.model.IslandNotificationItem
import com.example.model.IslandSettings
import com.example.model.IslandThemePreset
import com.example.model.MonitoredAppOption
import com.example.model.PermissionStatus
import com.example.model.SystemTelemetrySnapshot
import com.example.service.DynamicIslandService
import com.example.system.FormatUtils
import com.example.system.PermissionHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val settings: IslandSettings = IslandSettings(),
    val telemetry: SystemTelemetrySnapshot = SystemTelemetrySnapshot(),
    val permissions: PermissionStatus = PermissionStatus(),
    val isOverlayServiceRunning: Boolean = false,
    val previewMode: IslandDisplayMode? = null,
    val isPreviewExpanded: Boolean = false,
    val monitoredApps: List<MonitoredAppOption> = emptyList(),
    val statusBannerMessage: String? = null
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DynamicIslandApp
    private val repository = app.settingsRepository
    private val telemetryManager = app.telemetryManager

    private val _permissions = MutableStateFlow(PermissionHelper.inspectPermissions(application))
    private val _previewMode = MutableStateFlow<IslandDisplayMode?>(null)
    private val _isPreviewExpanded = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)

    val permissions: StateFlow<PermissionStatus> = _permissions.asStateFlow()

    val uiState: StateFlow<MainUiState> = combine(
        repository.settingsFlow,
        telemetryManager.snapshot,
        _permissions,
        DynamicIslandService.isServiceRunning,
        combine(_previewMode, _isPreviewExpanded, _statusMessage) { mode, expanded, msg ->
            Triple(mode, expanded, msg)
        }
    ) { settings, telemetry, perms, running, previewTuple ->
        val (previewMode, isPreviewExpanded, statusMsg) = previewTuple
        MainUiState(
            settings = settings,
            telemetry = telemetry,
            permissions = perms,
            isOverlayServiceRunning = running,
            previewMode = previewMode,
            isPreviewExpanded = isPreviewExpanded,
            monitoredApps = telemetryManager.getMonitoredAppOptions(settings.monitoredPackages),
            statusBannerMessage = statusMsg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = MainUiState(permissions = _permissions.value)
    )

    init {
        telemetryManager.acquireMonitoring()
        refreshPermissions()
    }

    override fun onCleared() {
        telemetryManager.releaseMonitoring()
        super.onCleared()
    }

    fun refreshPermissions() {
        val status = PermissionHelper.inspectPermissions(app)
        _permissions.value = status
        telemetryManager.refreshPermissionsAndBindings()
        val currentSettings = telemetryManager.settings.value
        if (currentSettings.isIslandEnabled && status.hasOverlayPermission && !DynamicIslandService.isServiceRunning.value) {
            DynamicIslandService.start(app)
        }
    }

    fun toggleDynamicIsland(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled) {
                val perms = PermissionHelper.inspectPermissions(app)
                _permissions.value = perms
                if (!perms.hasOverlayPermission) {
                    repository.setIslandEnabled(false)
                    _statusMessage.value = "Grant 'Display over other apps' permission in Setup to activate the floating overlay."
                    return@launch
                }
                repository.setIslandEnabled(true)
                DynamicIslandService.start(app)
                _statusMessage.value = null
            } else {
                repository.setIslandEnabled(false)
                DynamicIslandService.stop(app)
                _statusMessage.value = null
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun setPreviewMode(mode: IslandDisplayMode?) {
        _previewMode.value = mode
    }

    fun togglePreviewExpanded() {
        _isPreviewExpanded.value = !_isPreviewExpanded.value
        if (telemetryManager.settings.value.hapticFeedbackEnabled) {
            FormatUtils.performSubtleHaptic(app)
        }
    }

    fun applyThemePreset(preset: IslandThemePreset) {
        viewModelScope.launch { repository.applyThemePreset(preset) }
    }

    fun updateDimensions(
        widthDp: Float? = null,
        heightDp: Float? = null,
        offsetXFraction: Float? = null,
        offsetYDp: Float? = null,
        cornerRadiusDp: Float? = null
    ) {
        viewModelScope.launch {
            repository.updateDimensions(widthDp, heightDp, offsetXFraction, offsetYDp, cornerRadiusDp)
        }
    }

    fun updateAppearance(
        transparency: Float? = null,
        glowIntensity: Float? = null,
        backgroundColorHex: Long? = null,
        borderColorHex: Long? = null,
        glowColorHex: Long? = null,
        textSizeSp: Float? = null,
        iconStyle: IconStyle? = null
    ) {
        viewModelScope.launch {
            repository.updateAppearance(
                transparency,
                glowIntensity,
                backgroundColorHex,
                borderColorHex,
                glowColorHex,
                textSizeSp,
                iconStyle
            )
        }
    }

    fun updateAnimation(
        speed: Float? = null,
        style: AnimationStyle? = null,
        hapticEnabled: Boolean? = null
    ) {
        viewModelScope.launch { repository.updateAnimation(speed, style, hapticEnabled) }
    }

    fun toggleFeature(feature: IslandSettingsRepository.FeatureToggle, enabled: Boolean) {
        viewModelScope.launch { repository.updateFeatureToggle(feature, enabled) }
    }

    fun toggleMonitoredPackage(packageName: String, enabled: Boolean) {
        viewModelScope.launch { repository.toggleMonitoredPackage(packageName, enabled) }
    }

    fun resetStudioDefaults() {
        viewModelScope.launch { repository.resetToDefaults() }
    }

    // Real system control actions
    fun toggleMediaPlayPause() = telemetryManager.toggleMediaPlayPause()
    fun skipToNextMedia() = telemetryManager.skipToNextMedia()
    fun skipToPreviousMedia() = telemetryManager.skipToPreviousMedia()
    fun openActiveMediaApp() = telemetryManager.openActiveMediaApp()
    fun openNotification(item: IslandNotificationItem) = telemetryManager.openNotification(item)
    fun dismissNotification(key: String) = telemetryManager.dismissNotification(key)
    fun setMediaVolume(index: Int) = telemetryManager.setMediaVolume(index)
    fun setSystemBrightness(percent: Int): Boolean = telemetryManager.setSystemBrightness(percent)
}
