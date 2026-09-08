package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.VideoPlayerApp
import com.example.data.local.entity.AllowedFolderEntity
import com.example.data.local.entity.ExcludedFolderEntity
import com.example.data.local.entity.PlayerSettingsEntity
import com.example.data.model.FolderItem
import com.example.data.model.VideoItem
import com.example.data.model.VideoSortOrder
import com.example.player.EqualizerBand
import com.example.player.PlayerManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as VideoPlayerApp
    private val repository = app.repository
    val playerManager: PlayerManager = app.playerManager

    private val _rawVideos = MutableStateFlow<List<VideoItem>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _sortOrder = MutableStateFlow(VideoSortOrder.DATE_DESC)
    val sortOrder: StateFlow<VideoSortOrder> = _sortOrder.asStateFlow()

    private val _selectedFolder = MutableStateFlow<FolderItem?>(null)
    val selectedFolder: StateFlow<FolderItem?> = _selectedFolder.asStateFlow()

    private val _settings = MutableStateFlow(PlayerSettingsEntity())
    val settings: StateFlow<PlayerSettingsEntity> = _settings.asStateFlow()

    val excludedFolders: StateFlow<List<ExcludedFolderEntity>> = repository.excludedFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allowedFolders: StateFlow<List<AllowedFolderEntity>> = repository.allowedFoldersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _hiddenCount = MutableStateFlow(0)
    val hiddenCount: StateFlow<Int> = _hiddenCount.asStateFlow()

    // Filtered & Sorted Videos
    val videos: StateFlow<List<VideoItem>> = combine(
        _rawVideos,
        _searchQuery,
        _sortOrder,
        _selectedFolder
    ) { raw, query, sort, folder ->
        var list = raw
        if (folder != null) {
            list = list.filter {
                it.folderPath.equals(folder.path, ignoreCase = true) ||
                        it.bucketDisplayName.equals(folder.name, ignoreCase = true)
            }
        }
        if (query.isNotBlank()) {
            list = list.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.displayName.contains(query, ignoreCase = true)
            }
        }
        repository.sortVideos(list, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Folders
    private val _folders = MutableStateFlow<List<FolderItem>>(emptyList())
    val folders: StateFlow<List<FolderItem>> = combine(
        _folders,
        _sortOrder
    ) { folderList, sort ->
        repository.sortFolders(folderList, sort)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Favorite Folders (for quick access on main screen)
    val favoriteFolders: StateFlow<List<FolderItem>> = folders
        .map { it.filter { f -> f.isFavorite } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadSettingsAndScan()
        observeHiddenCount()
    }

    private fun loadSettingsAndScan() {
        viewModelScope.launch {
            val initialSettings = repository.getSettings()
            _settings.value = initialSettings
            val savedSort = try {
                VideoSortOrder.valueOf(initialSettings.folderSortOrder)
            } catch (e: Exception) {
                VideoSortOrder.DATE_DESC
            }
            _sortOrder.value = savedSort
            playerManager.setConcurrentPlayback(initialSettings.ignoreAudioFocus)
            playerManager.setBackgroundMode(initialSettings.backgroundPlaybackEnabled)
            playerManager.setVideoVolume(initialSettings.videoVolume)
            playerManager.setEqualizerEnabled(initialSettings.equalizerEnabled)
            playerManager.setBassStrength(initialSettings.bassStrength)
            playerManager.setTrebleStrength(initialSettings.trebleStrength)
            if (initialSettings.eqPreset != "Normal") {
                playerManager.applyPreset(initialSettings.eqPreset)
            }

            refreshVideos()
        }
    }

    private fun observeHiddenCount() {
        viewModelScope.launch {
            repository.hiddenVideosFlow.collect { list ->
                _hiddenCount.value = list.size
            }
        }
    }

    fun refreshVideos() {
        viewModelScope.launch {
            _isLoading.value = true
            val scanned = repository.scanVideos()
            _rawVideos.value = scanned
            val folderItems = repository.getFolders(scanned)
            _folders.value = folderItems
            _isLoading.value = false
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSortOrder(order: VideoSortOrder) {
        _sortOrder.value = order
        viewModelScope.launch {
            val current = _settings.value.copy(folderSortOrder = order.name)
            _settings.value = current
            repository.updateSettings(current)
        }
    }

    fun selectFolder(folder: FolderItem?) {
        _selectedFolder.value = folder
    }

    fun toggleFavoriteFolder(folder: FolderItem) {
        viewModelScope.launch {
            repository.toggleFavoriteFolder(folder.path, folder.name)
            refreshVideos()
        }
    }

    fun excludeFolder(folder: FolderItem) {
        viewModelScope.launch {
            repository.excludeFolder(folder.path, folder.name)
            if (_selectedFolder.value?.path == folder.path) {
                _selectedFolder.value = null
            }
            refreshVideos()
        }
    }

    fun removeExcludedFolder(folderPath: String) {
        viewModelScope.launch {
            repository.removeExcludedFolder(folderPath)
            refreshVideos()
        }
    }

    // Hide video from list (does not delete original file from storage)
    fun hideVideoFromList(video: VideoItem) {
        viewModelScope.launch {
            repository.hideVideoFromList(video)
            // If current playing video is this one, stop
            if (playerManager.currentVideo.value?.id == video.id) {
                playerManager.stopAndClear()
            }
            refreshVideos()
        }
    }

    fun restoreAllHiddenVideos() {
        viewModelScope.launch {
            repository.restoreAllHiddenVideos()
            refreshVideos()
        }
    }

    fun toggleDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(isDarkMode = isDark)
            _settings.value = updated
            repository.updateSettings(updated)
        }
    }

    fun toggleConcurrentPlayback(enabled: Boolean) {
        playerManager.setConcurrentPlayback(enabled)
        viewModelScope.launch {
            val updated = _settings.value.copy(ignoreAudioFocus = enabled)
            _settings.value = updated
            repository.updateSettings(updated)
        }
    }

    fun toggleBackgroundPlayback(enabled: Boolean) {
        playerManager.setBackgroundMode(enabled)
        viewModelScope.launch {
            val updated = _settings.value.copy(backgroundPlaybackEnabled = enabled)
            _settings.value = updated
            repository.updateSettings(updated)
        }
    }

    fun setVideoVolume(volume: Float) {
        playerManager.setVideoVolume(volume)
        viewModelScope.launch {
            val updated = _settings.value.copy(videoVolume = volume)
            _settings.value = updated
            repository.updateSettings(updated)
        }
    }

    fun toggleEqualizer(enabled: Boolean) {
        playerManager.setEqualizerEnabled(enabled)
        viewModelScope.launch {
            val updated = _settings.value.copy(equalizerEnabled = enabled)
            _settings.value = updated
            repository.updateSettings(updated)
        }
    }

    fun setBassStrength(strength: Int) {
        playerManager.setBassStrength(strength)
        viewModelScope.launch {
            val updated = _settings.value.copy(bassStrength = strength)
            _settings.value = updated
            repository.updateSettings(updated)
        }
    }

    fun setTrebleStrength(strength: Int) {
        playerManager.setTrebleStrength(strength)
        viewModelScope.launch {
            val updated = _settings.value.copy(trebleStrength = strength)
            _settings.value = updated
            repository.updateSettings(updated)
        }
    }

    fun setBandLevel(band: EqualizerBand, level: Short) {
        playerManager.setBandLevel(band.index, level)
    }

    fun applyEqPreset(preset: String) {
        playerManager.applyPreset(preset)
        viewModelScope.launch {
            val updated = _settings.value.copy(eqPreset = preset)
            _settings.value = updated
            repository.updateSettings(updated)
        }
    }

    fun toggleWhitelistMode(enabled: Boolean) {
        viewModelScope.launch {
            val updated = _settings.value.copy(whitelistModeEnabled = enabled)
            _settings.value = updated
            repository.updateSettings(updated)
            refreshVideos()
        }
    }

    fun saveAllowedFolders(folders: List<Pair<String, String>>) {
        viewModelScope.launch {
            repository.setAllowedFolders(folders)
            refreshVideos()
        }
    }

    fun playVideo(video: VideoItem, playlist: List<VideoItem> = listOf(video)) {
        playerManager.playVideo(video, playlist)
    }

    fun playAllInFolder(folder: FolderItem) {
        val folderVideos = _rawVideos.value.filter {
            it.folderPath.equals(folder.path, ignoreCase = true) ||
                    it.bucketDisplayName.equals(folder.name, ignoreCase = true)
        }
        if (folderVideos.isNotEmpty()) {
            playerManager.playVideo(folderVideos.first(), folderVideos)
        }
    }
}
