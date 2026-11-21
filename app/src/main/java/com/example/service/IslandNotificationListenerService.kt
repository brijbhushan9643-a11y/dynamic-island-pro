package com.example.service

import android.app.KeyguardManager
import android.app.Notification
import android.content.Context
import android.os.SystemClock
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.DynamicIslandApp
import com.example.model.CallState
import com.example.model.IslandNotificationItem

class IslandNotificationListenerService : NotificationListenerService() {

    private var activeCallStartMs: Long = 0L
    private var activeCallKey: String? = null

    private fun telemetry() = (application as? DynamicIslandApp)?.telemetryManager

    override fun onListenerConnected() {
        super.onListenerConnected()
        telemetry()?.registerNotificationListener(this)
        syncAllActiveNotifications(newlyPostedKey = null)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        telemetry()?.unregisterNotificationListener()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        if (sbn.packageName == packageName) return
        syncAllActiveNotifications(newlyPostedKey = sbn.key)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.key == activeCallKey) {
            activeCallKey = null
            activeCallStartMs = 0L
        }
        syncAllActiveNotifications(newlyPostedKey = null)
    }

    private fun syncAllActiveNotifications(newlyPostedKey: String?) {
        val manager = telemetry() ?: return
        val settings = manager.settings.value
        val activeSbns = runCatching { activeNotifications }.getOrNull() ?: emptyArray()

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isDeviceLocked = keyguardManager?.isKeyguardLocked == true

        var detectedCall: CallState? = null
        val parsedItems = mutableListOf<IslandNotificationItem>()
        var newlyPostedItem: IslandNotificationItem? = null

        for (sbn in activeSbns.sortedByDescending { it.postTime }) {
            if (sbn.packageName == packageName) continue
            val notif = sbn.notification ?: continue

            // Ignore group summaries to prevent duplicate clutter
            if ((notif.flags and Notification.FLAG_GROUP_SUMMARY) != 0) continue

            val extras = notif.extras
            val rawTitle = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
            val rawText = (extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras?.getCharSequence(Notification.EXTRA_TEXT))
                ?.toString()?.trim().orEmpty()
            val appName = manager.resolveAppLabel(sbn.packageName)

            // Detect legitimate incoming/ongoing Call notifications without interfering with Dialer
            if (notif.category == Notification.CATEGORY_CALL && settings.callMonitorEnabled) {
                val isOngoingCall = (notif.flags and Notification.FLAG_ONGOING_EVENT) != 0
                if (activeCallKey != sbn.key) {
                    activeCallKey = sbn.key
                    activeCallStartMs = SystemClock.elapsedRealtime()
                }
                val callerLabel = rawTitle.ifBlank { appName.ifBlank { "Call" } }
                val statusLabel = rawText.ifBlank {
                    if (isOngoingCall) "Active call" else "Incoming call"
                }
                detectedCall = CallState(
                    isActive = true,
                    isIncoming = !isOngoingCall,
                    callerName = callerLabel,
                    appName = appName,
                    packageName = sbn.packageName,
                    startedAtElapsedMs = activeCallStartMs,
                    statusText = statusLabel
                )
                continue
            }

            // Filter notifications according to user's per-app whitelist
            val monitored = settings.monitoredPackages
            if (monitored.isNotEmpty() && !monitored.contains(sbn.packageName)) {
                continue
            }

            val isSecret = notif.visibility == Notification.VISIBILITY_SECRET
            val isPrivateOnLock = notif.visibility == Notification.VISIBILITY_PRIVATE && isDeviceLocked
            val hideContent = !settings.showNotificationContent ||
                isSecret ||
                (settings.hideSensitiveOnLock && (isPrivateOnLock || isDeviceLocked))

            val appIcon = runCatching { packageManager.getApplicationIcon(sbn.packageName) }.getOrNull()

            val item = IslandNotificationItem(
                key = sbn.key,
                packageName = sbn.packageName,
                appName = appName,
                title = if (hideContent) appName else rawTitle.ifBlank { appName },
                text = if (hideContent) "New notification (content hidden)" else rawText,
                timestampMs = sbn.postTime,
                appIcon = appIcon,
                contentIntent = notif.contentIntent,
                isContentHidden = hideContent,
                isDismissible = sbn.isClearable
            )
            parsedItems.add(item)
            if (sbn.key == newlyPostedKey) {
                newlyPostedItem = item
            }
        }

        if (detectedCall == null) {
            activeCallKey = null
            activeCallStartMs = 0L
        }

        manager.onNotificationListUpdated(
            items = parsedItems,
            activeCall = detectedCall,
            newlyPostedItem = newlyPostedItem
        )
    }
}
