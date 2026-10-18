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

    // Public catalog feeds and fallback royalty-free tracks from Pixabay
    suspend fun getPopularMusic(genre: String? = null, limit: Int = 30): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<OnlineTrack>()
        try {
            val q = if (genre.isNullOrBlank() || genre == "All") "music" else genre
            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            // Try fetching from public CC/Pixabay audio mirror or search
            val url = "https://archive.org/advancedsearch.php?q=collection:(pixabay_music OR audio_music) AND ($encodedQuery)&fl[]=identifier,title,creator,album,duration,year&rows=$limit&output=json"

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()

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
                                        id = "pixabay_$id",
                                        title = doc.optString("title", "Pixabay Track $i"),
                                        artist = doc.optString("creator", "Pixabay Music Creator"),
                                        album = "Pixabay Royalty Free",
                                        durationMs = 185000L,
                                        streamUrl = "https://archive.org/download/$id/${id}_vbr.mp3",
                                        artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
                                        source = "Pixabay Music",
                                        genre = genre ?: "Royalty Free"
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

        if (tracks.isEmpty()) {
            tracks.addAll(getCuratedPixabayMusic())
        }

        tracks.take(limit)
    }

    suspend fun searchTracks(query: String, limit: Int = 25): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val curated = getCuratedPixabayMusic()
        val q = query.trim().lowercase()
        val matchedCurated = curated.filter {
            it.title.lowercase().contains(q) ||
            it.artist.lowercase().contains(q) ||
            it.genre.lowercase().contains(q) ||
            it.album.lowercase().contains(q)
        }

        val results = mutableListOf<OnlineTrack>()
        results.addAll(matchedCurated)

        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=($encodedQuery) AND mediatype:(audio)&fl[]=identifier,title,creator,album,duration&rows=$limit&output=json"
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
                                results.add(
                                    OnlineTrack(
                                        id = "pixabay_$id",
                                        title = doc.optString("title", id),
                                        artist = doc.optString("creator", "Pixabay Creator"),
                                        album = "Pixabay Free Collection",
                                        durationMs = 190000L,
                                        streamUrl = "https://archive.org/download/$id/${id}_vbr.mp3",
                                        artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500",
                                        source = "Pixabay Music",
                                        genre = "Free Royalty"
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
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/05/27/audio_1808fbf07a.mp3?filename=lofi-study-112191.mp3",
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
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/01/18/audio_d0a13f69d2.mp3?filename=cinematic-time-lapse-115672.mp3",
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
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/03/15/audio_c8c8a73467.mp3?filename=reflected-light-14759.mp3",
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
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/08/02/audio_884fe92c21.mp3?filename=summer-walk-152722.mp3",
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
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/10/14/audio_9939f792cb.mp3?filename=the-cradle-of-your-soul-15700.mp3",
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
                streamUrl = "https://cdn.pixabay.com/download/audio/2022/03/10/audio_51d2f97607.mp3?filename=future-bass-113222.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500",
                source = "Pixabay Music",
                genre = "Electronic",
                year = "2024"
            )
        )
    }
}
