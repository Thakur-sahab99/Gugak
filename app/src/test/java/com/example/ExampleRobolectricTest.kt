package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.GugakDatabase
import com.example.data.local.PlaylistEntity
import com.example.data.local.ThemeMode
import com.example.data.local.UserPreferences
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: GugakDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, GugakDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Gugak", appName)
    }

    @Test
    fun `playlist insertion and query`() = runBlocking {
        val playlistDao = db.playlistDao()
        val id = playlistDao.insertPlaylist(PlaylistEntity(name = "Chill Beats"))
        val playlists = playlistDao.getAllPlaylists().first()
        assertEquals(1, playlists.size)
        assertEquals("Chill Beats", playlists[0].name)
    }

    @Test
    fun `preferences theme mode switching`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = UserPreferences(context)
        prefs.setThemeMode(ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, prefs.themeMode.value)
    }

    @Test
    fun `cloud track entity insertion and retrieval`() = runBlocking {
        val cloudDao = db.cloudTrackDao()
        val track = com.example.data.local.CloudTrackEntity(
            id = 9001L,
            title = "Cloud Resonance",
            artist = "Google Cloud",
            album = "Cloud Storage Vault",
            durationMs = 60000L,
            localFilePath = "/data/local/tmp/song.mp3",
            cloudFileId = "gcloud_12345"
        )
        cloudDao.insertCloudTrack(track)
        val cloudTracks = cloudDao.getAllCloudTracks().first()
        assertEquals(1, cloudTracks.size)
        assertEquals("Cloud Resonance", cloudTracks[0].title)
    }
}
