package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioPlaybackController
import com.example.audio.EqualizerState
import com.example.audio.PlaybackState
import com.example.data.local.AuraWaveDatabase
import com.example.data.local.EqualizerPresetEntity
import com.example.data.local.PlaylistWithTracks
import com.example.data.local.TrackEntity
import com.example.data.repository.MusicRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination {
    LIBRARY, PLAYLISTS, EQUALIZER
}

data class LibraryFilterState(
    val searchQuery: String = "",
    val selectedGenre: String = "Все",
    val onlyFavorites: Boolean = false,
    val statusBannerMessage: String? = null
)

data class MusicUiState(
    val allTracks: List<TrackEntity> = emptyList(),
    val filteredTracks: List<TrackEntity> = emptyList(),
    val availableGenres: List<String> = listOf("Все"),
    val playlists: List<PlaylistWithTracks> = emptyList(),
    val equalizerPresets: List<EqualizerPresetEntity> = emptyList(),
    val filter: LibraryFilterState = LibraryFilterState()
)

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AuraWaveDatabase.getInstance(application)
    private val repository = MusicRepository(application, database.musicDao())
    private val playbackController = AudioPlaybackController.getInstance(application)

    private val _filterState = MutableStateFlow(LibraryFilterState())
    val filterState: StateFlow<LibraryFilterState> = _filterState.asStateFlow()

    private val _currentDestination = MutableStateFlow(AppDestination.LIBRARY)
    val currentDestination: StateFlow<AppDestination> = _currentDestination.asStateFlow()

    private val _selectedPlaylistId = MutableStateFlow<Long?>(null)
    val selectedPlaylistId: StateFlow<Long?> = _selectedPlaylistId.asStateFlow()

    private val _isNowPlayingSheetOpen = MutableStateFlow(false)
    val isNowPlayingSheetOpen: StateFlow<Boolean> = _isNowPlayingSheetOpen.asStateFlow()

    val playbackState: StateFlow<PlaybackState> = playbackController.playbackState
    val equalizerState: StateFlow<EqualizerState> = playbackController.equalizerState

    val uiState: StateFlow<MusicUiState> = combine(
        repository.allTracks,
        repository.playlistsWithTracks,
        repository.equalizerPresets,
        _filterState
    ) { tracks, playlists, presets, filter ->
        playbackController.updateQueueTracks(tracks)

        val genres = listOf("Все") + tracks.map { it.genre }.filter { it.isNotBlank() }.distinct()
        val filtered = tracks.filter { track ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                track.title.contains(filter.searchQuery, ignoreCase = true) ||
                track.artist.contains(filter.searchQuery, ignoreCase = true) ||
                track.album.contains(filter.searchQuery, ignoreCase = true)
            val matchesGenre = filter.selectedGenre == "Все" || track.genre.equals(filter.selectedGenre, ignoreCase = true)
            val matchesFav = !filter.onlyFavorites || track.isFavorite
            matchesQuery && matchesGenre && matchesFav
        }

        MusicUiState(
            allTracks = tracks,
            filteredTracks = filtered,
            availableGenres = genres,
            playlists = playlists,
            equalizerPresets = presets,
            filter = filter
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MusicUiState()
    )

    init {
        playbackController.setOnTrackPlayedListener { trackId ->
            viewModelScope.launch {
                repository.incrementPlayCount(trackId)
            }
        }
        viewModelScope.launch {
            repository.ensureEqualizerPresetsSeeded()
            repository.scanDeviceMusic()
        }
    }

    fun navigateTo(destination: AppDestination) {
        _currentDestination.value = destination
        if (destination != AppDestination.PLAYLISTS) {
            _selectedPlaylistId.value = null
        }
    }

    fun selectPlaylist(playlistId: Long?) {
        _selectedPlaylistId.value = playlistId
    }

    fun setNowPlayingSheetOpen(open: Boolean) {
        _isNowPlayingSheetOpen.value = open
    }

    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun selectGenre(genre: String) {
        _filterState.update { it.copy(selectedGenre = genre) }
    }

    fun toggleFavoritesFilter() {
        _filterState.update { it.copy(onlyFavorites = !it.onlyFavorites) }
    }

    fun clearBannerMessage() {
        _filterState.update { it.copy(statusBannerMessage = null) }
    }

    fun scanDeviceMusicLibrary() {
        viewModelScope.launch {
            val count = repository.scanDeviceMusic()
            val message = if (count > 0) {
                "Найдено аудиофайлов на устройстве: $count"
            } else {
                "Аудиофайлы в стандартных папках не найдены. Используйте «Добавить файлы»."
            }
            _filterState.update { it.copy(statusBannerMessage = message) }
        }
    }

    // Playback actions
    fun playTrack(track: TrackEntity, customQueue: List<TrackEntity>? = null) {
        val queueToUse = customQueue ?: uiState.value.filteredTracks.ifEmpty { uiState.value.allTracks }
        playbackController.playTrack(track, queueToUse)
    }

    fun togglePlayPause() = playbackController.togglePlayPause()
    fun skipNext() = playbackController.skipNext()
    fun skipPrevious() = playbackController.skipPrevious()
    fun seekTo(positionMs: Long) = playbackController.seekTo(positionMs)
    fun toggleShuffle() = playbackController.toggleShuffle()
    fun cycleRepeatMode() = playbackController.cycleRepeatMode()
    fun setPlaybackSpeed(speed: Float) = playbackController.setPlaybackSpeed(speed)
    fun setSleepTimerMinutes(minutes: Int?) {
        playbackController.setSleepTimerMinutes(minutes)
        val msg = if (minutes != null) "Таймер сна установлен на $minutes мин." else "Таймер сна отключен"
        _filterState.update { it.copy(statusBannerMessage = msg) }
    }

    // Track actions
    fun toggleFavorite(track: TrackEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(track)
        }
    }

    fun importLocalAudioUris(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            var lastImported: TrackEntity? = null
            var importedCount = 0
            uris.forEach { uri ->
                val track = repository.importAudioUri(uri)
                if (track != null) {
                    lastImported = track
                    importedCount++
                }
            }
            if (importedCount > 0 && lastImported != null) {
                _filterState.update {
                    it.copy(
                        statusBannerMessage = if (importedCount == 1) {
                            "Добавлен трек: «${lastImported!!.title}»"
                        } else {
                            "Добавлено треков с устройства: $importedCount"
                        }
                    )
                }
                playbackController.playTrack(lastImported!!, uiState.value.allTracks + listOfNotNull(lastImported))
            } else {
                _filterState.update {
                    it.copy(statusBannerMessage = "Не удалось открыть выбранный аудиофайл")
                }
            }
        }
    }

    // Playlist actions
    fun createPlaylist(name: String, description: String, paletteIndex: Int) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = repository.createPlaylist(name, description, paletteIndex)
            _filterState.update {
                it.copy(statusBannerMessage = "Создан плейлист «${name.trim()}»")
            }
            _selectedPlaylistId.value = id
        }
    }

    fun deletePlaylist(playlistId: Long, name: String) {
        viewModelScope.launch {
            if (_selectedPlaylistId.value == playlistId) {
                _selectedPlaylistId.value = null
            }
            repository.deletePlaylist(playlistId)
            _filterState.update {
                it.copy(statusBannerMessage = "Плейлист «$name» удален")
            }
        }
    }

    fun addTrackToPlaylist(playlistId: Long, track: TrackEntity, playlistName: String) {
        viewModelScope.launch {
            repository.addTrackToPlaylist(playlistId, track.id)
            _filterState.update {
                it.copy(statusBannerMessage = "«${track.title}» добавлен в плейлист «$playlistName»")
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: Long, trackId: Long) {
        viewModelScope.launch {
            repository.removeTrackFromPlaylist(playlistId, trackId)
        }
    }

    // Equalizer actions
    fun setEqualizerEnabled(enabled: Boolean) = playbackController.setEqualizerEnabled(enabled)
    fun updateEqualizerBand(bandIndex: Int, gainDb: Float) = playbackController.updateBandGain(bandIndex, gainDb)
    fun setBassBoostStrength(strength: Int) = playbackController.setBassBoostStrength(strength)
    fun setVirtualizerStrength(strength: Int) = playbackController.setVirtualizerStrength(strength)
    fun setLoudnessGainDb(gainDb: Float) = playbackController.setLoudnessGainDb(gainDb)
    fun applyEqualizerPreset(preset: EqualizerPresetEntity) = playbackController.applyPreset(preset)
    fun resetEqualizerFlat() = playbackController.resetEqualizerFlat()

    fun saveCustomEqualizerPreset(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val eq = equalizerState.value
            val saved = repository.saveCustomEqualizerPreset(
                name = name,
                bandGains = eq.bandGainsDb,
                bassBoost = eq.bassBoostStrength,
                virtualizer = eq.virtualizerStrength,
                loudnessDb = eq.loudnessGainDb
            )
            playbackController.applyPreset(saved)
            _filterState.update {
                it.copy(statusBannerMessage = "Сохранен пресет эквалайзера «${saved.name}»")
            }
        }
    }

    fun deleteCustomPreset(preset: EqualizerPresetEntity) {
        viewModelScope.launch {
            repository.deleteCustomPreset(preset.id)
            _filterState.update {
                it.copy(statusBannerMessage = "Пресет «${preset.name}» удален")
            }
        }
    }
}
