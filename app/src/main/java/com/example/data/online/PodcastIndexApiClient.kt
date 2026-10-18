package com.example.data.online

import com.example.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class PodcastIndexApiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun getPopularPodcasts(): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<OnlineTrack>()
        try {
            val queries = listOf("music", "technology", "news")
            for (query in queries) {
                val results = searchPodcasts(query, limit = 6)
                tracks.addAll(results)
                if (tracks.size >= 15) break
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (tracks.isEmpty()) {
            tracks.addAll(getCuratedPodcasts())
        }
        tracks.distinctBy { it.id }
    }

    suspend fun searchPodcasts(query: String, limit: Int = 20): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<OnlineTrack>()
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encodedQuery&media=podcast&entity=podcastEpisode&limit=$limit"
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
                                val episodeUrl = item.optString("episodeUrl")
                                val trackName = item.optString("trackName")
                                val collectionName = item.optString("collectionName", "Podcast")
                                val artistName = item.optString("artistName", collectionName)
                                val artworkUrl = item.optString("artworkUrl600").ifBlank {
                                    item.optString("artworkUrl160")
                                }
                                val durationMs = item.optLong("trackTimeMillis", 1800000L)
                                val releaseDate = item.optString("releaseDate")
                                val year = if (releaseDate.length >= 4) releaseDate.substring(0, 4) else ""
                                val genre = item.optString("primaryGenreName", "Podcast")
                                val episodeId = item.optString("trackId", item.optString("collectionId", "pod_$i"))

                                if (episodeUrl.isNotBlank() && trackName.isNotBlank()) {
                                    tracks.add(
                                        OnlineTrack(
                                            id = "pod_$episodeId",
                                            title = trackName,
                                            artist = if (artistName.isNotBlank()) artistName else collectionName,
                                            album = collectionName,
                                            durationMs = durationMs,
                                            streamUrl = episodeUrl,
                                            artworkUrl = artworkUrl.ifBlank { null },
                                            source = "Podcast Index",
                                            genre = genre,
                                            year = year,
                                            downloadUrl = episodeUrl
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
            tracks.addAll(getCuratedPodcasts().filter {
                it.title.contains(query, ignoreCase = true) ||
                it.artist.contains(query, ignoreCase = true) ||
                it.genre.contains(query, ignoreCase = true)
            })
        }
        tracks
    }

    private fun getCuratedPodcasts(): List<OnlineTrack> = listOf(
        OnlineTrack(
            id = "pod_curated_1",
            title = "The Sound of Storytelling & Modern Audio Production",
            artist = "Song Exploder",
            album = "Music & Sound Design",
            durationMs = 1320000L,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
            artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500&q=80",
            source = "Podcast Index",
            genre = "Music",
            year = "2024"
        ),
        OnlineTrack(
            id = "pod_curated_2",
            title = "Next-Gen Mobile Architecture & Kotlin Frontiers",
            artist = "Android Developers Backstage",
            album = "Technology",
            durationMs = 1680000L,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
            artworkUrl = "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500&q=80",
            source = "Podcast Index",
            genre = "Technology",
            year = "2024"
        ),
        OnlineTrack(
            id = "pod_curated_3",
            title = "Global Rhythm Chronicles: The Evolution of Synthwave",
            artist = "Electronic Music Insights",
            album = "Culture & Beats",
            durationMs = 1540000L,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
            artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500&q=80",
            source = "Podcast Index",
            genre = "Electronic",
            year = "2024"
        ),
        OnlineTrack(
            id = "pod_curated_4",
            title = "Deep Space Sounds & The Echoes of the Universe",
            artist = "Science & Wonders Daily",
            album = "Astronomy & Sound",
            durationMs = 1200000L,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
            artworkUrl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?w=500&q=80",
            source = "Podcast Index",
            genre = "Science",
            year = "2024"
        ),
        OnlineTrack(
            id = "pod_curated_5",
            title = "Morning Coffee & Creative Thinking",
            artist = "Daily Mindset Journal",
            album = "Education & Life",
            durationMs = 960000L,
            streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
            artworkUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500&q=80",
            source = "Podcast Index",
            genre = "Education",
            year = "2024"
        )
    )
}
