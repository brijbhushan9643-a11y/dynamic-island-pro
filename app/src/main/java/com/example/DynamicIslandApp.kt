package com.example

import android.app.Application
import com.example.data.IslandSettingsRepository
import com.example.system.SystemTelemetryManager

class DynamicIslandApp : Application() {

    lateinit var settingsRepository: IslandSettingsRepository
        private set

    lateinit var telemetryManager: SystemTelemetryManager
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = IslandSettingsRepository(this)
        telemetryManager = SystemTelemetryManager(this, settingsRepository)
    }
}
