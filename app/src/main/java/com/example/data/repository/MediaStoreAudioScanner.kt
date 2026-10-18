package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.data.model.Album
import com.example.data.model.Artist
import com.example.data.model.Folder
import com.example.data.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class MediaStoreAudioScanner(private val context: Context) {

    suspend fun scanAudioFiles(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()
        val contentResolver = context.contentResolver

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 1000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(titleColumn) ?: "Unknown Title"
                    val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                    val album = cursor.getString(albumColumn) ?: "Unknown Album"
                    val albumId = cursor.getLong(albumIdColumn)
                    val duration = cursor.getLong(durationColumn)
                    val path = cursor.getString(dataColumn) ?: ""
                    val dateAdded = cursor.getLong(dateAddedColumn)
                    val size = cursor.getLong(sizeColumn)

                    val contentUri = ContentUris.withAppendedId(collection, id).toString()
                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    ).toString()

                    val file = File(path)
                    val folderPath = file.parent ?: "Internal Storage"
                    val folderName = file.parentFile?.name ?: "Music"

                    songsList.add(
                        Song(
                            id = id,
                            title = title,
                            artist = if (artist.contains("<unknown>", ignoreCase = true)) "Unknown Artist" else artist,
                            album = if (album.contains("<unknown>", ignoreCase = true)) "Unknown Album" else album,
                            albumId = albumId,
                            durationMs = duration,
                            contentUri = contentUri,
                            albumArtUri = albumArtUri,
                            folderPath = folderPath,
                            folderName = folderName,
                            dateAdded = dateAdded,
                            size = size,
                            isDemoTrack = false
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // If no songs found on device/emulator, generate high quality playable audio tracks
        if (songsList.isEmpty()) {
            songsList.addAll(generatePlayableDemoTracks())
        }

        songsList
    }

    private fun generatePlayableDemoTracks(): List<Song> {
        val demoDir = File(context.filesDir, "demo_tracks").apply { mkdirs() }
        val demos = listOf(
            DemoTrackSpec(-101L, "Pixel Horizon", "Gugak Ensemble", "Material Horizons", 45000, 440.0, 554.37),
            DemoTrackSpec(-102L, "Midnight Reverie", "Luma & Sound", "Ambient Nocturne", 60000, 329.63, 392.00),
            DemoTrackSpec(-103L, "Cosmic Pulse", "Apex Synthetics", "Neural Audio Labs", 50000, 261.63, 349.23),
            DemoTrackSpec(-104L, "Acoustic Serenade", "Gugak Studio", "Material Horizons", 55000, 523.25, 659.25),
            DemoTrackSpec(-105L, "Nordic Waves", "Fjord Echoes", "Subtle Currents", 48000, 293.66, 369.99)
        )

        return demos.map { spec ->
            val trackFile = File(demoDir, "demo_${spec.id.toString().replace("-", "n")}.wav")
            if (!trackFile.exists() || trackFile.length() < 1000) {
                createHarmonicWavFile(trackFile, (spec.durationMs / 1000).toInt(), spec.freq1, spec.freq2)
            }
            Song(
                id = spec.id,
                title = spec.title,
                artist = spec.artist,
                album = spec.album,
                albumId = spec.id - 100,
                durationMs = spec.durationMs,
                contentUri = Uri.fromFile(trackFile).toString(),
                albumArtUri = null,
                folderPath = demoDir.absolutePath,
                folderName = "Gugak Curated Demos",
                dateAdded = System.currentTimeMillis() / 1000,
                size = trackFile.length(),
                isDemoTrack = true
            )
        }
    }

    private fun createHarmonicWavFile(outFile: File, durationSeconds: Int, f1: Double, f2: Double) {
        val sampleRate = 22050
        val numSamples = durationSeconds * sampleRate
        val dataSize = numSamples * 2 // 16-bit mono

        try {
            FileOutputStream(outFile).use { fos ->
                // Write standard WAV header (44 bytes)
                fos.write("RIFF".toByteArray())
                fos.write(intToByteArray(36 + dataSize))
                fos.write("WAVEfmt ".toByteArray())
                fos.write(intToByteArray(16)) // Subchunk1Size (16 for PCM)
                fos.write(shortToByteArray(1.toShort())) // AudioFormat 1 = PCM
                fos.write(shortToByteArray(1.toShort())) // NumChannels = 1
                fos.write(intToByteArray(sampleRate))
                fos.write(intToByteArray(sampleRate * 2)) // ByteRate
                fos.write(shortToByteArray(2.toShort())) // BlockAlign
                fos.write(shortToByteArray(16.toShort())) // BitsPerSample
                fos.write("data".toByteArray())
                fos.write(intToByteArray(dataSize))

                // Generate pleasant harmonic chord tones with smooth envelope
                val buffer = ByteArray(1024)
                var bufIdx = 0

                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    // Chord blend with warm sub-bass and overtone
                    val wave = 0.5 * Math.sin(2.0 * Math.PI * f1 * t) +
                            0.3 * Math.sin(2.0 * Math.PI * f2 * t) +
                            0.2 * Math.sin(2.0 * Math.PI * (f1 / 2.0) * t)

                    // Envelope: soft tremolo and gradual decay
                    val tremolo = 0.85 + 0.15 * Math.sin(2.0 * Math.PI * 1.5 * t)
                    val sampleVal = (wave * tremolo * 16000.0).toInt().coerceIn(-32767, 32767).toShort()

                    buffer[bufIdx++] = (sampleVal.toInt() and 0xFF).toByte()
                    buffer[bufIdx++] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()

                    if (bufIdx >= buffer.size) {
                        fos.write(buffer, 0, bufIdx)
                        bufIdx = 0
                    }
                }
                if (bufIdx > 0) {
                    fos.write(buffer, 0, bufIdx)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun intToByteArray(value: Int): ByteArray {
        return byteArrayOf(
            (value and 0xFF).toByte(),
            ((value shr 8) and 0xFF).toByte(),
            ((value shr 16) and 0xFF).toByte(),
            ((value shr 24) and 0xFF).toByte()
        )
    }

    private fun shortToByteArray(value: Short): ByteArray {
        return byteArrayOf(
            (value.toInt() and 0xFF).toByte(),
            ((value.toInt() shr 8) and 0xFF).toByte()
        )
    }

    private data class DemoTrackSpec(
        val id: Long,
        val title: String,
        val artist: String,
        val album: String,
        val durationMs: Long,
        val freq1: Double,
        val freq2: Double
    )
}
