package com.example.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.model.RepeatMode
import com.example.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

class MusicPlayerEngine(private val context: Context) {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isBuffering = MutableStateFlow(false)
    val isBuffering: StateFlow<Boolean> = _isBuffering.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _visualizerLevels = MutableStateFlow(List(24) { 0.1f })
    val visualizerLevels: StateFlow<List<Float>> = _visualizerLevels.asStateFlow()

    private val _sleepMinutesLeft = MutableStateFlow(0)
    val sleepMinutesLeft: StateFlow<Int> = _sleepMinutesLeft.asStateFlow()

    var onTrackCompleted: (() -> Unit)? = null
    var currentTrack: Track? = null
        private set

    var playbackSpeed: Float = 1.0f
        set(value) {
            field = value
            applyPlaybackSpeed()
        }

    fun playTrack(track: Track, onComplete: () -> Unit) {
        currentTrack = track
        onTrackCompleted = onComplete
        _isBuffering.value = true

        scope.launch(Dispatchers.IO) {
            releasePlayer()

            try {
                val isRemote = track.audioUrl.startsWith("http://") || track.audioUrl.startsWith("https://")
                val sourcePath = if (isRemote) {
                    track.audioUrl
                } else if (track.isLocal && File(track.audioUrl).exists()) {
                    track.audioUrl
                } else {
                    val audioFile = AudioSynthHelper.getOrCreateTrackAudio(context, track.id, track.artist)
                    audioFile.absolutePath
                }

                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(sourcePath)

                    setOnPreparedListener { mp ->
                        scope.launch(Dispatchers.Main) {
                            _isBuffering.value = false
                            _duration.value = mp.duration.toLong().coerceAtLeast(1000L)
                            applyPlaybackSpeed()
                            mp.start()
                            _isPlaying.value = true
                            startProgressTracker()
                        }
                    }

                    setOnCompletionListener {
                        scope.launch(Dispatchers.Main) {
                            _isPlaying.value = false
                            onTrackCompleted?.invoke()
                        }
                    }

                    setOnErrorListener { _, _, _ ->
                        scope.launch(Dispatchers.Main) {
                            _isBuffering.value = false
                            fallbackToSynthAudio(track)
                        }
                        true
                    }

                    prepareAsync()
                }
                mediaPlayer = player
            } catch (e: Exception) {
                e.printStackTrace()
                scope.launch(Dispatchers.Main) {
                    fallbackToSynthAudio(track)
                }
            }
        }
    }

    private fun fallbackToSynthAudio(track: Track) {
        scope.launch(Dispatchers.IO) {
            releasePlayer()
            try {
                val audioFile = AudioSynthHelper.getOrCreateTrackAudio(context, track.id, track.artist)
                val player = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
                    )
                    setDataSource(audioFile.absolutePath)
                    setOnPreparedListener { mp ->
                        scope.launch(Dispatchers.Main) {
                            _isBuffering.value = false
                            _duration.value = mp.duration.toLong().coerceAtLeast(1000L)
                            applyPlaybackSpeed()
                            mp.start()
                            _isPlaying.value = true
                            startProgressTracker()
                        }
                    }
                    setOnCompletionListener {
                        scope.launch(Dispatchers.Main) {
                            _isPlaying.value = false
                            onTrackCompleted?.invoke()
                        }
                    }
                    setOnErrorListener { _, _, _ ->
                        scope.launch(Dispatchers.Main) {
                            _isBuffering.value = false
                            _isPlaying.value = false
                        }
                        true
                    }
                    prepareAsync()
                }
                mediaPlayer = player
            } catch (e: Exception) {
                e.printStackTrace()
                scope.launch(Dispatchers.Main) {
                    _isBuffering.value = false
                    _isPlaying.value = false
                }
            }
        }
    }

    fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                _isPlaying.value = false
            }
        }
    }

    fun resume() {
        mediaPlayer?.let {
            it.start()
            _isPlaying.value = true
            startProgressTracker()
        } ?: run {
            currentTrack?.let { track ->
                onTrackCompleted?.let { callback -> playTrack(track, callback) }
            }
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    fun seekTo(positionMs: Long) {
        mediaPlayer?.let {
            val target = positionMs.coerceIn(0L, it.duration.toLong())
            it.seekTo(target.toInt())
            _currentPosition.value = target
        }
    }

    private fun applyPlaybackSpeed() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            mediaPlayer?.let {
                try {
                    val params = it.playbackParams ?: PlaybackParams()
                    params.speed = playbackSpeed
                    it.playbackParams = params
                } catch (e: Exception) {
                    // Ignored on unsupported devices
                }
            }
        }
    }

    fun setSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        _sleepMinutesLeft.value = minutes
        if (minutes <= 0) return

        sleepTimerJob = scope.launch {
            var remaining = minutes
            while (remaining > 0 && isActive) {
                delay(60000L)
                remaining--
                _sleepMinutesLeft.value = remaining
            }
            if (remaining <= 0) {
                pause()
                _sleepMinutesLeft.value = 0
            }
        }
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var visualizerPhase = 0.0
            while (isActive && _isPlaying.value) {
                mediaPlayer?.let { player ->
                    try {
                        if (player.isPlaying) {
                            _currentPosition.value = player.currentPosition.toLong()
                            val dur = player.duration.toLong()
                            if (dur > 0) _duration.value = dur

                            // Dynamically generate rhythm waveform heights matching tempo
                            visualizerPhase += 0.25
                            val bars = List(24) { i ->
                                val offset = i * 0.3
                                val wave = abs(sin(visualizerPhase + offset)).toFloat()
                                val flutter = Random.nextFloat() * 0.2f
                                (wave * 0.75f + flutter).coerceIn(0.12f, 1.0f)
                            }
                            _visualizerLevels.value = bars
                        }
                    } catch (e: Exception) {
                        // ignore state errors during async transitions
                    }
                }
                delay(120L)
            }
            // Reset visualizer bars when paused
            _visualizerLevels.value = List(24) { 0.1f }
        }
    }

    private fun releasePlayer() {
        progressJob?.cancel()
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
    }

    fun release() {
        sleepTimerJob?.cancel()
        releasePlayer()
    }
}
