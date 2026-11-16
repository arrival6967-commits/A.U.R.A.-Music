package com.example.audio

import android.content.Context
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.media.audiofx.Virtualizer
import android.net.Uri
import android.os.Build
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.TrackEntity
import com.example.service.MusicPlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class RepeatMode {
    OFF, ALL, ONE
}

data class EqualizerState(
    val isEnabled: Boolean = true,
    val activePresetId: Long = 1L,
    val activePresetName: String = "Студийный Hi-Fi",
    // 5 bands in dB (-15f..+15f): 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val bandGainsDb: List<Float> = listOf(2.5f, 1.0f, 0.0f, 1.8f, 3.0f),
    val bassBoostStrength: Int = 350,     // 0..1000
    val virtualizerStrength: Int = 250,   // 0..1000
    val loudnessGainDb: Float = 1.5f,     // 0f..10f
    val hardwareAudioFxActive: Boolean = false
)

data class PlaybackState(
    val currentTrack: TrackEntity? = null,
    val currentCoverBitmap: Bitmap? = null,
    val coverDominantColorArgb: Int? = null,
    val coverSecondaryColorArgb: Int? = null,
    val queue: List<TrackEntity> = emptyList(),
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val repeatMode: RepeatMode = RepeatMode.ALL,
    val isShuffleEnabled: Boolean = false,
    val playbackSpeed: Float = 1.0f,
    val sleepTimerRemainingSeconds: Int? = null,
    val spectrumBars: List<Float> = List(18) { 0.12f },
    val audioSessionId: Int = 0
)

class AudioPlaybackController private constructor(private val appContext: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaPlayer: MediaPlayer? = null
    private var equalizerEffect: Equalizer? = null
    private var bassBoostEffect: BassBoost? = null
    private var virtualizerEffect: Virtualizer? = null
    private var loudnessEnhancerEffect: LoudnessEnhancer? = null

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var onTrackPlayedCallback: ((Long) -> Unit)? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerState())
    val equalizerState: StateFlow<EqualizerState> = _equalizerState.asStateFlow()

    fun setOnTrackPlayedListener(listener: (Long) -> Unit) {
        onTrackPlayedCallback = listener
    }

    fun playTrack(track: TrackEntity, newQueue: List<TrackEntity>? = null) {
        scope.launch {
            val updatedQueue = newQueue ?: _playbackState.value.queue.ifEmpty { listOf(track) }
            val coverResult = AlbumArtLoader.loadCoverArt(appContext, track, targetSizePx = 512)

            _playbackState.update {
                it.copy(
                    currentTrack = track,
                    currentCoverBitmap = coverResult.bitmap,
                    coverDominantColorArgb = track.dominantColorArgb ?: coverResult.dominantColorArgb,
                    coverSecondaryColorArgb = track.secondaryColorArgb ?: coverResult.secondaryColorArgb,
                    queue = updatedQueue,
                    currentPositionMs = 0L,
                    durationMs = track.durationMs.coerceAtLeast(1000L)
                )
            }

            onTrackPlayedCallback?.invoke(track.id)

            val prepared = prepareAndStartMediaPlayer(track)
            _playbackState.update {
                it.copy(
                    isPlaying = prepared,
                    durationMs = mediaPlayer?.duration?.toLong()?.takeIf { d -> d > 0 } ?: track.durationMs
                )
            }
            startProgressUpdater()
            MusicPlaybackService.startOrUpdateService(appContext)
        }
    }

    private fun prepareAndStartMediaPlayer(track: TrackEntity): Boolean {
        return try {
            releasePlayerOnly()
            val player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                setDataSource(appContext, Uri.parse(track.contentUri))
                prepare()
            }

            applyPlaybackSpeedInternal(player, _playbackState.value.playbackSpeed)
            player.setOnCompletionListener {
                onTrackCompleted()
            }
            player.start()
            mediaPlayer = player

            val sessionId = player.audioSessionId
            _playbackState.update { it.copy(audioSessionId = sessionId) }
            attachAudioEffects(sessionId)
            true
        } catch (_: Exception) {
            true
        }
    }

    private fun attachAudioEffects(sessionId: Int) {
        if (sessionId <= 0) return
        releaseAudioEffects()
        var hwActive = false
        try {
            val eq = Equalizer(0, sessionId)
            eq.enabled = _equalizerState.value.isEnabled
            equalizerEffect = eq
            hwActive = true
        } catch (_: Exception) {
            equalizerEffect = null
        }

        try {
            val bb = BassBoost(0, sessionId)
            bb.enabled = _equalizerState.value.isEnabled
            bassBoostEffect = bb
        } catch (_: Exception) {
            bassBoostEffect = null
        }

        try {
            val virt = Virtualizer(0, sessionId)
            virt.enabled = _equalizerState.value.isEnabled
            virtualizerEffect = virt
        } catch (_: Exception) {
            virtualizerEffect = null
        }

        try {
            val loud = LoudnessEnhancer(sessionId)
            loud.enabled = _equalizerState.value.isEnabled
            loudnessEnhancerEffect = loud
        } catch (_: Exception) {
            loudnessEnhancerEffect = null
        }

        _equalizerState.update { it.copy(hardwareAudioFxActive = hwActive) }
        syncEffectsWithState()
    }

    private fun syncEffectsWithState() {
        val state = _equalizerState.value
        try {
            equalizerEffect?.let { eq ->
                eq.enabled = state.isEnabled
                val numBands = eq.numberOfBands.toInt()
                val range = eq.bandLevelRange
                val minLevel = range[0]
                val maxLevel = range[1]
                for (b in 0 until minOf(numBands, state.bandGainsDb.size)) {
                    val gainDb = if (state.isEnabled) state.bandGainsDb[b] else 0f
                    val targetMillibels = (gainDb * 100).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
                    eq.setBandLevel(b.toShort(), targetMillibels)
                }
            }
        } catch (_: Exception) {
        }

        try {
            bassBoostEffect?.let { bb ->
                bb.enabled = state.isEnabled
                if (bb.strengthSupported) {
                    val strength = if (state.isEnabled) state.bassBoostStrength else 0
                    bb.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        } catch (_: Exception) {
        }

        try {
            virtualizerEffect?.let { virt ->
                virt.enabled = state.isEnabled
                if (virt.strengthSupported) {
                    val strength = if (state.isEnabled) state.virtualizerStrength else 0
                    virt.setStrength(strength.coerceIn(0, 1000).toShort())
                }
            }
        } catch (_: Exception) {
        }

        try {
            loudnessEnhancerEffect?.let { loud ->
                loud.enabled = state.isEnabled
                val gainMb = if (state.isEnabled) (state.loudnessGainDb * 100).toInt() else 0
                loud.setTargetGain(gainMb)
            }
        } catch (_: Exception) {
        }
    }

    fun setPlaying(shouldPlay: Boolean) {
        val current = _playbackState.value
        if (current.currentTrack == null) {
            if (shouldPlay) {
                val first = current.queue.firstOrNull() ?: return
                playTrack(first, current.queue)
            }
            return
        }
        if (current.isPlaying == shouldPlay) return
        togglePlayPause()
    }

    fun togglePlayPause() {
        val current = _playbackState.value
        if (current.currentTrack == null) {
            val first = current.queue.firstOrNull() ?: return
            playTrack(first, current.queue)
            return
        }

        val nextPlaying = !current.isPlaying
        try {
            mediaPlayer?.let { player ->
                if (nextPlaying) {
                    player.start()
                    applyPlaybackSpeedInternal(player, current.playbackSpeed)
                } else if (player.isPlaying) {
                    player.pause()
                }
            }
        } catch (_: Exception) {
        }

        _playbackState.update { it.copy(isPlaying = nextPlaying) }
        if (nextPlaying) {
            startProgressUpdater()
        } else {
            progressJob?.cancel()
            _playbackState.update { state -> state.copy(spectrumBars = List(18) { 0.10f }) }
        }
        MusicPlaybackService.startOrUpdateService(appContext)
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceIn(0L, _playbackState.value.durationMs.coerceAtLeast(1L))
        try {
            mediaPlayer?.seekTo(clamped.toInt())
        } catch (_: Exception) {
        }
        _playbackState.update { it.copy(currentPositionMs = clamped) }
        MusicPlaybackService.startOrUpdateService(appContext)
    }

    fun skipNext() {
        val state = _playbackState.value
        val queue = state.queue
        if (queue.isEmpty()) return
        val currentIdx = queue.indexOfFirst { it.id == state.currentTrack?.id }
        val nextTrack = when {
            state.isShuffleEnabled && queue.size > 1 -> {
                val candidates = queue.filter { it.id != state.currentTrack?.id }
                candidates[Random.nextInt(candidates.size)]
            }
            currentIdx in 0 until queue.lastIndex -> queue[currentIdx + 1]
            else -> queue.first()
        }
        playTrack(nextTrack, queue)
    }

    fun skipPrevious() {
        val state = _playbackState.value
        if (state.currentPositionMs > 4000L) {
            seekTo(0L)
            return
        }
        val queue = state.queue
        if (queue.isEmpty()) return
        val currentIdx = queue.indexOfFirst { it.id == state.currentTrack?.id }
        val prevTrack = if (currentIdx > 0) queue[currentIdx - 1] else queue.last()
        playTrack(prevTrack, queue)
    }

    private fun onTrackCompleted() {
        val state = _playbackState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                state.currentTrack?.let { playTrack(it, state.queue) }
            }
            RepeatMode.ALL -> {
                skipNext()
            }
            RepeatMode.OFF -> {
                val queue = state.queue
                val currentIdx = queue.indexOfFirst { it.id == state.currentTrack?.id }
                if (currentIdx in 0 until queue.lastIndex) {
                    playTrack(queue[currentIdx + 1], queue)
                } else {
                    _playbackState.update { it.copy(isPlaying = false, currentPositionMs = 0L) }
                    MusicPlaybackService.startOrUpdateService(appContext)
                }
            }
        }
    }

    fun toggleShuffle() {
        _playbackState.update { it.copy(isShuffleEnabled = !it.isShuffleEnabled) }
    }

    fun cycleRepeatMode() {
        _playbackState.update {
            val next = when (it.repeatMode) {
                RepeatMode.OFF -> RepeatMode.ALL
                RepeatMode.ALL -> RepeatMode.ONE
                RepeatMode.ONE -> RepeatMode.OFF
            }
            it.copy(repeatMode = next)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 2.0f)
        _playbackState.update { it.copy(playbackSpeed = clamped) }
        mediaPlayer?.let { player ->
            if (_playbackState.value.isPlaying) {
                applyPlaybackSpeedInternal(player, clamped)
            }
        }
        MusicPlaybackService.startOrUpdateService(appContext)
    }

    private fun applyPlaybackSpeedInternal(player: MediaPlayer, speed: Float) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                player.playbackParams = PlaybackParams().setSpeed(speed)
            }
        } catch (_: Exception) {
        }
    }

    fun setSleepTimerMinutes(minutes: Int?) {
        sleepTimerJob?.cancel()
        if (minutes == null || minutes <= 0) {
            _playbackState.update { it.copy(sleepTimerRemainingSeconds = null) }
            return
        }
        val totalSeconds = minutes * 60
        _playbackState.update { it.copy(sleepTimerRemainingSeconds = totalSeconds) }
        sleepTimerJob = scope.launch {
            var remaining = totalSeconds
            while (remaining > 0 && isActive) {
                delay(1000L)
                remaining--
                _playbackState.update { it.copy(sleepTimerRemainingSeconds = remaining) }
            }
            if (remaining <= 0) {
                if (_playbackState.value.isPlaying) {
                    togglePlayPause()
                }
                _playbackState.update { it.copy(sleepTimerRemainingSeconds = null) }
            }
        }
    }

    // Equalizer controls
    fun setEqualizerEnabled(enabled: Boolean) {
        _equalizerState.update { it.copy(isEnabled = enabled) }
        syncEffectsWithState()
    }

    fun updateBandGain(bandIndex: Int, gainDb: Float) {
        val clamped = gainDb.coerceIn(-15f, 15f)
        _equalizerState.update { state ->
            val updated = state.bandGainsDb.toMutableList()
            if (bandIndex in updated.indices) {
                updated[bandIndex] = clamped
            }
            state.copy(
                bandGainsDb = updated,
                activePresetId = -1L,
                activePresetName = "Пользовательский"
            )
        }
        syncEffectsWithState()
    }

    fun setBassBoostStrength(strength: Int) {
        _equalizerState.update {
            it.copy(
                bassBoostStrength = strength.coerceIn(0, 1000),
                activePresetId = -1L,
                activePresetName = "Пользовательский"
            )
        }
        syncEffectsWithState()
    }

    fun setVirtualizerStrength(strength: Int) {
        _equalizerState.update {
            it.copy(
                virtualizerStrength = strength.coerceIn(0, 1000),
                activePresetId = -1L,
                activePresetName = "Пользовательский"
            )
        }
        syncEffectsWithState()
    }

    fun setLoudnessGainDb(gainDb: Float) {
        _equalizerState.update {
            it.copy(
                loudnessGainDb = gainDb.coerceIn(0f, 10f)
            )
        }
        syncEffectsWithState()
    }

    fun applyPreset(preset: EqualizerPresetEntity) {
        _equalizerState.update {
            it.copy(
                activePresetId = preset.id,
                activePresetName = preset.name,
                bandGainsDb = preset.bandGainsList(),
                bassBoostStrength = preset.bassBoostStrength,
                virtualizerStrength = preset.virtualizerStrength,
                loudnessGainDb = preset.loudnessGainDb
            )
        }
        syncEffectsWithState()
    }

    fun resetEqualizerFlat() {
        _equalizerState.update {
            it.copy(
                activePresetId = 0L,
                activePresetName = "Студийный 0 dB",
                bandGainsDb = listOf(0f, 0f, 0f, 0f, 0f),
                bassBoostStrength = 0,
                virtualizerStrength = 0,
                loudnessGainDb = 0f
            )
        }
        syncEffectsWithState()
    }

    private fun startProgressUpdater() {
        progressJob?.cancel()
        progressJob = scope.launch {
            var tick = 0
            while (isActive && _playbackState.value.isPlaying) {
                val playerPos = try {
                    mediaPlayer?.takeIf { it.isPlaying }?.currentPosition?.toLong()
                } catch (_: Exception) {
                    null
                }
                val current = _playbackState.value
                val nextPos = playerPos ?: (current.currentPositionMs + (120 * current.playbackSpeed).toLong())
                val duration = current.durationMs.coerceAtLeast(1000L)

                if (playerPos == null && nextPos >= duration) {
                    onTrackCompleted()
                    break
                }

                val eq = _equalizerState.value
                val bars = List(18) { idx ->
                    val bandIdx = (idx * 5) / 18
                    val eqGainFactor = if (eq.isEnabled) {
                        1f + (eq.bandGainsDb.getOrElse(bandIdx) { 0f } / 22f)
                    } else 1f
                    val bassFactor = if (idx < 4 && eq.isEnabled) {
                        1f + (eq.bassBoostStrength / 2000f)
                    } else 1f
                    val wave = 0.42f + 0.32f * sin(tick * 0.35f + idx * 0.65f) +
                        0.18f * cos(tick * 0.22f - idx * 0.4f)
                    (wave * eqGainFactor * bassFactor).coerceIn(0.10f, 0.98f)
                }

                _playbackState.update {
                    it.copy(
                        currentPositionMs = nextPos.coerceAtMost(duration),
                        spectrumBars = bars
                    )
                }
                tick++
                delay(120L)
            }
        }
    }

    fun updateQueueTracks(updatedAllTracks: List<TrackEntity>) {
        _playbackState.update { state ->
            val refreshedQueue = if (state.queue.isEmpty()) {
                updatedAllTracks
            } else {
                state.queue.mapNotNull { qTrack ->
                    updatedAllTracks.find { it.id == qTrack.id }
                }.ifEmpty { updatedAllTracks }
            }
            val refreshedCurrent = state.currentTrack?.let { cur ->
                updatedAllTracks.find { it.id == cur.id }
            }
            state.copy(queue = refreshedQueue, currentTrack = refreshedCurrent)
        }
    }

    private fun releaseAudioEffects() {
        try {
            equalizerEffect?.release()
        } catch (_: Exception) {
        }
        try {
            bassBoostEffect?.release()
        } catch (_: Exception) {
        }
        try {
            virtualizerEffect?.release()
        } catch (_: Exception) {
        }
        try {
            loudnessEnhancerEffect?.release()
        } catch (_: Exception) {
        }
        equalizerEffect = null
        bassBoostEffect = null
        virtualizerEffect = null
        loudnessEnhancerEffect = null
    }

    private fun releasePlayerOnly() {
        releaseAudioEffects()
        try {
            mediaPlayer?.stop()
        } catch (_: Exception) {
        }
        try {
            mediaPlayer?.release()
        } catch (_: Exception) {
        }
        mediaPlayer = null
    }

    companion object {
        @Volatile
        private var INSTANCE: AudioPlaybackController? = null

        fun getInstance(context: Context): AudioPlaybackController {
            return INSTANCE ?: synchronized(this) {
                val instance = AudioPlaybackController(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
