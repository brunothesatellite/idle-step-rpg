package com.brunothesatellite.idlesteprpg

import android.app.Application
import android.content.Context
import com.brunothesatellite.idlesteprpg.data.GameRepository
import com.brunothesatellite.idlesteprpg.garmin.GarminClient
import com.brunothesatellite.idlesteprpg.garmin.GarminRepository
import com.brunothesatellite.idlesteprpg.garmin.SecretStore
import com.brunothesatellite.idlesteprpg.sync.SyncScheduler

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SyncScheduler.schedule(this)
    }
}

class AppContainer(context: Context) {
    val gameRepository = GameRepository(context)
    val secretStore = SecretStore(context)
    val garminClient = GarminClient()
    val garminRepository = GarminRepository(context, secretStore, garminClient, gameRepository)
}
