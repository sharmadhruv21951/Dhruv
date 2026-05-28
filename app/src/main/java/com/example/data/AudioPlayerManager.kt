package com.example.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AudioPlayerManager(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    
    private val _volumeBoost = MutableStateFlow(1.0f) // 1.0f = 100%, 1.5f = 150%, 2.0f = 200%, 3.0f = 300%
    val volumeBoost: StateFlow<Float> = _volumeBoost.asStateFlow()
    
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    
    private val _currentSong = MutableStateFlow<Song?>(null)
    val currentSong: StateFlow<Song?> = _currentSong.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Queue tracking
    private var playlistQueue: List<Song> = emptyList()
    private var currentIndex: Int = -1
    private var isSimulating = false

    init {
        initMediaPlayer()
    }

    private fun initMediaPlayer() {
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setOnPreparedListener { player ->
                    _isLoading.value = false
                    isSimulating = false
                    _duration.value = player.duration.toLong()
                    player.start()
                    _isPlaying.value = true
                    applyLoudnessEnhancer()
                    startProgressTracker(simulated = false)
                }
                setOnCompletionListener {
                    _isPlaying.value = false
                    stopProgressTracker()
                    playNext()
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("AudioPlayerManager", "MediaPlayer error: what=$what extra=$extra. Commencing simulation fallback.")
                    _isLoading.value = false
                    startSimulationFallback()
                    true // prevent error dialog
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Failed to construct native MediaPlayer, fallback simulation enabled.")
        }
    }

    fun playSongFromQueue(queue: List<Song>, index: Int) {
        if (queue.isEmpty() || index < 0 || index >= queue.size) return
        playlistQueue = queue
        currentIndex = index
        val song = queue[index]
        playSong(song)
    }

    fun playSong(song: Song) {
        _currentSong.value = song
        _currentPosition.value = 0L
        _isLoading.value = true
        _isPlaying.value = false
        stopProgressTracker()

        val player = mediaPlayer
        if (player != null) {
            try {
                player.reset()
                player.setDataSource(context, Uri.parse(song.audioUrl))
                player.prepareAsync()
            } catch (e: Exception) {
                Log.e("AudioPlayerManager", "Source load error: ${e.message}. Initializing simulation fallback.")
                startSimulationFallback()
            }
        } else {
            startSimulationFallback()
        }
    }

    private fun startSimulationFallback() {
        val song = _currentSong.value ?: return
        isSimulating = true
        _isLoading.value = false
        _isPlaying.value = true
        _duration.value = song.durationMs
        startProgressTracker(simulated = true)
    }

    fun togglePlayPause() {
        if (isSimulating) {
            val playing = _isPlaying.value
            _isPlaying.value = !playing
            if (!playing) startProgressTracker(simulated = true) else stopProgressTracker()
            return
        }

        val player = mediaPlayer ?: return
        try {
            if (player.isPlaying) {
                player.pause()
                _isPlaying.value = false
                stopProgressTracker()
            } else {
                if (_duration.value > 0) {
                    player.start()
                    _isPlaying.value = true
                    startProgressTracker(simulated = false)
                } else {
                    _currentSong.value?.let { playSong(it) }
                }
            }
        } catch (e: Exception) {
            Log.e("AudioPlayerManager", "Toggle play error: ${e.message}. Switch to simulation.")
            startSimulationFallback()
        }
    }

    fun seekTo(positionMs: Long) {
        if (isSimulating) {
            _currentPosition.value = positionMs.coerceIn(0L, _duration.value)
            return
        }

        val player = mediaPlayer ?: return
        try {
            player.seekTo(positionMs.toInt())
            _currentPosition.value = positionMs
        } catch (e: Exception) {
            _currentPosition.value = positionMs
        }
    }

    fun playNext() {
        if (playlistQueue.isNotEmpty() && currentIndex < playlistQueue.size - 1) {
            currentIndex++
            playSong(playlistQueue[currentIndex])
        } else if (playlistQueue.isNotEmpty()) {
            currentIndex = 0
            playSong(playlistQueue[0])
        }
    }

    fun playPrevious() {
        if (playlistQueue.isNotEmpty() && currentIndex > 0) {
            currentIndex--
            playSong(playlistQueue[currentIndex])
        } else if (playlistQueue.isNotEmpty()) {
            currentIndex = playlistQueue.size - 1
            playSong(playlistQueue[currentIndex])
        }
    }

    private fun startProgressTracker(simulated: Boolean) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                if (simulated) {
                    val nextPos = _currentPosition.value + 1000
                    if (nextPos >= _duration.value) {
                        _currentPosition.value = _duration.value
                        _isPlaying.value = false
                        stopProgressTracker()
                        playNext()
                        break
                    } else {
                        _currentPosition.value = nextPos
                    }
                } else {
                    val player = mediaPlayer
                    if (player != null && player.isPlaying) {
                        _currentPosition.value = player.currentPosition.toLong()
                    }
                }
                delay(1000)
            }
        }
    }

    fun setVolumeBoost(multiplier: Float) {
        _volumeBoost.value = multiplier
        applyLoudnessEnhancer()
    }

    private fun applyLoudnessEnhancer() {
        val player = mediaPlayer ?: return
        val boost = _volumeBoost.value
        try {
            // Clean up previous enhancer
            loudnessEnhancer?.release()
            loudnessEnhancer = null

            if (boost > 1.0f) {
                val sessionId = player.audioSessionId
                if (sessionId != 0) {
                    val enhancer = LoudnessEnhancer(sessionId)
                    enhancer.enabled = true
                    // Convert multiplier: 1.5 -> 1500mB, 2.0 -> 3000mB, 3.0 -> 6000mB
                    val gain = ((boost - 1.0f) * 3000).toInt()
                    enhancer.setTargetGain(gain)
                    loudnessEnhancer = enhancer
                    Log.d("AudioPlayerManager", "LoudnessEnhancer active (Session: $sessionId, Gain: $gain mB)")
                }
            }
        } catch (e: Throwable) {
            Log.e("AudioPlayerManager", "Failed to apply LoudnessEnhancer: ${e.message}")
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        scope.cancel()
        try {
            loudnessEnhancer?.release()
        } catch (e: Exception) {
            // ignore
        }
        loudnessEnhancer = null
        try {
            mediaPlayer?.release()
        } catch (e: Exception) {
            // ignore
        }
        mediaPlayer = null
    }
}
