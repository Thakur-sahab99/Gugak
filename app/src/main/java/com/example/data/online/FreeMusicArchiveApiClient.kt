package com.example.data.online

import com.example.data.model.OnlineTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class FreeMusicArchiveApiClient {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    suspend fun getCuratedTracks(genre: String? = null, limit: Int = 30): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<OnlineTrack>()
        try {
            val q = if (genre.isNullOrBlank() || genre == "All") {
                "collection:(freemusicarchive OR netlabels) AND mediatype:(audio)"
            } else {
                "collection:(freemusicarchive OR netlabels) AND ($genre) AND mediatype:(audio)"
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
                    val docs = json.optJSONObject("response")?.optJSONArray("docs")
                    if (docs != null) {
                        for (i in 0 until docs.length()) {
                            val doc = docs.getJSONObject(i)
                            val id = doc.optString("identifier", "")
                            if (id.isNotBlank()) {
                                tracks.add(
                                    OnlineTrack(
                                        id = "fma_$id",
                                        title = doc.optString("title", id),
                                        artist = doc.optString("creator", "Free Music Archive Artist"),
                                        album = doc.optString("album", "Free Music Archive Collection"),
                                        durationMs = 210000L,
                                        streamUrl = "https://archive.org/download/$id/${id}_vbr.mp3",
                                        artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500",
                                        source = "Free Music Archive",
                                        genre = genre ?: "Creative Commons",
                                        year = doc.optString("year", "")
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
            tracks.addAll(getCuratedFMABackups())
        }

        tracks.take(limit)
    }

    suspend fun searchTracks(query: String, limit: Int = 25): List<OnlineTrack> = withContext(Dispatchers.IO) {
        val curated = getCuratedFMABackups()
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
            val encodedQuery = URLEncoder.encode("collection:(freemusicarchive OR netlabels) AND ($query)", "UTF-8")
            val url = "https://archive.org/advancedsearch.php?q=$encodedQuery&fl[]=identifier,title,creator,album,duration,year&rows=$limit&output=json"
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
                                        id = "fma_$id",
                                        title = doc.optString("title", id),
                                        artist = doc.optString("creator", "FMA Community Artist"),
                                        album = "Free Music Archive",
                                        durationMs = 210000L,
                                        streamUrl = "https://archive.org/download/$id/${id}_vbr.mp3",
                                        artworkUrl = "https://images.unsplash.com/photo-1493225457124-a3eb161ffa5f?w=500",
                                        source = "Free Music Archive",
                                        genre = "Independent CC"
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

    private fun getCuratedFMABackups(): List<OnlineTrack> {
        return listOf(
            OnlineTrack(
                id = "fma_scott_holmes",
                title = "Storybook & Inspiring Dreams",
                artist = "Scott Holmes Music",
                album = "Corporate & Inspiring Music",
                durationMs = 178000L,
                streamUrl = "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Scott_Holmes/Inspiring__Success/Scott_Holmes_-_04_-_Storybook.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=500",
                source = "Free Music Archive",
                genre = "Acoustic & Folk",
                year = "2020"
            ),
            OnlineTrack(
                id = "fma_kai_engel",
                title = "Contention (Neo-Classical)",
                artist = "Kai Engel",
                album = "Chapter One / Cold",
                durationMs = 212000L,
                streamUrl = "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/ccCommunity/Kai_Engel/Chapter_One_-_Cold/Kai_Engel_-_02_-_Contention.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1445985543469-221e5744637b?w=500",
                source = "Free Music Archive",
                genre = "Neo-Classical",
                year = "2019"
            ),
            OnlineTrack(
                id = "fma_broke_for_free",
                title = "Night Owl (Indie Electronic)",
                artist = "Broke For Free",
                album = "Directionless EP",
                durationMs = 200000L,
                streamUrl = "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/WFMU/Broke_For_Free/Directionless_EP/Broke_For_Free_-_01_-_Night_Owl.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1501386761578-eac5c94b800a?w=500",
                source = "Free Music Archive",
                genre = "Indie Electronic",
                year = "2021"
            ),
            OnlineTrack(
                id = "fma_doctor_turtle",
                title = "His Last Share Of The Stars",
                artist = "Doctor Turtle",
                album = "Flush Your Rolex",
                durationMs = 154000L,
                streamUrl = "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/no_curator/Doctor_Turtle/Flush_Your_Rolex/Doctor_Turtle_-_05_-_His_Last_Share_Of_The_Stars.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1470225620780-dba8ba36b745?w=500",
                source = "Free Music Archive",
                genre = "Folk / Ambient",
                year = "2020"
            ),
            OnlineTrack(
                id = "fma_meydan",
                title = "Synthwave Nostalgia Run",
                artist = "Meydän",
                album = "Interplanetary Highway",
                durationMs = 225000L,
                streamUrl = "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/ccCommunity/Meydan/Interplanetary_Highway/Meydan_-_03_-_Synthwave_Run.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500",
                source = "Free Music Archive",
                genre = "Synthwave",
                year = "2022"
            ),
            OnlineTrack(
                id = "fma_jahzzar",
                title = "Solitude in Autumn",
                artist = "Jahzzar",
                album = "Tumbling Dishes Like Old-Man's Wishes",
                durationMs = 186000L,
                streamUrl = "https://files.freemusicarchive.org/storage-freemusicarchive-org/music/WFMU/Jahzzar/Tumbling_Dishes_Like_Old-Mans_Wishes/Jahzzar_-_05_-_Solitude.mp3",
                artworkUrl = "https://images.unsplash.com/photo-1465847899084-d164df4dedc6?w=500",
                source = "Free Music Archive",
                genre = "Acoustic",
                year = "2021"
            )
        )
    }
}
