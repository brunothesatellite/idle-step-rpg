package com.brunothesatellite.idlesteprpg

import android.app.Application
import android.content.Context
import com.brunothesatellite.idlesteprpg.audio.AmbientMusic
import com.brunothesatellite.idlesteprpg.audio.SoundFx
import com.brunothesatellite.idlesteprpg.data.GameRepository
import com.brunothesatellite.idlesteprpg.data.SettingsStore
import com.brunothesatellite.idlesteprpg.garmin.GarminClient
import com.brunothesatellite.idlesteprpg.garmin.GarminRepository
import com.brunothesatellite.idlesteprpg.garmin.SecretStore
import com.brunothesatellite.idlesteprpg.notify.GameNotifications
import com.brunothesatellite.idlesteprpg.sync.MaintenanceScheduler
import com.brunothesatellite.idlesteprpg.sync.SyncScheduler

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        GameNotifications.ensureChannels(this)
        SoundFx.init(this)
        SyncScheduler.schedule(this, container.settings.stepSource == "garmin")
        MaintenanceScheduler.schedule(this)
    }
}

class AppContainer(val context: Context) {
    val settings = SettingsStore(context)
    val gameRepository = GameRepository(context, settings)
    val secretStore = SecretStore(context)
    val garminClient = GarminClient()
    val garminRepository = GarminRepository(context, secretStore, garminClient, gameRepository)
    val healthConnectSource = com.brunothesatellite.idlesteprpg.steps.HealthConnectSource(context)
    val stepRepository = com.brunothesatellite.idlesteprpg.steps.StepRepository(
        context,
        settings,
        garminRepository,
        healthConnectSource,
        gameRepository
    )
    val ambientMusic = AmbientMusic(context, settings)
}
