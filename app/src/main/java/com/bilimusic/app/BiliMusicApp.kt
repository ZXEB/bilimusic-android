package com.bilimusic.app

import android.app.Application
import com.bilimusic.app.api.BiliApiClient
import com.bilimusic.app.api.BiliQuality
import com.bilimusic.app.api.BilibiliRepository
import com.bilimusic.app.data.PlayHistoryStorage
import com.bilimusic.app.data.PreferencesStorage
import com.bilimusic.app.player.PlayerManager
import com.bilimusic.app.util.CrashHandler
import com.bilimusic.app.util.DebugLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BiliMusicApp : Application() {

    lateinit var preferences: PreferencesStorage
        private set
    lateinit var playHistoryStorage: PlayHistoryStorage
        private set
    lateinit var repository: BilibiliRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this
        DebugLog.init(this)
        Thread.setDefaultUncaughtExceptionHandler(CrashHandler(this))
        DebugLog.i("App started")
        preferences = PreferencesStorage(this)
        playHistoryStorage = PlayHistoryStorage(this)

        val savedSessdata = runBlocking { preferences.getSessdata() }
        if (savedSessdata.isNotEmpty()) {
            DebugLog.i("Restoring session from saved SESSDATA")
        }
        repository = BilibiliRepository(BiliApiClient(savedSessdata))
        PlayerManager.initialize(this)

        appScope.launch {
            val history = playHistoryStorage.loadHistory()
            PlayerManager.loadPlayHistory(history)
        }

        PlayerManager.onResolveAudioUrl = { song ->
            val qualityKey = runBlocking { preferences.getAudioQuality() }
            val quality = BiliQuality.fromKey(qualityKey)
            repository.getAudioUrl(song.bvid, quality).getOrNull()
        }

        PlayerManager.onSongChanged = { _ ->
            appScope.launch {
                val current = PlayerManager.playHistory.value
                playHistoryStorage.saveHistory(current)
            }
        }
    }

    companion object {
        lateinit var instance: BiliMusicApp
            private set
    }
}
