package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.DynamicIslandApp
import com.example.MainActivity
import com.example.R
import com.example.model.IslandSettings
import com.example.system.FormatUtils
import com.example.system.PermissionHelper
import com.example.ui.components.DynamicIslandPill
import com.example.ui.components.IslandCallbacks
import com.example.ui.theme.DynamicIslandProTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class DynamicIslandService :
    Service(),
    LifecycleOwner,
    ViewModelStoreOwner,
    SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var isExpandedState by mutableStateOf(false)
    private var latestSettings = IslandSettings()

    override fun onCreate() {
        super.onCreate()
        savedStateRegistryController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        createOverlayChannel()
        startAsForeground()

        if (!PermissionHelper.canDrawOverlays(this)) {
            stopSelf()
            return
        }

        val app = application as DynamicIslandApp
        app.telemetryManager.acquireMonitoring()

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        attachOverlayView(app)

        serviceScope.launch {
            app.settingsRepository.settingsFlow.collect { settings ->
                latestSettings = settings
                updateWindowPosition(settings)
                if (!settings.isIslandEnabled) {
                    stopSelf()
                }
            }
        }

        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        _isServiceRunning.value = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateWindowPosition(latestSettings)
    }

    override fun onDestroy() {
        _isServiceRunning.value = false
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)

        overlayView?.let { view ->
            runCatching { windowManager?.removeViewImmediate(view) }
        }
        overlayView = null
        (application as? DynamicIslandApp)?.telemetryManager?.releaseMonitoring()
        store.clear()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startAsForeground() {
        val notification = buildForegroundNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FOREGROUND_NOTIFICATION_ID, notification)
        }
    }

    private fun attachOverlayView(app: DynamicIslandApp) {
        val params = createLayoutParams(latestSettings)
        layoutParams = params

        val view = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@DynamicIslandService)
            setViewTreeViewModelStoreOwner(this@DynamicIslandService)
            setViewTreeSavedStateRegistryOwner(this@DynamicIslandService)
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
            setContent {
                val settings by app.settingsRepository.settingsFlow.collectAsStateWithLifecycle(
                    initialValue = IslandSettings()
                )
                val snapshot by app.telemetryManager.snapshot.collectAsStateWithLifecycle()

                DynamicIslandProTheme {
                    Box(
                        modifier = Modifier.wrapContentSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        DynamicIslandPill(
                            settings = settings,
                            telemetry = snapshot,
                            isExpanded = isExpandedState,
                            callbacks = IslandCallbacks(
                                onToggleExpand = {
                                    isExpandedState = !isExpandedState
                                    if (settings.hapticFeedbackEnabled) {
                                        FormatUtils.performSubtleHaptic(this@DynamicIslandService)
                                    }
                                },
                                onPlayPause = { app.telemetryManager.toggleMediaPlayPause() },
                                onSkipNext = { app.telemetryManager.skipToNextMedia() },
                                onSkipPrevious = { app.telemetryManager.skipToPreviousMedia() },
                                onOpenMediaApp = {
                                    isExpandedState = false
                                    app.telemetryManager.openActiveMediaApp()
                                },
                                onOpenNotification = { item ->
                                    isExpandedState = false
                                    app.telemetryManager.openNotification(item)
                                },
                                onDismissNotification = { key ->
                                    app.telemetryManager.dismissNotification(key)
                                },
                                onSetMediaVolume = { idx ->
                                    app.telemetryManager.setMediaVolume(idx)
                                },
                                onSetBrightness = { pct ->
                                    app.telemetryManager.setSystemBrightness(pct)
                                }
                            )
                        )
                    }
                }
            }
        }
        overlayView = view
        runCatching {
            windowManager?.addView(view, params)
        }.onFailure {
            stopSelf()
        }
    }

    private fun createLayoutParams(settings: IslandSettings): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val screenWidthPx = resources.displayMetrics.widthPixels
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = (settings.offsetXFraction * screenWidthPx).roundToInt()
            y = (settings.offsetYDp * density).roundToInt()
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun updateWindowPosition(settings: IslandSettings) {
        val params = layoutParams ?: return
        val view = overlayView ?: return
        val density = resources.displayMetrics.density
        val screenWidthPx = resources.displayMetrics.widthPixels
        params.x = (settings.offsetXFraction * screenWidthPx).roundToInt()
        params.y = (settings.offsetYDp * density).roundToInt()
        if (view.isAttachedToWindow) {
            runCatching { windowManager?.updateViewLayout(view, params) }
        }
    }

    private fun createOverlayChannel() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val channel = NotificationChannel(
            OVERLAY_CHANNEL_ID,
            getString(R.string.channel_overlay_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.channel_overlay_desc)
            setShowBadge(false)
        }
        nm.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val openPending = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopIntent = Intent(this, DynamicIslandService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPending = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, OVERLAY_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(getString(R.string.overlay_notif_title))
            .setContentText(getString(R.string.overlay_notif_body))
            .setContentIntent(openPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(0, "Stop", stopPending)
            .build()
    }

    companion object {
        private const val OVERLAY_CHANNEL_ID = "dynamic_island_overlay_channel"
        private const val FOREGROUND_NOTIFICATION_ID = 4001
        private const val ACTION_STOP = "com.example.action.STOP_ISLAND"

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun start(context: Context) {
            if (!PermissionHelper.canDrawOverlays(context)) return
            val intent = Intent(context, DynamicIslandService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, DynamicIslandService::class.java)
            context.stopService(intent)
        }
    }
}
