package com.example.data.online

import com.example.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class InternetArchiveApiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun getPopularArchiveAudio(query: String? = null, limit: Int = 30): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<OnlineTrack>()
        try {
            val q = if (query.isNullOrBlank()) {
                "collection:(etree OR audio_music OR 78rpm OR classic_rock OR livemusic) AND mediatype:(audio)"
            } else {
                "(${query}) AND mediatype:(audio)"
            }

            val encodedQuery = URLEncoder.encode(q, "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=$encodedQuery&fl[]=identifier,title,creator,album,duration,year&sort[]=downloads+desc&rows=$limit&output=json"

            val request = Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string() ?: "{}"
                    val json = JSONObject(body)
                    val responseObj = json.optJSONObject("response")
                    val docs = responseObj?.optJSONArray("docs")

                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val doc = docs.getJSONObject(i)
                            val id = doc.optString("identifier", "")
                            if (id.isNotBlank()) {
                                val title = doc.optString("title", id)
                                val creator = doc.optString("creator", "Internet Archive Artist")
                                val album = doc.optString("album", "Archive Collection")
                                val year = doc.optString("year", "")

                                val artworkUrl = "https://archive.org/services/img/$id"
                                // Standard IA stream / download endpoint
                                val streamUrl = "https://archive.org/download/$id/${id}_vbr.mp3"

                                tracks.add(
                                    OnlineTrack(
                                        id = "ia_$id",
                                        title = title,
                                        artist = creator,
                                        album = if (album.isNotBlank()) album else "Internet Archive",
                                        durationMs = 240000L,
                                        streamUrl = streamUrl,
                                        artworkUrl = artworkUrl,
                                        source = "Internet Archive",
                                        year = year
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

        // Curated backup public-domain / Creative Commons tracks if offline/network restricted in emulator
        if (tracks.isEmpty()) {
            tracks.addAll(getCuratedArchiveBackups())
        }

        tracks
    }

    private fun getCuratedArchiveBackups(): List<OnlineTrack> {
        return listOf(
            OnlineTrack(
                id = "ia_beethoven_sym5",
                title = "Symphony No. 5 in C Minor",
                artist = "Ludwig van Beethoven",
                album = "Internet Archive Classical",
                durationMs = 432000L,
                streamUrl = "https://archive.org/download/BeethovenSymphonyNo.5_201609/01_AllegroConBrio.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1507838153414-b4b713384a76?w=500",
                source = "Internet Archive",
                year = "1808"
            ),
            OnlineTrack(
                id = "ia_chopin_nocturne",
                title = "Nocturne Op. 9 No. 2 in E-Flat Major",
                artist = "Frédéric Chopin",
                album = "Internet Archive Classical",
                durationMs = 270000L,
                streamUrl = "https://archive.org/download/ChopinNocturneOp9No2_405/nocturne.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1520523839898-5071282544e1?w=500",
                source = "Internet Archive",
                year = "1832"
            ),
            OnlineTrack(
                id = "ia_clair_de_lune",
                title = "Clair de Lune (Suite Bergamasque)",
                artist = "Claude Debussy",
                album = "Piano Masterworks",
                durationMs = 300000L,
                streamUrl = "https://archive.org/download/DebussyClairDeLune_836/clair.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1511192336575-5a79af67a629?w=500",
                source = "Internet Archive",
                year = "1905"
            ),
            OnlineTrack(
                id = "ia_vivaldi_four_seasons",
                title = "The Four Seasons: Spring (La Primavera)",
                artist = "Antonio Vivaldi",
                album = "Baroque Treasures",
                durationMs = 210000L,
                streamUrl = "https://archive.org/download/VivaldiSpring/spring.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500",
                source = "Internet Archive",
                year = "1725"
            )
        )
    }
}
