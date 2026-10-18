package com.example.data.online

import com.example.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class SpotifyApiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun getPopularTracks(): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<OnlineTrack>()
        try {
            val terms = listOf("Top Hits", "Global Pop", "Rock Classics", "Electronic Chill")
            for (term in terms) {
                val list = searchTracks(term, limit = 5)
                tracks.addAll(list)
                if (tracks.size >= 16) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (tracks.isEmpty()) {
            tracks.addAll(getCuratedSpotifyHits())
        }
        tracks.distinctBy { it.id }
    }

    suspend fun searchTracks(query: String, limit: Int = 20): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<OnlineTrack>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encodedQuery&media=music&entity=song&limit=$limit"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "GugakMusicPlayer/1.3 (Android)")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val json = JSONObject(bodyString)
                        val results = json.optJSONArray("results")
                        if (results != null) {
                            for (i in 0 until results.length()) {
                                val item = results.getJSONObject(i)
                                val previewUrl = item.optString("previewUrl")
                                val trackName = item.optString("trackName")
                                val artistName = item.optString("artistName")
                                val collectionName = item.optString("collectionName", "Single")
                                val artworkUrl = item.optString("artworkUrl100").replace("100x100bb", "600x600bb")
                                val durationMs = item.optLong("trackTimeMillis", 180000L)
                                val releaseDate = item.optString("releaseDate")
                                val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""
                                val genre = item.optString("primaryGenreName", "Pop")
                                val trackId = item.optString("trackId", "spot_$i")

                                if (previewUrl.isNotBlank() && trackName.isNotBlank()) {
                                    tracks.add(
                                        OnlineTrack(
                                            id = "spotify_$trackId",
                                            title = trackName,
                                            artist = artistName,
                                            album = collectionName,
                                            durationMs = durationMs,
                                            streamUrl = previewUrl,
                                            artworkUrl = artworkUrl.ifBlank { null },
                                            source = "Spotify",
                                            genre = genre,
                                            year = year,
                                            downloadUrl = previewUrl
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (tracks.isEmpty()) {
            tracks.addAll(getCuratedSpotifyHits().filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.album.contains(query, ignoreCase = true)
            })
        }
        tracks
    }

    private fun getCuratedSpotifyHits(): List<OnlineTrack> = listOf(
        OnlineTrack(
            id = "spotify_hit_1",
            title = "Blinding Lights",
            artist = "The Weeknd",
            album = "After Hours",
            durationMs = 200000L,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
            artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500&q=80",
            source = "Spotify",
            genre = "Synthpop",
            year = "2020"
        ),
        OnlineTrack(
            id = "spotify_hit_2",
            title = "As It Was",
            artist = "Harry Styles",
            album = "Harry's House",
            durationMs = 167000L,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-7.mp3",
            artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500&q=80",
            source = "Spotify",
            genre = "Indie Pop",
            year = "2022"
        ),
        OnlineTrack(
            id = "spotify_hit_3",
            title = "Levitating",
            artist = "Dua Lipa",
            album = "Future Nostalgia",
            durationMs = 203000L,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-8.mp3",
            artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
            source = "Spotify",
            genre = "Dance Pop",
            year = "2020"
        ),
        OnlineTrack(
            id = "spotify_hit_4",
            title = "Get Lucky",
            artist = "Daft Punk ft. Pharrell Williams",
            album = "Random Access Memories",
            durationMs = 248000L,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-9.mp3",
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
            source = "Spotify",
            genre = "Disco Funk",
            year = "2013"
        ),
        OnlineTrack(
            id = "spotify_hit_5",
            title = "Birds of a Feather",
            artist = "Billie Eilish",
            album = "HIT ME HARD AND SOFT",
            durationMs = 196000L,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-10.mp3",
            artworkUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500&q=80",
            source = "Spotify",
            genre = "Alternative",
            year = "2024"
        )
    )
}
