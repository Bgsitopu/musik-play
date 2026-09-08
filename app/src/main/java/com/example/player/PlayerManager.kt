package com.example.player

import android.content.Context
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import com.example.data.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PlayerManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var progressJob: Job? = null

    val exoPlayer: ExoPlayer by lazy {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
            updateAudioAttributes(ignoreAudioFocus = _isConcurrentPlayback.value)

            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isPlaying.value = isPlaying
                    if (isPlaying) {
                        startProgressUpdates()
                    } else {
                        stopProgressUpdates()
                    }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    _duration.value = duration.coerceAtLeast(0L)
                    if (playbackState == Player.STATE_READY) {
                        _duration.value = duration.coerceAtLeast(0L)
                        setupAudioEffects(audioSessionId)
                    } else if (playbackState == Player.STATE_ENDED) {
                        playNext()
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    Log.e("PlayerManager", "Player error: ${error.message}", error)
                }
            })

            addAnalyticsListener(object : AnalyticsListener {
                override fun onAudioSessionIdChanged(
                    eventTime: AnalyticsListener.EventTime,
                    audioSessionId: Int
                ) {
                    setupAudioEffects(audioSessionId)
                }
            })
        }
    }

    private val _currentVideo = MutableStateFlow<VideoItem?>(null)
    val currentVideo: StateFlow<VideoItem?> = _currentVideo.asStateFlow()

    private val _playlist = MutableStateFlow<List<VideoItem>>(emptyList())
    val playlist: StateFlow<List<VideoItem>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(-1)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    // Independent video volume: 0.0f to 1.0f (or boosted)
    private val _videoVolume = MutableStateFlow(1.0f)
    val videoVolume: StateFlow<Float> = _videoVolume.asStateFlow()

    // Concurrent playback (ignore audio focus to keep playing without interruption when other apps play videos)
    private val _isConcurrentPlayback = MutableStateFlow(true)
    val isConcurrentPlayback: StateFlow<Boolean> = _isConcurrentPlayback.asStateFlow()

    // Background playback mode (like music)
    private val _isBackgroundMode = MutableStateFlow(false)
    val isBackgroundMode: StateFlow<Boolean> = _isBackgroundMode.asStateFlow()

    // Equalizer & Audio FX state
    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    private val _equalizerEnabled = MutableStateFlow(true)
    val equalizerEnabled: StateFlow<Boolean> = _equalizerEnabled.asStateFlow()

    private val _bassStrength = MutableStateFlow(300) // 0 to 1000
    val bassStrength: StateFlow<Int> = _bassStrength.asStateFlow()

    private val _trebleStrength = MutableStateFlow(200) // -1000 to 1000
    val trebleStrength: StateFlow<Int> = _trebleStrength.asStateFlow()

    private val _equalizerBands = MutableStateFlow<List<EqualizerBand>>(emptyList())
    val equalizerBands: StateFlow<List<EqualizerBand>> = _equalizerBands.asStateFlow()

    private val _currentPreset = MutableStateFlow("Normal")
    val currentPreset: StateFlow<String> = _currentPreset.asStateFlow()

    init {
        // Initialize default bands representation
        initDefaultBands()
    }

    private fun initDefaultBands() {
        val defaultFrequencies = intArrayOf(60, 230, 910, 3600, 14000)
        _equalizerBands.value = defaultFrequencies.mapIndexed { index, freq ->
            EqualizerBand(
                index = index.toShort(),
                centerFreqHz = freq,
                minLevelMilliBels = -1500,
                maxLevelMilliBels = 1500,
                currentLevelMilliBels = 0
            )
        }
    }

    fun setConcurrentPlayback(enabled: Boolean) {
        _isConcurrentPlayback.value = enabled
        updateAudioAttributes(ignoreAudioFocus = enabled)
    }

    private fun updateAudioAttributes(ignoreAudioFocus: Boolean) {
        try {
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build()
            // When handleAudioFocus is FALSE, ExoPlayer will NOT pause when other apps play media!
            exoPlayer.setAudioAttributes(audioAttributes, /* handleAudioFocus = */ !ignoreAudioFocus)
        } catch (e: Exception) {
            Log.w("PlayerManager", "Could not set audio attributes", e)
        }
    }

    fun playVideo(video: VideoItem, playlist: List<VideoItem> = listOf(video)) {
        _playlist.value = playlist
        val index = playlist.indexOfFirst { it.id == video.id }.let { if (it == -1) 0 else it }
        _currentIndex.value = index
        _currentVideo.value = video

        val mediaItem = MediaItem.Builder()
            .setUri(video.uri)
            .setMediaId(video.id.toString())
            .build()

        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
    }

    fun playNext() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val nextIndex = _currentIndex.value + 1
        if (nextIndex < list.size) {
            playVideo(list[nextIndex], list)
        } else if (exoPlayer.repeatMode == Player.REPEAT_MODE_ALL) {
            playVideo(list[0], list)
        }
    }

    fun playPrevious() {
        val list = _playlist.value
        if (list.isEmpty()) return
        val prevIndex = _currentIndex.value - 1
        if (prevIndex >= 0) {
            playVideo(list[prevIndex], list)
        } else {
            seekTo(0L)
        }
    }

    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            exoPlayer.play()
        }
    }

    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
        _currentPosition.value = positionMs
    }

    fun seekBy(offsetMs: Long) {
        val newPos = (exoPlayer.currentPosition + offsetMs).coerceIn(0L, exoPlayer.duration.coerceAtLeast(0L))
        seekTo(newPos)
    }

    // Set dedicated volume for current video (0.0f to 1.0f)
    fun setVideoVolume(volume: Float) {
        val clamped = volume.coerceIn(0.0f, 1.0f)
        _videoVolume.value = clamped
        exoPlayer.volume = clamped
    }

    fun setBackgroundMode(enabled: Boolean) {
        _isBackgroundMode.value = enabled
    }

    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                _currentPosition.value = exoPlayer.currentPosition
                _duration.value = exoPlayer.duration.coerceAtLeast(0L)
                delay(200)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
        _currentPosition.value = exoPlayer.currentPosition
    }

    // Audio Effects & Equalizer
    private fun setupAudioEffects(sessionId: Int) {
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId <= 0) return
        try {
            // Equalizer
            try {
                equalizer?.release()
                equalizer = Equalizer(0, sessionId).apply {
                    enabled = _equalizerEnabled.value
                }
                loadEqualizerBandsFromHardware()
            } catch (e: Exception) {
                Log.w("PlayerManager", "Hardware Equalizer setup note: ${e.message}")
            }

            // Bass Boost
            try {
                bassBoost?.release()
                bassBoost = BassBoost(0, sessionId).apply {
                    enabled = _equalizerEnabled.value
                    if (strengthSupported) {
                        setStrength(_bassStrength.value.toShort())
                    }
                }
            } catch (e: Exception) {
                Log.w("PlayerManager", "BassBoost setup note: ${e.message}")
            }

            applyTrebleToEqualizer(_trebleStrength.value)
        } catch (e: Exception) {
            Log.w("PlayerManager", "setupAudioEffects error: ${e.message}")
        }
    }

    private fun loadEqualizerBandsFromHardware() {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            val (minRange, maxRange) = eq.bandLevelRange
            val updatedBands = mutableListOf<EqualizerBand>()

            for (i in 0 until numBands) {
                val bandIndex = i.toShort()
                val freq = eq.getCenterFreq(bandIndex) / 1000 // mHz to Hz
                val currentLevel = eq.getBandLevel(bandIndex)
                updatedBands.add(
                    EqualizerBand(
                        index = bandIndex,
                        centerFreqHz = freq,
                        minLevelMilliBels = minRange,
                        maxLevelMilliBels = maxRange,
                        currentLevelMilliBels = currentLevel
                    )
                )
            }
            if (updatedBands.isNotEmpty()) {
                _equalizerBands.value = updatedBands
            }
        } catch (e: Exception) {
            Log.w("PlayerManager", "Error reading equalizer bands: ${e.message}")
        }
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerEnabled.value = enabled
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
        } catch (e: Exception) {
            Log.w("PlayerManager", "Failed to toggle equalizer: ${e.message}")
        }
    }

    fun setBassStrength(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        _bassStrength.value = clamped
        try {
            bassBoost?.let {
                if (it.strengthSupported) {
                    it.setStrength(clamped.toShort())
                }
            }
        } catch (e: Exception) {
            Log.w("PlayerManager", "Failed to set bass strength: ${e.message}")
        }
    }

    fun setTrebleStrength(strength: Int) {
        val clamped = strength.coerceIn(-1000, 1000)
        _trebleStrength.value = clamped
        applyTrebleToEqualizer(clamped)
    }

    private fun applyTrebleToEqualizer(trebleVal: Int) {
        val eq = equalizer ?: return
        try {
            val numBands = eq.numberOfBands
            if (numBands <= 0) return
            val lastBandIndex = (numBands - 1).toShort()
            val secondLastBandIndex = if (numBands > 2) (numBands - 2).toShort() else null

            val (minRange, maxRange) = eq.bandLevelRange
            val scaledLevel = (trebleVal * maxRange / 1000).coerceIn(minRange.toInt(), maxRange.toInt()).toShort()

            eq.setBandLevel(lastBandIndex, scaledLevel)
            secondLastBandIndex?.let {
                eq.setBandLevel(it, (scaledLevel / 2).toShort())
            }

            // Reflect in UI state
            _equalizerBands.value = _equalizerBands.value.map { band ->
                when (band.index) {
                    lastBandIndex -> band.copy(currentLevelMilliBels = scaledLevel)
                    secondLastBandIndex -> band.copy(currentLevelMilliBels = (scaledLevel / 2).toShort())
                    else -> band
                }
            }
        } catch (e: Exception) {
            Log.w("PlayerManager", "Failed to apply treble: ${e.message}")
        }
    }

    fun setBandLevel(bandIndex: Short, levelMilliBels: Short) {
        _currentPreset.value = "Custom"
        try {
            equalizer?.setBandLevel(bandIndex, levelMilliBels)
        } catch (e: Exception) {
            Log.w("PlayerManager", "Failed to set band level: ${e.message}")
        }
        _equalizerBands.value = _equalizerBands.value.map { band ->
            if (band.index == bandIndex) {
                band.copy(currentLevelMilliBels = levelMilliBels)
            } else {
                band
            }
        }
    }

    fun applyPreset(presetName: String) {
        _currentPreset.value = presetName
        val bands = _equalizerBands.value
        val bandCount = bands.size
        if (bandCount == 0) return

        val levels: List<Short> = when (presetName) {
            "Bass Boost" -> {
                setBassStrength(900)
                setTrebleStrength(100)
                listOf(700, 500, 100, -100, -200).map { it.toShort() }
            }
            "Treble Boost" -> {
                setBassStrength(200)
                setTrebleStrength(900)
                listOf(-200, 0, 200, 600, 900).map { it.toShort() }
            }
            "Rock" -> {
                setBassStrength(600)
                setTrebleStrength(500)
                listOf(500, 300, -100, 400, 600).map { it.toShort() }
            }
            "Pop" -> {
                setBassStrength(400)
                setTrebleStrength(300)
                listOf(-100, 200, 500, 300, -100).map { it.toShort() }
            }
            "Jazz" -> {
                setBassStrength(400)
                setTrebleStrength(400)
                listOf(300, 200, -100, 200, 400).map { it.toShort() }
            }
            "Vocal" -> {
                setBassStrength(100)
                setTrebleStrength(700)
                listOf(-300, 100, 700, 400, 100).map { it.toShort() }
            }
            else -> { // Normal / Flat
                setBassStrength(300)
                setTrebleStrength(0)
                listOf(0, 0, 0, 0, 0).map { it.toShort() }
            }
        }

        bands.forEachIndexed { i, band ->
            val level = if (i < levels.size) levels[i] else 0.toShort()
            try {
                equalizer?.setBandLevel(band.index, level)
            } catch (e: Exception) {
                // Ignore
            }
        }
        _equalizerBands.value = bands.mapIndexed { i, band ->
            val level = if (i < levels.size) levels[i] else 0.toShort()
            band.copy(currentLevelMilliBels = level)
        }
    }

    fun stopAndClear() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _currentVideo.value = null
        _isPlaying.value = false
        _currentPosition.value = 0L
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
            exoPlayer.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        @Volatile
        private var instance: PlayerManager? = null

        fun getInstance(context: Context): PlayerManager {
            return instance ?: synchronized(this) {
                instance ?: PlayerManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
