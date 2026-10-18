package com.example.data.model

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUri: String,
    val albumArtUri: String? = null,
    val folderPath: String = "",
    val folderName: String = "",
    val dateAdded: Long = 0,
    val size: Long = 0,
    val isDemoTrack: Boolean = false,
    val isCloudSynced: Boolean = false,
    val cloudFileId: String? = null,
    val source: String = "Local",
    val streamUrl: String? = null,
    val isOnlineOnly: Boolean = false
) {
    val durationFormatted: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val streamUrl: String,
    val artworkUrl: String? = null,
    val source: String, // "Audius" or "Internet Archive"
    val genre: String = "",
    val year: String = "",
    val downloadUrl: String = streamUrl
) {
    val durationFormatted: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }

    fun toSong(isSynced: Boolean = false, localUri: String? = null): Song {
        val numericId = (id.hashCode().toLong() and 0x7FFFFFFF) + 5000000L
        return Song(
            id = numericId,
            title = title,
            artist = artist,
            album = album,
            albumId = numericId,
            durationMs = durationMs,
            contentUri = localUri ?: streamUrl,
            albumArtUri = artworkUrl,
            folderPath = source,
            folderName = source,
            dateAdded = System.currentTimeMillis() / 1000,
            size = 0L,
            isDemoTrack = false,
            isCloudSynced = isSynced,
            cloudFileId = id,
            source = source,
            streamUrl = streamUrl,
            isOnlineOnly = !isSynced
        )
    }
}

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val songCount: Int,
    val albumArtUri: String? = null
)

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int
)

data class Folder(
    val path: String,
    val name: String,
    val songCount: Int
)

data class Playlist(
    val id: Long,
    val name: String,
    val createdAt: Long,
    val songCount: Int = 0
)

enum class RepeatMode {
    OFF, ALL, ONE
}

enum class SortOption {
    TITLE, ARTIST, ALBUM, DURATION, DATE_ADDED
}
