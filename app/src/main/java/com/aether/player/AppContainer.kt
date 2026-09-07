package com.aether.player

import android.content.Context
import com.aether.player.data.db.AetherDatabase
import com.aether.player.data.library.LibraryRepository
import com.aether.player.data.library.MediaStoreScanner
import com.aether.player.data.prefs.UserPreferences
import com.aether.player.playback.PlayerManager

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AetherDatabase = AetherDatabase.create(appContext)
    val preferences = UserPreferences(appContext)
    val library = LibraryRepository(database, MediaStoreScanner(appContext))
    val playerManager: PlayerManager = PlayerManager.get(appContext, library, preferences)
}

class AetherApp : android.app.Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
