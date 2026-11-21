package com.example.system

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.IslandSettingsRepository
import com.example.model.BatteryState
import com.example.model.CallState
import com.example.model.IslandDisplayMode
import com.example.model.IslandNotificationItem
import com.example.model.IslandSettings
import com.example.model.MediaState
import com.example.model.MonitoredAppOption
import com.example.model.NetworkState
import com.example.model.SystemTelemetrySnapshot
import com.example.model.VolumeBrightnessState
import com.example.service.IslandNotificationListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class SystemTelemetryManager(
    private val context: Context,
    private val settingsRepository: IslandSettingsRepository
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _snapshot = MutableStateFlow(SystemTelemetrySnapshot())
    val snapshot: StateFlow<SystemTelemetrySnapshot> = _snapshot.asStateFlow()

    private val _settings = MutableStateFlow(IslandSettings())
    val settings: StateFlow<IslandSettings> = _settings.asStateFlow()

    private var activeClientCount = 0
    private var isMonitoring = false
    private var transientResetJob: Job? = null
    private var samplerJob: Job? = null

    private var listenerServiceRef: WeakReference<IslandNotificationListenerService>? = null

    // Battery tracking
    private var lastChargingState: Boolean? = null
    private var lastFullAlertTriggered = false
    private var lastLowAlertTriggered = false

    // Network tracking
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var wasPreviouslyConnected: Boolean? = null
    private var lastTotalRxBytes = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
    private var lastTotalTxBytes = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
    private var lastAppRxBytes = TrafficStats.getUidRxBytes(Process.myUid()).coerceAtLeast(0L)
    private var lastAppTxBytes = TrafficStats.getUidTxBytes(Process.myUid()).coerceAtLeast(0L)
    private var lastSampleTimeMs = SystemClock.elapsedRealtime()

    // Media tracking
    private var mediaSessionManager: MediaSessionManager? = null
    private var activeMediaController: MediaController? = null
    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            attachPrimaryMediaController(controllers)
        }
    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            refreshMediaSnapshot()
        }

        override fun onMetadataChanged(metadata: MediaMetadata?) {
            refreshMediaSnapshot()
        }

        override fun onSessionDestroyed() {
            activeMediaController = null
            refreshMediaSnapshot()
        }
    }

    // Volume & Brightness tracking
    private val audioManager by lazy {
        appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }
    private val settingsObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            val previousBrightness = _snapshot.value.volumeBrightness.brightnessRaw
            refreshVolumeBrightness()
            val newBrightness = _snapshot.value.volumeBrightness.brightnessRaw
            if (previousBrightness != newBrightness && _settings.value.volumeBrightnessEnabled) {
                triggerTransient(
                    mode = IslandDisplayMode.VOLUME_BRIGHTNESS,
                    title = "Brightness $newBrightness",
                    subtitle = "${_snapshot.value.volumeBrightness.brightnessPercent}% display level"
                )
            }
        }
    }

    private val systemBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_BATTERY_CHANGED -> handleBatteryIntent(intent)
                Intent.ACTION_POWER_CONNECTED -> {
                    if (_settings.value.batteryMonitorEnabled) {
                        triggerTransient(
                            mode = IslandDisplayMode.BATTERY,
                            title = "Charging Started",
                            subtitle = "${_snapshot.value.battery.level}% • ${_snapshot.value.battery.pluggedLabel}"
                        )
                    }
                }
                Intent.ACTION_POWER_DISCONNECTED -> {
                    lastFullAlertTriggered = false
                }
                Intent.ACTION_BATTERY_LOW -> {
                    if (_settings.value.batteryMonitorEnabled) {
                        triggerTransient(
                            mode = IslandDisplayMode.BATTERY,
                            title = "Low Battery Warning",
                            subtitle = "${_snapshot.value.battery.level}% remaining"
                        )
                        postSystemAlertNotification(
                            id = ALERT_BATTERY_LOW_ID,
                            title = "Low Battery (${_snapshot.value.battery.level}%)",
                            body = "Dynamic Island Pro detected low battery level."
                        )
                    }
                }
                "android.media.VOLUME_CHANGED_ACTION" -> {
                    val prevMediaVol = _snapshot.value.volumeBrightness.mediaVolume
                    refreshVolumeBrightness()
                    val currMediaVol = _snapshot.value.volumeBrightness.mediaVolume
                    if (prevMediaVol != currMediaVol && _settings.value.volumeBrightnessEnabled) {
                        val max = _snapshot.value.volumeBrightness.maxMediaVolume.coerceAtLeast(1)
                        val pct = ((currMediaVol * 100f) / max).toInt()
                        triggerTransient(
                            mode = IslandDisplayMode.VOLUME_BRIGHTNESS,
                            title = "Media Volume",
                            subtitle = "$pct% ($currMediaVol/$max)"
                        )
                    }
                }
            }
        }
    }

    init {
        ensureAlertChannel()
        scope.launch {
            settingsRepository.settingsFlow.collect { latest ->
                _settings.value = latest
                recomputeActiveMode()
            }
        }
    }

    fun acquireMonitoring() {
        activeClientCount++
        if (!isMonitoring) {
            startMonitoring()
        }
    }

    fun releaseMonitoring() {
        activeClientCount = (activeClientCount - 1).coerceAtLeast(0)
        if (activeClientCount == 0 && isMonitoring) {
            stopMonitoring()
        }
    }

    private fun startMonitoring() {
        isMonitoring = true
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }
        ContextCompat.registerReceiver(
            appContext,
            systemBroadcastReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )

        // Initial sticky battery read
        appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let {
            handleBatteryIntent(it)
        }

        // Content observer for brightness
        runCatching {
            appContext.contentResolver.registerContentObserver(
                Settings.System.CONTENT_URI,
                true,
                settingsObserver
            )
        }
        refreshVolumeBrightness()

        // Network callback
        registerNetworkMonitor()

        // Media sessions
        refreshMediaSessionBinding()

        // Periodic lightweight 1-second sampler for TrafficStats & live playback progress
        samplerJob?.cancel()
        samplerJob = scope.launch {
            while (true) {
                sampleTrafficStats()
                tickMediaPositionAndCallMode()
                delay(1000L)
            }
        }
    }

    private fun stopMonitoring() {
        isMonitoring = false
        samplerJob?.cancel()
        samplerJob = null
        runCatching { appContext.unregisterReceiver(systemBroadcastReceiver) }
        runCatching { appContext.contentResolver.unregisterContentObserver(settingsObserver) }
        unregisterNetworkMonitor()
        detachMediaSessionListeners()
    }

    fun refreshPermissionsAndBindings() {
        refreshVolumeBrightness()
        refreshMediaSessionBinding()
    }

    fun refreshMediaSessionBinding() {
        val hasAccess = PermissionHelper.isNotificationListenerEnabled(appContext)
        if (!hasAccess) {
            detachMediaSessionListeners()
            _snapshot.update {
                it.copy(media = it.media.copy(hasMediaAccess = false, isActive = false))
            }
            recomputeActiveMode()
            return
        }

        try {
            val manager = mediaSessionManager
                ?: (appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager)
                    .also { mediaSessionManager = it }
            val listenerComponent = ComponentName(appContext, IslandNotificationListenerService::class.java)
            manager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            manager.addOnActiveSessionsChangedListener(sessionsChangedListener, listenerComponent)
            val controllers = manager.getActiveSessions(listenerComponent)
            attachPrimaryMediaController(controllers)
        } catch (_: SecurityException) {
            _snapshot.update {
                it.copy(media = it.media.copy(hasMediaAccess = false, isActive = false))
            }
        }
    }

    private fun detachMediaSessionListeners() {
        runCatching {
            activeMediaController?.unregisterCallback(mediaCallback)
        }
        activeMediaController = null
        runCatching {
            mediaSessionManager?.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        }
    }

    private fun attachPrimaryMediaController(controllers: List<MediaController>?) {
        val list = controllers.orEmpty()
        val chosen = list.firstOrNull { controller ->
            controller.playbackState?.state == PlaybackState.STATE_PLAYING
        } ?: list.firstOrNull()

        if (activeMediaController?.sessionToken != chosen?.sessionToken) {
            runCatching { activeMediaController?.unregisterCallback(mediaCallback) }
            activeMediaController = chosen
            chosen?.registerCallback(mediaCallback)
        }
        refreshMediaSnapshot()
    }

    private fun refreshMediaSnapshot() {
        val hasAccess = PermissionHelper.isNotificationListenerEnabled(appContext)
        val controller = activeMediaController
        if (!hasAccess || controller == null) {
            _snapshot.update {
                it.copy(
                    media = MediaState(hasMediaAccess = hasAccess, isActive = false)
                )
            }
            recomputeActiveMode()
            return
        }

        val meta = controller.metadata
        val state = controller.playbackState
        val isPlaying = state?.state == PlaybackState.STATE_PLAYING
        val isPaused = state?.state == PlaybackState.STATE_PAUSED ||
            state?.state == PlaybackState.STATE_BUFFERING
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: ""
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: ""
        val albumArt = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION)?.coerceAtLeast(0L) ?: 0L
        val position = state?.position?.coerceAtLeast(0L) ?: 0L
        val actions = state?.actions ?: 0L
        val pkg = controller.packageName ?: ""
        val appName = resolveAppLabel(pkg)

        val isActive = title.isNotBlank() || isPlaying || isPaused
        _snapshot.update {
            it.copy(
                media = MediaState(
                    isActive = isActive,
                    isPlaying = isPlaying,
                    title = title.ifBlank { appName.ifBlank { "Active Media" } },
                    artist = artist.ifBlank { appName },
                    packageName = pkg,
                    appName = appName,
                    albumArt = albumArt,
                    positionMs = position,
                    durationMs = duration,
                    canPlayPause = (actions and (PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE)) != 0L || isActive,
                    canSkipNext = (actions and PlaybackState.ACTION_SKIP_TO_NEXT) != 0L,
                    canSkipPrevious = (actions and PlaybackState.ACTION_SKIP_TO_PREVIOUS) != 0L,
                    hasMediaAccess = true
                )
            )
        }
        recomputeActiveMode()
    }

    private fun tickMediaPositionAndCallMode() {
        val currentMedia = _snapshot.value.media
        if (currentMedia.isActive && currentMedia.isPlaying && currentMedia.durationMs > 0L) {
            val state = activeMediaController?.playbackState
            if (state != null) {
                val timeDelta = SystemClock.elapsedRealtime() - state.lastPositionUpdateTime
                val computed = (state.position + (timeDelta * state.playbackSpeed).toLong())
                    .coerceIn(0L, currentMedia.durationMs)
                _snapshot.update {
                    it.copy(media = it.media.copy(positionMs = computed))
                }
            }
        }
    }

    fun toggleMediaPlayPause() {
        val controller = activeMediaController ?: return
        val state = controller.playbackState?.state
        if (state == PlaybackState.STATE_PLAYING) {
            controller.transportControls.pause()
        } else {
            controller.transportControls.play()
        }
    }

    fun skipToNextMedia() {
        activeMediaController?.transportControls?.skipToNext()
    }

    fun skipToPreviousMedia() {
        activeMediaController?.transportControls?.skipToPrevious()
    }

    fun seekMediaTo(positionMs: Long) {
        activeMediaController?.transportControls?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun openActiveMediaApp() {
        val pkg = _snapshot.value.media.packageName
        if (pkg.isNotBlank()) {
            launchPackage(pkg)
        }
    }

    private fun handleBatteryIntent(intent: Intent) {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
        val pct = if (level >= 0) ((level * 100f) / scale).toInt().coerceIn(0, 100) else 0
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        val isFull = status == BatteryManager.BATTERY_STATUS_FULL || (isCharging && pct >= 100)
        val isLow = !isCharging && pct in 1..15

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
        val pluggedLabel = when (plugged) {
            BatteryManager.BATTERY_PLUGGED_AC -> "AC Fast/Wall"
            BatteryManager.BATTERY_PLUGGED_USB -> "USB Cable"
            BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
            BatteryManager.BATTERY_PLUGGED_DOCK -> "Dock"
            else -> if (isCharging) "Charging" else "On Battery"
        }

        val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)
        val tempC = tempTenths / 10f
        val voltageMv = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0)
        val healthCode = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN)
        val healthLabel = when (healthCode) {
            BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
            BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
            BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
            BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
            BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
            else -> "Normal"
        }

        val updated = BatteryState(
            level = pct,
            isCharging = isCharging,
            isFull = isFull,
            isLow = isLow,
            temperatureCelsius = tempC,
            voltageMv = voltageMv,
            pluggedLabel = pluggedLabel,
            healthLabel = healthLabel,
            hasValidReading = level >= 0
        )
        _snapshot.update { it.copy(battery = updated) }

        if (_settings.value.batteryMonitorEnabled) {
            if (lastChargingState == false && isCharging) {
                triggerTransient(
                    mode = IslandDisplayMode.BATTERY,
                    title = "Charging Connected",
                    subtitle = "$pct% • $pluggedLabel"
                )
            }
            if (isFull && !lastFullAlertTriggered) {
                lastFullAlertTriggered = true
                triggerTransient(
                    mode = IslandDisplayMode.BATTERY,
                    title = "Battery Fully Charged",
                    subtitle = "100% • Safe to unplug"
                )
                postSystemAlertNotification(
                    id = ALERT_BATTERY_FULL_ID,
                    title = "Battery Full (100%)",
                    body = "Your device is fully charged."
                )
            } else if (!isFull) {
                lastFullAlertTriggered = false
            }

            if (isLow && !lastLowAlertTriggered) {
                lastLowAlertTriggered = true
                triggerTransient(
                    mode = IslandDisplayMode.BATTERY,
                    title = "Low Battery ($pct%)",
                    subtitle = "Connect charger soon"
                )
            } else if (!isLow) {
                lastLowAlertTriggered = false
            }
        }
        lastChargingState = isCharging
        recomputeActiveMode()
    }

    private fun registerNetworkMonitor() {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return
        connectivityManager = cm
        updateNetworkCapabilities(cm)

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                updateNetworkCapabilities(cm)
            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                updateNetworkCapabilities(cm)
            }

            override fun onLost(network: Network) {
                updateNetworkCapabilities(cm)
            }
        }
        networkCallback = callback
        runCatching {
            cm.registerDefaultNetworkCallback(callback)
        }
    }

    private fun unregisterNetworkMonitor() {
        val cm = connectivityManager
        val cb = networkCallback
        if (cm != null && cb != null) {
            runCatching { cm.unregisterNetworkCallback(cb) }
        }
        networkCallback = null
    }

    private fun updateNetworkCapabilities(cm: ConnectivityManager) {
        val activeNetwork = cm.activeNetwork
        val caps = if (activeNetwork != null) cm.getNetworkCapabilities(activeNetwork) else null
        val isConnected = caps != null &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps != null &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
        val transportLabel = when {
            !isConnected -> "Offline"
            isWifi -> "Wi-Fi"
            isCellular -> "Mobile Data"
            isEthernet -> "Ethernet"
            else -> "Connected"
        }

        val prev = wasPreviouslyConnected
        var eventMsg = _snapshot.value.network.lastEventMessage
        if (prev != null && prev != isConnected && _settings.value.networkMonitorEnabled) {
            if (isConnected) {
                eventMsg = "Connection Restored ($transportLabel)"
                triggerTransient(
                    mode = IslandDisplayMode.NETWORK,
                    title = "Connection Restored",
                    subtitle = "$transportLabel active"
                )
            } else {
                eventMsg = "Connection Lost"
                triggerTransient(
                    mode = IslandDisplayMode.NETWORK,
                    title = "Connection Lost",
                    subtitle = "No active internet link"
                )
            }
        }
        wasPreviouslyConnected = isConnected

        _snapshot.update {
            it.copy(
                network = it.network.copy(
                    isConnected = isConnected,
                    isValidated = isValidated,
                    transportLabel = transportLabel,
                    isWifi = isWifi,
                    isCellular = isCellular,
                    lastEventMessage = eventMsg
                )
            )
        }
        recomputeActiveMode()
    }

    private fun sampleTrafficStats() {
        val now = SystemClock.elapsedRealtime()
        val elapsedMs = (now - lastSampleTimeMs).coerceAtLeast(200L)

        val totalRx = TrafficStats.getTotalRxBytes()
        val totalTx = TrafficStats.getTotalTxBytes()
        val uid = Process.myUid()
        val appRx = TrafficStats.getUidRxBytes(uid)
        val appTx = TrafficStats.getUidTxBytes(uid)

        val deviceDownBps = if (totalRx >= 0 && lastTotalRxBytes >= 0 && totalRx >= lastTotalRxBytes) {
            ((totalRx - lastTotalRxBytes) * 1000L) / elapsedMs
        } else 0L
        val deviceUpBps = if (totalTx >= 0 && lastTotalTxBytes >= 0 && totalTx >= lastTotalTxBytes) {
            ((totalTx - lastTotalTxBytes) * 1000L) / elapsedMs
        } else 0L
        val appDownBps = if (appRx >= 0 && lastAppRxBytes >= 0 && appRx >= lastAppRxBytes) {
            ((appRx - lastAppRxBytes) * 1000L) / elapsedMs
        } else 0L
        val appUpBps = if (appTx >= 0 && lastAppTxBytes >= 0 && appTx >= lastAppTxBytes) {
            ((appTx - lastAppTxBytes) * 1000L) / elapsedMs
        } else 0L

        if (totalRx >= 0) lastTotalRxBytes = totalRx
        if (totalTx >= 0) lastTotalTxBytes = totalTx
        if (appRx >= 0) lastAppRxBytes = appRx
        if (appTx >= 0) lastAppTxBytes = appTx
        lastSampleTimeMs = now

        _snapshot.update {
            it.copy(
                network = it.network.copy(
                    deviceDownBytesPerSec = deviceDownBps,
                    deviceUpBytesPerSec = deviceUpBps,
                    appDownBytesPerSec = appDownBps,
                    appUpBytesPerSec = appUpBps
                )
            )
        }
    }

    fun refreshVolumeBrightness() {
        val mediaVol = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(0)
        val maxMedia = runCatching { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }.getOrDefault(15)
        val ringVol = runCatching { audioManager.getStreamVolume(AudioManager.STREAM_RING) }.getOrDefault(0)
        val maxRing = runCatching { audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) }.getOrDefault(7)

        val rawBrightness = runCatching {
            Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS)
        }.getOrDefault(128).coerceIn(0, 255)
        val brightnessMode = runCatching {
            Settings.System.getInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE)
        }.getOrDefault(Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        val brightnessPct = ((rawBrightness * 100f) / 255f).toInt().coerceIn(0, 100)

        _snapshot.update {
            it.copy(
                volumeBrightness = VolumeBrightnessState(
                    mediaVolume = mediaVol,
                    maxMediaVolume = maxMedia,
                    ringVolume = ringVol,
                    maxRingVolume = maxRing,
                    brightnessRaw = rawBrightness,
                    brightnessPercent = brightnessPct,
                    isAutoBrightness = brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC,
                    canWriteSettings = PermissionHelper.canWriteSystemSettings(appContext)
                )
            )
        }
    }

    fun setMediaVolume(index: Int) {
        val max = _snapshot.value.volumeBrightness.maxMediaVolume.coerceAtLeast(1)
        val target = index.coerceIn(0, max)
        runCatching {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
        }
        refreshVolumeBrightness()
        if (_settings.value.volumeBrightnessEnabled) {
            val pct = ((target * 100f) / max).toInt()
            triggerTransient(
                mode = IslandDisplayMode.VOLUME_BRIGHTNESS,
                title = "Media Volume",
                subtitle = "$pct% ($target/$max)"
            )
        }
    }

    fun setSystemBrightness(percent: Int): Boolean {
        if (!PermissionHelper.canWriteSystemSettings(appContext)) return false
        val clampedPct = percent.coerceIn(1, 100)
        val raw = ((clampedPct / 100f) * 255f).toInt().coerceIn(1, 255)
        return runCatching {
            Settings.System.putInt(
                appContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS_MODE,
                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
            )
            Settings.System.putInt(
                appContext.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS,
                raw
            )
            refreshVolumeBrightness()
            if (_settings.value.volumeBrightnessEnabled) {
                triggerTransient(
                    mode = IslandDisplayMode.VOLUME_BRIGHTNESS,
                    title = "Display Brightness",
                    subtitle = "$clampedPct% brightness"
                )
            }
            true
        }.getOrDefault(false)
    }

    // NotificationListenerService bridge
    fun registerNotificationListener(service: IslandNotificationListenerService) {
        listenerServiceRef = WeakReference(service)
        _snapshot.update { it.copy(listenerConnected = true) }
        refreshMediaSessionBinding()
    }

    fun unregisterNotificationListener() {
        listenerServiceRef = null
        _snapshot.update { it.copy(listenerConnected = false) }
    }

    fun onNotificationListUpdated(
        items: List<IslandNotificationItem>,
        activeCall: CallState?,
        newlyPostedItem: IslandNotificationItem?
    ) {
        _snapshot.update { current ->
            current.copy(
                notifications = items.take(15),
                call = activeCall ?: CallState(isActive = false)
            )
        }
        if (newlyPostedItem != null && _settings.value.notificationsEnabled) {
            val subtitle = if (newlyPostedItem.isContentHidden) {
                "${newlyPostedItem.appName} • Content hidden"
            } else {
                newlyPostedItem.text.ifBlank { newlyPostedItem.appName }
            }
            triggerTransient(
                mode = IslandDisplayMode.NOTIFICATION,
                title = newlyPostedItem.title.ifBlank { newlyPostedItem.appName },
                subtitle = subtitle
            )
        }
        recomputeActiveMode()
    }

    fun dismissNotification(key: String) {
        val service = listenerServiceRef?.get()
        if (service != null) {
            runCatching { service.cancelNotification(key) }
        }
        _snapshot.update { current ->
            current.copy(notifications = current.notifications.filterNot { it.key == key })
        }
        recomputeActiveMode()
    }

    fun openNotification(item: IslandNotificationItem) {
        val intent = item.contentIntent
        if (intent != null) {
            val sent = runCatching {
                intent.send()
                true
            }.getOrDefault(false)
            if (sent) return
        }
        launchPackage(item.packageName)
    }

    fun launchPackage(packageName: String) {
        runCatching {
            val launch = appContext.packageManager.getLaunchIntentForPackage(packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (launch != null) {
                appContext.startActivity(launch)
            }
        }
    }

    fun getMonitoredAppOptions(enabledPackages: Set<String>): List<MonitoredAppOption> {
        val catalog = listOf(
            Triple("com.whatsapp", "WhatsApp", "Messaging"),
            Triple("com.instagram.android", "Instagram", "Social"),
            Triple("org.telegram.messenger", "Telegram", "Messaging"),
            Triple("com.google.android.apps.messaging", "Google Messages", "SMS"),
            Triple("com.android.mms", "System SMS / Messaging", "SMS"),
            Triple("com.google.android.gm", "Gmail", "Email"),
            Triple("com.discord", "Discord", "Messaging"),
            Triple("com.spotify.music", "Spotify", "Music"),
            Triple("com.google.android.apps.youtube.music", "YouTube Music", "Music"),
            Triple("com.google.android.youtube", "YouTube", "Video")
        )
        return catalog.map { (pkg, name, category) ->
            val installed = isPackageInstalled(pkg)
            MonitoredAppOption(
                packageName = pkg,
                displayName = name,
                category = category,
                isInstalled = installed,
                isEnabled = enabledPackages.contains(pkg)
            )
        }
    }

    private fun isPackageInstalled(pkg: String): Boolean {
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getPackageInfo(
                    pkg,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getPackageInfo(pkg, 0)
            }
            true
        }.getOrDefault(false)
    }

    fun resolveAppLabel(pkg: String): String {
        if (pkg.isBlank()) return ""
        return runCatching {
            val appInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                appContext.packageManager.getApplicationInfo(
                    pkg,
                    PackageManager.ApplicationInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                appContext.packageManager.getApplicationInfo(pkg, 0)
            }
            appContext.packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(pkg.substringAfterLast('.').replaceFirstChar { it.uppercase() })
    }

    private fun triggerTransient(mode: IslandDisplayMode, title: String, subtitle: String) {
        transientResetJob?.cancel()
        _snapshot.update {
            it.copy(
                transientMode = mode,
                transientTitle = title,
                transientSubtitle = subtitle
            )
        }
        recomputeActiveMode()
        transientResetJob = scope.launch {
            delay(4200L)
            _snapshot.update {
                it.copy(transientMode = null, transientTitle = "", transientSubtitle = "")
            }
            recomputeActiveMode()
        }
    }

    private fun recomputeActiveMode() {
        val s = _settings.value
        val snap = _snapshot.value
        val resolved = when {
            s.callMonitorEnabled && snap.call.isActive -> IslandDisplayMode.CALL
            snap.transientMode != null -> snap.transientMode
            s.musicPlayerEnabled && snap.media.isActive -> IslandDisplayMode.MUSIC
            s.notificationsEnabled && snap.notifications.isNotEmpty() -> IslandDisplayMode.NOTIFICATION
            s.batteryMonitorEnabled && (snap.battery.isCharging || snap.battery.isLow) -> IslandDisplayMode.BATTERY
            s.networkMonitorEnabled && !snap.network.isConnected -> IslandDisplayMode.NETWORK
            else -> IslandDisplayMode.IDLE
        }
        _snapshot.update { it.copy(activeMode = resolved) }
    }

    private fun ensureAlertChannel() {
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val channel = NotificationChannel(
            ALERT_CHANNEL_ID,
            appContext.getString(R.string.channel_alerts_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = appContext.getString(R.string.channel_alerts_desc)
        }
        nm.createNotificationChannel(channel)
    }

    private fun postSystemAlertNotification(id: Int, title: String, body: String) {
        if (!PermissionHelper.hasPostNotificationsPermission(appContext)) return
        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val openIntent = Intent(appContext, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pi = PendingIntent.getActivity(
            appContext,
            id,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(appContext, ALERT_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(id, notif)
    }

    companion object {
        private const val ALERT_CHANNEL_ID = "dynamic_island_alerts"
        private const val ALERT_BATTERY_FULL_ID = 4101
        private const val ALERT_BATTERY_LOW_ID = 4102
    }
}
