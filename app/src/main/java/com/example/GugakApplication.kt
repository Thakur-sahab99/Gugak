package com.example

import android.app.Application
import com.example.data.local.GugakDatabase
import com.example.data.local.UserPreferences
import com.example.data.repository.MediaStoreAudioScanner
import com.example.data.repository.MusicRepository
import com.example.playback.PlaybackController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GugakApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var database: GugakDatabase
        private set

    lateinit var preferences: UserPreferences
        private set

    lateinit var scanner: MediaStoreAudioScanner
        private set

    lateinit var repository: MusicRepository
        private set

    lateinit var playbackController: PlaybackController
        private set

    lateinit var onlineMusicManager: com.example.data.online.OnlineMusicManager
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = GugakDatabase.getInstance(this)
        preferences = UserPreferences(this)
        scanner = MediaStoreAudioScanner(this)
        repository = MusicRepository(scanner, database, applicationScope)
        playbackController = PlaybackController(this, preferences, repository, applicationScope)
        onlineMusicManager = com.example.data.online.OnlineMusicManager(this, database.cloudTrackDao(), applicationScope)
    }

    override fun onTerminate() {
        super.onTerminate()
        playbackController.release()
    }

    companion object {
        lateinit var instance: GugakApplication
            private set
    }
}
