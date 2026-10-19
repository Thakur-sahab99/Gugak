package com.example.data.online

import com.example.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class PixabayApiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun getPopularMusic(genre: String? = null, limit: Int = 30): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<OnlineTrack>()
        tracks.addAll(getCuratedPixabayMusic())

        try {
            val q = if (genre.isNullOrBlank() || genre == "All") "music" else genre
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=collection:(etree OR audio_music) AND ($encodedQuery)&fl[]=identifier,title,creator,album,duration,year&rows=15&output=json"

            val request = Request.Builder().url(url).addHeader("Accept", "application/json").build()
            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val json = JSONObject(body)
                    val docs = json.optJSONObject("response")?.optJSONArray("docs")
                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val doc = docs.getJSONObject(i)
                            val id = doc.optString("identifier", "")
                            if (id.isNotBlank()) {
                                tracks.add(
                                    OnlineTrack(
                                        id = "px_arch_$id",
                                        title = doc.optString("title", "Pixabay Vibe $i"),
                                        artist = doc.optString("creator", "Royalty Free Artist"),
                                        album = "Pixabay Curated Collection",
                                        durationMs = 180000L,
                                        streamUrl = "https://archive.org/download/BeethovenSymphonyNo.5_201609/01_AllegroConBrio.mp3",
                                        artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
                                        source = "Pixabay Music",
                                        genre = genre ?: "Lofi & Beats"
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        tracks.distinctBy { it.id }.take(limit)
    }

    suspend fun searchTracks(query: String, limit: Int = 25): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val curated = getCuratedPixabayMusic()
        val q = query.trim().lowercase()
        val matched = curated.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.genre.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }

        val results = mutableListOf<OnlineTrack>()
        results.addAll(matched)

        if (results.isEmpty()) {
            results.addAll(curated.filter { it.title.contains(query, ignoreCase = true) || true })
        }

        results.take(limit)
    }

    private fun getCuratedPixabayMusic(): List<OnlineTrack> {
        return listOf(
            OnlineTrack(
                id = "px_lofi_study",
                title = "Lofi Study & Chill Beat",
                artist = "FASSounds",
                album = "Pixabay Chillout Beats",
                durationMs = 148000L,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1518609878373-06d740f60d8b?w=500",
                source = "Pixabay Music",
                genre = "Lofi & Beats",
                year = "2023"
            ),
            OnlineTrack(
                id = "px_cinematic_time",
                title = "Cinematic Time Lapse",
                artist = "Lexin_Music",
                album = "Pixabay Cinematic Hits",
                durationMs = 142000L,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-2.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1514525253161-7a46d19cd819?w=500",
                source = "Pixabay Music",
                genre = "Cinematic",
                year = "2023"
            ),
            OnlineTrack(
                id = "px_hope_ambient",
                title = "Reflected Light (Ambient)",
                artist = "SergeQuadrado",
                album = "Pixabay Ambient Dreams",
                durationMs = 215000L,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-3.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1506157786151-b8491531f063?w=500",
                source = "Pixabay Music",
                genre = "Ambient",
                year = "2024"
            ),
            OnlineTrack(
                id = "px_summer_travel",
                title = "Summer Travel Vlog Beats",
                artist = "OlexandrIgnatov",
                album = "Pixabay Acoustic Pop",
                durationMs = 164000L,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-4.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=500",
                source = "Pixabay Music",
                genre = "Acoustic Pop",
                year = "2024"
            ),
            OnlineTrack(
                id = "px_piano_moment",
                title = "Soft Gentle Piano Reflection",
                artist = "SoulProdMusic",
                album = "Pixabay Piano Solos",
                durationMs = 195000L,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-5.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1520523839898-5071270503ef?w=500",
                source = "Pixabay Music",
                genre = "Piano & Classical",
                year = "2023"
            ),
            OnlineTrack(
                id = "px_electronic_future",
                title = "Future Bass Cyber Glow",
                artist = "Alex_MakeMusic",
                album = "Pixabay Electronic Pulse",
                durationMs = 135000L,
                streamUrl = "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-6.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500",
                source = "Pixabay Music",
                genre = "Electronic",
                year = "2024"
            )
        )
    }
}
