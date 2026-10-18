package com.example.data.repository

import android.net.Uri
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class MusicRepository(
    private val scanner: MediaStoreAudioScanner,
    private val database: GugakDatabase,
    private val scope: CoroutineScope
) {
    private val playlistDao = database.playlistDao()
    private val favoriteDao = database.favoriteDao()
    private val historyDao = database.historyDao()
    private val cloudTrackDao = database.cloudTrackDao()

    private val _localSongs = MutableStateFlow<List<Song>>(emptyList())

    private val _cloudSongs: Flow<List<Song>> = cloudTrackDao.getAllCloudTracks().map { list ->
        list.map {
            Song(
                id = it.id,
                title = it.title,
                artist = it.artist,
                album = it.album,
                albumId = it.id,
                durationMs = it.durationMs,
                contentUri = Uri.fromFile(File(it.localFilePath)).toString(),
                albumArtUri = it.artworkUrl,
                folderPath = it.source,
                folderName = "${it.source} Synced",
                dateAdded = it.syncedAt / 1000,
                size = it.size,
                isDemoTrack = false,
                isCloudSynced = true,
                cloudFileId = it.cloudFileId,
                source = it.source
            )
        }
    }

    val songs: StateFlow<List<Song>> = combine(_localSongs, _cloudSongs) { local, cloud ->
        cloud + local
    }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    val favoriteSongIds: StateFlow<Set<Long>> = favoriteDao.getAllFavoriteSongIds()
        .map { it.toSet() }
        .stateIn(scope, SharingStarted.Eagerly, emptySet())

    val recentSongIds: StateFlow<List<Long>> = historyDao.getRecentSongIds()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val rawPlaylists: StateFlow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val cloudTracksCount: Flow<Int> = cloudTrackDao.getAllCloudTracks().map { it.size }

    init {
        rescanLibrary()
    }

    fun rescanLibrary() {
        scope.launch(Dispatchers.IO) {
            _isScanning.value = true
            try {
                val scanned = scanner.scanAudioFiles()
                _localSongs.value = scanned
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isScanning.value = false
            }
        }
    }

    val albums: Flow<List<Album>> = songs.map { songList ->
        songList.groupBy { it.albumId to it.album }
            .map { (key, tracks) ->
                Album(
                    id = key.first,
                    title = key.second,
                    artist = tracks.firstOrNull()?.artist ?: "Unknown Artist",
                    songCount = tracks.size,
                    albumArtUri = tracks.firstOrNull()?.albumArtUri
                )
            }.sortedBy { it.title.lowercase() }
    }

    val artists: Flow<List<Artist>> = songs.map { songList ->
        songList.groupBy { it.artist }
            .map { (artistName, tracks) ->
                Artist(
                    name = artistName,
                    songCount = tracks.size,
                    albumCount = tracks.map { it.albumId }.distinct().size
                )
            }.sortedBy { it.name.lowercase() }
    }

    val folders: Flow<List<Folder>> = songs.map { songList ->
        songList.groupBy { it.folderPath }
            .map { (folderPath, tracks) ->
                Folder(
                    path = folderPath,
                    name = tracks.firstOrNull()?.folderName ?: "Music",
                    songCount = tracks.size
                )
            }.sortedBy { it.name.lowercase() }
    }

    fun getFavoriteSongs(): Flow<List<Song>> {
        return combine(songs, favoriteSongIds) { allSongs, favIds ->
            allSongs.filter { it.id in favIds }
        }
    }

    fun getRecentlyPlayedSongs(): Flow<List<Song>> {
        return combine(songs, recentSongIds) { allSongs, recIds ->
            val songMap = allSongs.associateBy { it.id }
            recIds.mapNotNull { songMap[it] }.distinctBy { it.id }
        }
    }

    suspend fun toggleFavorite(songId: Long) {
        if (favoriteSongIds.value.contains(songId)) {
            favoriteDao.removeFavorite(songId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(songId = songId))
        }
    }

    suspend fun recordPlayed(songId: Long) {
        historyDao.recordPlayed(HistoryEntity(songId = songId))
    }

    suspend fun createPlaylist(name: String): Long {
        return playlistDao.insertPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(id: Long, newName: String) {
        playlistDao.renamePlaylist(id, newName)
    }

    suspend fun deletePlaylist(id: Long) {
        playlistDao.deletePlaylist(id)
        playlistDao.clearPlaylistSongs(id)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        playlistDao.addSongToPlaylist(PlaylistSongCrossRef(playlistId = playlistId, songId = songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        playlistDao.removeSongFromPlaylist(playlistId, songId)
    }

    fun getSongsForPlaylist(playlistId: Long): Flow<List<Song>> {
        return combine(songs, playlistDao.getSongIdsForPlaylist(playlistId)) { allSongs, ids ->
            val songMap = allSongs.associateBy { it.id }
            ids.mapNotNull { songMap[it] }
        }
    }

    fun getSongById(id: Long): Song? {
        return songs.value.find { it.id == id }
    }
}
