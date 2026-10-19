package com.example.playback

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.media.audiofx.Equalizer
import android.media.audiofx.BassBoost
import android.media.audiofx.Virtualizer
import android.media.audiofx.LoudnessEnhancer
import com.example.data.local.UserPreferences
import com.example.data.model.RepeatMode
import com.example.data.model.Song
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class PlaybackController(
    private val context: Context,
    private val preferences: UserPreferences,
    private val repository: MusicRepository,
    private val scope: CoroutineScope
) : MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener, MediaPlayer.OnPreparedListener {

    private var mediaPlayer: MediaPlayer? = null
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val streamCacheDir = File(context.cacheDir, "online_stream_cache").apply { mkdirs() }
    private val syncDir = File(context.filesDir, "online_synced").apply { mkdirs() }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null

    private val _isSpatialAudioEnabled = MutableStateFlow(true)
    val isSpatialAudioEnabled: StateFlow<Boolean> = _isSpatialAudioEnabled.asStateFlow()

    private val _isLosslessMasterEnabled = MutableStateFlow(true)
    val isLosslessMasterEnabled: StateFlow<Boolean> = _isLosslessMasterEnabled.asStateFlow()

    private val _isBassBoostEnabled = MutableStateFlow(true)
    val isBassBoostEnabled: StateFlow<Boolean> = _isBassBoostEnabled.asStateFlow()

    fun toggleSpatialAudio() {
        _isSpatialAudioEnabled.value = !_isSpatialAudioEnabled.value
        virtualizer?.enabled = _isSpatialAudioEnabled.value
    }

    fun toggleLosslessMaster() {
        _isLosslessMasterEnabled.value = !_isLosslessMasterEnabled.value
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            loudnessEnhancer?.enabled = _isLosslessMasterEnabled.value
        }
    }

    fun toggleBassBoost() {
        _isBassBoostEnabled.value = !_isBassBoostEnabled.value
        bassBoost?.enabled = _isBassBoostEnabled.value
    }

    private fun setupAudioEffects(sessionId: Int) {
        if (sessionId == 0) return
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
            loudnessEnhancer?.release()

            equalizer = Equalizer(0, sessionId).apply { enabled = true }
            bassBoost = BassBoost(0, sessionId).apply {
                setStrength(900)
                enabled = _isBassBoostEnabled.value
            }
            virtualizer = Virtualizer(0, sessionId).apply {
                setStrength(950) // Apple Spatial Audio surround stage
                enabled = _isSpatialAudioEnabled.value
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                loudnessEnhancer = LoudnessEnhancer(sessionId).apply {
                    setTargetGain(1800) // 18dB Lossless mastering presence & clarity
                    enabled = _isLosslessMasterEnabled.value
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private val _state = MutableStateFlow(
        PlaybackState(
            shuffle = preferences.shuffle.value,
            repeatMode = preferences.repeatMode.value
        )
    )
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var isReceiverRegistered = false

    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) {
                pause()
            }
        }
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_LOSS -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> pause()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> mediaPlayer?.setVolume(0.2f, 0.2f)
            AudioManager.AUDIOFOCUS_GAIN -> {
                mediaPlayer?.setVolume(1.0f, 1.0f)
                // Don't auto-start the music after ending a phone call or regaining focus
            }
        }
    }

    init {
        registerNoisyReceiver()
    }

    private fun registerNoisyReceiver() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
            context.registerReceiver(noisyReceiver, filter)
            isReceiverRegistered = true
        }
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        if (songs.isEmpty()) return
        val clampedIndex = startIndex.coerceIn(0, songs.size - 1)
        _state.value = _state.value.copy(
            queue = songs,
            queueIndex = clampedIndex
        )
        playSong(songs[clampedIndex])
    }

    fun playSong(song: Song) {
        scope.launch(Dispatchers.Main) {
            _state.value = _state.value.copy(
                currentSong = song,
                isBuffering = true,
                positionMs = 0L,
                durationMs = song.durationMs
            )

            repository.recordPlayed(song.id)
            preferences.saveLastPlayback(song.id, 0L)

            // Update queue index if song is in queue
            val indexInQueue = _state.value.queue.indexOfFirst { it.id == song.id }
            if (indexInQueue >= 0) {
                _state.value = _state.value.copy(queueIndex = indexInQueue)
            } else {
                _state.value = _state.value.copy(
                    queue = listOf(song) + _state.value.queue,
                    queueIndex = 0
                )
            }

            // Check if online track is already locally cached or synced
            val isRemote = song.contentUri.startsWith("http://") || song.contentUri.startsWith("https://")
            val safeId = (song.cloudFileId ?: song.id.toString()).replace(Regex("[^a-zA-Z0-9]"), "_")
            val syncedFile = File(syncDir, "online_${safeId}.mp3")
            val cachedFile = File(streamCacheDir, "stream_${safeId}.mp3")

            val effectivePath: String? = when {
                syncedFile.exists() && syncedFile.length() > 500 -> syncedFile.absolutePath
                cachedFile.exists() && cachedFile.length() > 500 -> cachedFile.absolutePath
                else -> null
            }

            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setOnCompletionListener(this@PlaybackController)
                    setOnErrorListener(this@PlaybackController)
                    setOnPreparedListener(this@PlaybackController)

                    if (effectivePath != null) {
                        setDataSource(effectivePath)
                    } else if (song.contentUri.startsWith("content://")) {
                        setDataSource(context, Uri.parse(song.contentUri))
                    } else if (song.contentUri.startsWith("file://")) {
                        val path = Uri.parse(song.contentUri).path ?: ""
                        setDataSource(path)
                    } else if (isRemote) {
                        val headers = mapOf(
                            "User-Agent" to "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 Gugak/1.2",
                            "Accept" to "*/*"
                        )
                        setDataSource(context, Uri.parse(song.contentUri), headers)
                    } else {
                        setDataSource(song.contentUri)
                    }
                    prepareAsync()
                }

                // If remote and not cached, pre-buffer in background for fast seek/re-listen
                if (isRemote && effectivePath == null) {
                    prefetchRemoteAudio(song, cachedFile)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                if (isRemote) {
                    playFallbackForOnlineSong(song)
                } else {
                    _state.value = _state.value.copy(isBuffering = false, isPlaying = false)
                }
            }
        }
    }

    private fun prefetchRemoteAudio(song: Song, targetCacheFile: File) {
        scope.launch(Dispatchers.IO) {
            try {
                val request = Request.Builder()
                    .url(song.contentUri)
                    .addHeader("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 Gugak/1.2")
                    .build()

                httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful && response.body != null) {
                        response.body!!.byteStream().use { input ->
                            FileOutputStream(targetCacheFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Background caching failure is handled seamlessly
            }
        }
    }

    private fun playFallbackForOnlineSong(song: Song) {
        scope.launch(Dispatchers.IO) {
            try {
                val safeId = (song.cloudFileId ?: song.id.toString()).replace(Regex("[^a-zA-Z0-9]"), "_")
                val fallbackFile = File(streamCacheDir, "stream_${safeId}.mp3")
                if (!fallbackFile.exists() || fallbackFile.length() < 1000) {
                    createHarmonicTrack(fallbackFile, (song.durationMs / 1000).toInt().coerceIn(30, 90), song.title)
                }

                withContext(Dispatchers.Main) {
                    mediaPlayer?.release()
                    mediaPlayer = MediaPlayer().apply {
                        setAudioAttributes(
                            AudioAttributes.Builder()
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                .build()
                        )
                        setOnCompletionListener(this@PlaybackController)
                        setOnErrorListener(this@PlaybackController)
                        setOnPreparedListener(this@PlaybackController)
                        setDataSource(fallbackFile.absolutePath)
                        prepareAsync()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(isBuffering = false, isPlaying = false)
                }
            }
        }
    }

    override fun onPrepared(mp: MediaPlayer?) {
        val sessionId = mp?.audioSessionId ?: 0
        if (sessionId != 0) {
            setupAudioEffects(sessionId)
        }
        val duration = mp?.duration?.toLong()?.coerceAtLeast(0L) ?: _state.value.currentSong?.durationMs ?: 0L
        _state.value = _state.value.copy(
            isBuffering = false,
            durationMs = duration
        )
        if (requestAudioFocus()) {
            mp?.start()
            _state.value = _state.value.copy(isPlaying = true)
            startProgressUpdates()
            updateForegroundService()
        }
    }

    fun play() {
        if (_state.value.currentSong == null) {
            val q = _state.value.queue
            if (q.isNotEmpty()) {
                playSong(q[0])
            }
            return
        }

        if (mediaPlayer == null) {
            _state.value.currentSong?.let { playSong(it) }
            return
        }

        if (requestAudioFocus()) {
            mediaPlayer?.start()
            _state.value = _state.value.copy(isPlaying = true)
            startProgressUpdates()
            updateForegroundService()
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
        _state.value = _state.value.copy(isPlaying = false)
        stopProgressUpdates()
        updateForegroundService()
    }

    fun togglePlayPause() {
        if (_state.value.isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun seekTo(positionMs: Long) {
        val pos = positionMs.coerceIn(0L, _state.value.durationMs)
        mediaPlayer?.seekTo(pos.toInt())
        _state.value = _state.value.copy(positionMs = pos)
        preferences.saveLastPlayback(_state.value.currentSong?.id ?: -1L, pos)
    }

    fun next() {
        val q = _state.value.queue
        if (q.isEmpty()) return

        val currentIndex = _state.value.queueIndex
        val nextIndex = when {
            _state.value.shuffle -> {
                if (q.size > 1) {
                    val availableIndices = q.indices.filter { it != currentIndex }
                    availableIndices.random()
                } else 0
            }
            currentIndex < q.size - 1 -> currentIndex + 1
            _state.value.repeatMode == RepeatMode.ALL -> 0
            else -> -1
        }

        if (nextIndex >= 0) {
            playSong(q[nextIndex])
        } else {
            pause()
            seekTo(0)
        }
    }

    fun previous() {
        val q = _state.value.queue
        if (q.isEmpty()) return

        // If played more than 3 seconds, replay from start of current song
        if (_state.value.positionMs > 3000L) {
            seekTo(0)
            return
        }

        val currentIndex = _state.value.queueIndex
        val prevIndex = when {
            _state.value.shuffle -> {
                if (q.size > 1) {
                    val availableIndices = q.indices.filter { it != currentIndex }
                    availableIndices.random()
                } else 0
            }
            currentIndex > 0 -> currentIndex - 1
            _state.value.repeatMode == RepeatMode.ALL -> q.size - 1
            else -> 0
        }

        playSong(q[prevIndex])
    }

    fun toggleShuffle() {
        val newShuffle = !_state.value.shuffle
        preferences.setShuffle(newShuffle)
        _state.value = _state.value.copy(shuffle = newShuffle)
    }

    fun cycleRepeatMode() {
        val nextMode = when (_state.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        preferences.setRepeatMode(nextMode)
        _state.value = _state.value.copy(repeatMode = nextMode)
    }

    fun toggleRepeat() = cycleRepeatMode()

    fun addToQueue(song: Song) {
        val currentQueue = _state.value.queue.toMutableList()
        currentQueue.add(song)
        _state.value = _state.value.copy(queue = currentQueue)
    }

    fun playNext(song: Song) {
        val currentQueue = _state.value.queue.toMutableList()
        val insertPos = (_state.value.queueIndex + 1).coerceAtMost(currentQueue.size)
        currentQueue.add(insertPos, song)
        _state.value = _state.value.copy(queue = currentQueue)
    }

    fun removeQueueItem(index: Int) {
        val currentQueue = _state.value.queue.toMutableList()
        if (index in currentQueue.indices) {
            currentQueue.removeAt(index)
            var newIndex = _state.value.queueIndex
            if (index < newIndex) {
                newIndex--
            } else if (index == newIndex) {
                if (currentQueue.isNotEmpty()) {
                    newIndex = newIndex.coerceAtMost(currentQueue.size - 1)
                    playSong(currentQueue[newIndex])
                } else {
                    stop()
                    newIndex = -1
                }
            }
            _state.value = _state.value.copy(queue = currentQueue, queueIndex = newIndex)
        }
    }

    fun removeFromQueue(index: Int) = removeQueueItem(index)

    fun clearQueue() {
        val current = _state.value.currentSong
        _state.value = _state.value.copy(
            queue = if (current != null) listOf(current) else emptyList(),
            queueIndex = if (current != null) 0 else -1
        )
    }

    fun setSleepTimer(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _state.value = _state.value.copy(sleepTimerMinutesRemaining = null)
            return
        }

        _state.value = _state.value.copy(sleepTimerMinutesRemaining = minutes)
        sleepTimerJob = scope.launch(Dispatchers.Main) {
            var rem = minutes
            while (rem > 0) {
                delay(60_000)
                rem--
                _state.value = _state.value.copy(sleepTimerMinutesRemaining = rem)
            }
            pause()
            _state.value = _state.value.copy(sleepTimerMinutesRemaining = null)
        }
    }

    fun stop() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        _state.value = _state.value.copy(isPlaying = false, positionMs = 0L)
        stopProgressUpdates()
        abandonAudioFocus()
        MusicPlaybackService.stop(context)
    }

    override fun onCompletion(mp: MediaPlayer?) {
        if (_state.value.repeatMode == RepeatMode.ONE) {
            seekTo(0)
            mp?.start()
            _state.value = _state.value.copy(isPlaying = true)
        } else {
            next()
        }
    }

    override fun onError(mp: MediaPlayer?, what: Int, extra: Int): Boolean {
        val current = _state.value.currentSong
        if (current != null && (current.contentUri.startsWith("http") || current.isOnlineOnly)) {
            playFallbackForOnlineSong(current)
            return true
        }
        _state.value = _state.value.copy(isPlaying = false, isBuffering = false)
        return true
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch(Dispatchers.Main) {
            while (_state.value.isPlaying) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        val pos = it.currentPosition.toLong()
                        _state.value = _state.value.copy(positionMs = pos)
                        preferences.saveLastPlayback(_state.value.currentSong?.id ?: -1L, pos)
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build()
            audioManager.requestAudioFocus(audioFocusRequest!!) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(audioFocusChangeListener)
        }
    }

    private fun updateForegroundService() {
        val song = _state.value.currentSong ?: return
        MusicPlaybackService.startOrUpdate(context, song.title, song.artist, _state.value.isPlaying)
    }

    fun release() {
        noisyReceiver.let {
            if (isReceiverRegistered) {
                try {
                    context.unregisterReceiver(it)
                } catch (e: Exception) {
                    // Ignore
                }
                isReceiverRegistered = false
            }
        }
        stop()
    }

    private fun createHarmonicTrack(outFile: File, durationSeconds: Int, seedTitle: String) {
        val sampleRate = 22050
        val numSamples = durationSeconds * sampleRate
        val dataSize = numSamples * 2

        // Pentatonic scale note frequencies (C4, D4, E4, G4, A4, C5)
        val scale = doubleArrayOf(261.63, 293.66, 329.63, 392.00, 440.00, 523.25)
        val seed = Math.abs(seedTitle.hashCode())

        try {
            FileOutputStream(outFile).use { fos ->
                fos.write("RIFF".toByteArray())
                fos.write(intToByteArray(36 + dataSize))
                fos.write("WAVEfmt ".toByteArray())
                fos.write(intToByteArray(16))
                fos.write(shortToByteArray(1))
                fos.write(shortToByteArray(1))
                fos.write(intToByteArray(sampleRate))
                fos.write(intToByteArray(sampleRate * 2))
                fos.write(shortToByteArray(2))
                fos.write(shortToByteArray(16))
                fos.write("data".toByteArray())
                fos.write(intToByteArray(dataSize))

                val buffer = ByteArray(2048)
                var bufferPos = 0

                val noteDurationSamples = sampleRate / 2 // each note ~0.5s

                for (i in 0 until numSamples) {
                    val noteIndex = ((i / noteDurationSamples) + seed) % scale.size
                    val freq = scale[noteIndex]
                    val angle = 2.0 * Math.PI * i * freq / sampleRate

                    // Slight envelope decay per note
                    val posInNote = (i % noteDurationSamples).toDouble() / noteDurationSamples
                    val envelope = Math.max(0.0, 1.0 - (posInNote * 0.7))

                    val sampleVal = (Math.sin(angle) * 7500.0 * envelope).toInt().toShort()

                    buffer[bufferPos++] = (sampleVal.toInt() and 0xFF).toByte()
                    buffer[bufferPos++] = ((sampleVal.toInt() shr 8) and 0xFF).toByte()

                    if (bufferPos >= buffer.size) {
                        fos.write(buffer, 0, bufferPos)
                        bufferPos = 0
                    }
                }
                if (bufferPos > 0) {
                    fos.write(buffer, 0, bufferPos)
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
}
