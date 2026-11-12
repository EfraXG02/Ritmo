package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MusicRepository
import com.example.model.ArtistInfo
import com.example.model.ArtistShuffleHistory
import com.example.model.ArtistShuffleVibe
import com.example.model.PlayerState
import com.example.model.RepeatMode
import com.example.model.Track
import com.example.player.MusicPlayerEngine
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

enum class NavTab {
    HOME, ARTIST_SHUFFLE, LIBRARY, SEARCH
}

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val playerEngine = MusicPlayerEngine(application)

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private val _currentTab = MutableStateFlow(NavTab.HOME)
    val currentTab: StateFlow<NavTab> = _currentTab.asStateFlow()

    private val _isFullPlayerVisible = MutableStateFlow(false)
    val isFullPlayerVisible: StateFlow<Boolean> = _isFullPlayerVisible.asStateFlow()

    private val _showSleepTimerDialog = MutableStateFlow(false)
    val showSleepTimerDialog: StateFlow<Boolean> = _showSleepTimerDialog.asStateFlow()

    private val _showQueueSheet = MutableStateFlow(false)
    val showQueueSheet: StateFlow<Boolean> = _showQueueSheet.asStateFlow()

    private val _isGeneratingShuffle = MutableStateFlow(false)
    val isGeneratingShuffle: StateFlow<Boolean> = _isGeneratingShuffle.asStateFlow()

    private val _artistShuffleHistory = MutableStateFlow<List<ArtistShuffleHistory>>(emptyList())
    val artistShuffleHistory: StateFlow<List<ArtistShuffleHistory>> = _artistShuffleHistory.asStateFlow()

    private val _allTracks = MutableStateFlow<List<Track>>(emptyList())
    val allTracks: StateFlow<List<Track>> = _allTracks.asStateFlow()

    private val _favoriteTracks = MutableStateFlow<List<Track>>(emptyList())
    val favoriteTracks: StateFlow<List<Track>> = _favoriteTracks.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearchingOnline = MutableStateFlow(false)
    val isSearchingOnline: StateFlow<Boolean> = _isSearchingOnline.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val featuredArtists: List<ArtistInfo> = repository.featuredArtists

    private var searchJob: Job? = null

    init {
        loadData()
        observeEngine()
    }

    private fun loadData() {
        val tracks = repository.getAllBuiltInTracks()
        _allTracks.value = tracks
        _favoriteTracks.value = tracks.filter { it.isFavorite }
        _artistShuffleHistory.value = repository.getArtistShuffleHistory()
    }

    private fun observeEngine() {
        viewModelScope.launch {
            playerEngine.isPlaying.collect { playing ->
                _playerState.value = _playerState.value.copy(isPlaying = playing)
            }
        }
        viewModelScope.launch {
            playerEngine.currentPosition.collect { pos ->
                _playerState.value = _playerState.value.copy(currentPositionMs = pos)
            }
        }
        viewModelScope.launch {
            playerEngine.duration.collect { dur ->
                _playerState.value = _playerState.value.copy(durationMs = dur)
            }
        }
        viewModelScope.launch {
            playerEngine.visualizerLevels.collect { bars ->
                _playerState.value = _playerState.value.copy(visualizerLevels = bars)
            }
        }
        viewModelScope.launch {
            playerEngine.sleepMinutesLeft.collect { mins ->
                _playerState.value = _playerState.value.copy(sleepTimerMinutesLeft = mins)
            }
        }
        viewModelScope.launch {
            playerEngine.isBuffering.collect { buffering ->
                _playerState.value = _playerState.value.copy(isBuffering = buffering)
            }
        }
    }

    fun selectTab(tab: NavTab) {
        _currentTab.value = tab
    }

    fun setFullPlayerVisible(visible: Boolean) {
        _isFullPlayerVisible.value = visible
    }

    fun setShowSleepTimerDialog(show: Boolean) {
        _showSleepTimerDialog.value = show
    }

    fun setShowQueueSheet(show: Boolean) {
        _showQueueSheet.value = show
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    /**
     * Primary feature requested: Play random songs according to singer name without manual playlist creation.
     */
    fun startArtistShuffle(artistName: String, vibe: ArtistShuffleVibe = ArtistShuffleVibe.ALL) {
        val trimmed = artistName.trim()
        if (trimmed.isEmpty()) return

        viewModelScope.launch {
            _isGeneratingShuffle.value = true
            showToast("Embaralhando faixas de $trimmed...")

            val shuffledQueue = repository.createRandomArtistQueue(trimmed, vibe)

            _isGeneratingShuffle.value = false

            if (shuffledQueue.isNotEmpty()) {
                val firstTrack = shuffledQueue[0]
                _playerState.value = _playerState.value.copy(
                    queue = shuffledQueue,
                    queueIndex = 0,
                    currentTrack = firstTrack,
                    currentArtistShuffleName = trimmed,
                    currentVibe = vibe,
                    isShuffleEnabled = true
                )

                playerEngine.playTrack(firstTrack) {
                    onTrackFinished()
                }

                _artistShuffleHistory.value = repository.getArtistShuffleHistory()
                showToast("Tocando modo aleatório de $trimmed! (${shuffledQueue.size} músicas)")
            } else {
                showToast("Nenhuma música encontrada para $trimmed.")
            }
        }
    }

    fun playTrack(track: Track, customQueue: List<Track>? = null) {
        val q = customQueue ?: _playerState.value.queue.ifEmpty { _allTracks.value }
        val idx = q.indexOfFirst { it.id == track.id }.coerceAtLeast(0)

        _playerState.value = _playerState.value.copy(
            queue = q,
            queueIndex = idx,
            currentTrack = track
        )

        playerEngine.playTrack(track) {
            onTrackFinished()
        }
    }

    fun togglePlayPause() {
        if (_playerState.value.currentTrack == null && _allTracks.value.isNotEmpty()) {
            val randomTrack = _allTracks.value.random()
            playTrack(randomTrack, _allTracks.value)
        } else {
            playerEngine.togglePlayPause()
        }
    }

    fun skipNext() {
        val state = _playerState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        val nextIndex = if (state.isShuffleEnabled && queue.size > 1) {
            var r = (0 until queue.size).random()
            while (r == state.queueIndex) {
                r = (0 until queue.size).random()
            }
            r
        } else {
            (state.queueIndex + 1) % queue.size
        }

        val nextTrack = queue[nextIndex]
        _playerState.value = state.copy(
            queueIndex = nextIndex,
            currentTrack = nextTrack
        )
        playerEngine.playTrack(nextTrack) {
            onTrackFinished()
        }
    }

    fun skipPrevious() {
        val state = _playerState.value
        val queue = state.queue
        if (queue.isEmpty()) return

        // If played more than 3 seconds, restart current track
        if (state.currentPositionMs > 3000L) {
            playerEngine.seekTo(0L)
            return
        }

        val prevIndex = if (state.queueIndex > 0) state.queueIndex - 1 else queue.size - 1
        val prevTrack = queue[prevIndex]
        _playerState.value = state.copy(
            queueIndex = prevIndex,
            currentTrack = prevTrack
        )
        playerEngine.playTrack(prevTrack) {
            onTrackFinished()
        }
    }

    private fun onTrackFinished() {
        val state = _playerState.value
        when (state.repeatMode) {
            RepeatMode.ONE -> {
                state.currentTrack?.let {
                    playerEngine.playTrack(it) { onTrackFinished() }
                }
            }
            RepeatMode.ALL -> skipNext()
            RepeatMode.OFF -> {
                if (state.queueIndex < state.queue.size - 1) {
                    skipNext()
                } else {
                    playerEngine.pause()
                }
            }
        }
    }

    fun seekTo(positionMs: Long) {
        playerEngine.seekTo(positionMs)
    }

    fun toggleShuffle() {
        _playerState.value = _playerState.value.copy(
            isShuffleEnabled = !_playerState.value.isShuffleEnabled
        )
    }

    fun toggleRepeat() {
        val nextMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        _playerState.value = _playerState.value.copy(repeatMode = nextMode)
    }

    fun setSpeed(speed: Float) {
        playerEngine.playbackSpeed = speed
        _playerState.value = _playerState.value.copy(speed = speed)
    }

    fun setSleepTimer(minutes: Int) {
        playerEngine.setSleepTimer(minutes)
        _showSleepTimerDialog.value = false
        if (minutes > 0) {
            showToast("Timer configurado para $minutes minutos.")
        } else {
            showToast("Timer cancelado.")
        }
    }

    fun toggleFavorite(track: Track) {
        val isFav = repository.toggleFavorite(track.id)
        val updatedTrack = track.copy(isFavorite = isFav)

        _allTracks.value = _allTracks.value.map { if (it.id == track.id) updatedTrack else it }
        _searchResults.value = _searchResults.value.map { if (it.id == track.id) updatedTrack else it }
        _favoriteTracks.value = _allTracks.value.filter { it.isFavorite }

        if (_playerState.value.currentTrack?.id == track.id) {
            _playerState.value = _playerState.value.copy(currentTrack = updatedTrack)
        }

        showToast(if (isFav) "Adicionada aos favoritos ❤️" else "Removida dos favoritos")
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        searchJob?.cancel()

        if (query.trim().isEmpty()) {
            _searchResults.value = emptyList()
            _isSearchingOnline.value = false
            return
        }

        searchJob = viewModelScope.launch {
            // First show instant local / catalog matches
            val localMatches = _allTracks.value.filter {
                it.title.contains(query, ignoreCase = true) ||
                        it.artist.contains(query, ignoreCase = true) ||
                        it.album.contains(query, ignoreCase = true)
            }
            _searchResults.value = localMatches

            // Then query online if query has >= 3 chars
            if (query.length >= 3) {
                _isSearchingOnline.value = true
                delay(400) // debounce
                val onlineResults = repository.createRandomArtistQueue(query, ArtistShuffleVibe.ALL)
                _isSearchingOnline.value = false
                val merged = (localMatches + onlineResults).distinctBy { "${it.title}_${it.artist}" }
                _searchResults.value = merged
            }
        }
    }

    fun scanLocalMedia(artistNameFilter: String = "") {
        viewModelScope.launch {
            val locals = repository.queryLocalTracksByArtist(artistNameFilter)
            if (locals.isNotEmpty()) {
                val combined = (_allTracks.value + locals).distinctBy { it.id }
                _allTracks.value = combined
                showToast("${locals.size} faixas locais carregadas!")
            } else {
                showToast("Nenhuma música local encontrada ou permissão necessária.")
            }
        }
    }

    private fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    override fun onCleared() {
        super.onCleared()
        playerEngine.release()
    }
}
