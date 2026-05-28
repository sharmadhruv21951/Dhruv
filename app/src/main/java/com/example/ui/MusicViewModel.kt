package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MusicViewModel(application: Application) : AndroidViewModel(application) {

    private val database = MusicDatabase.getDatabase(application)
    private val repository = MusicRepository(database.musicDao())
    private val playerManager = AudioPlayerManager(application)
    private val geminiService = GeminiRecommendationService()

    // UI Tab state: 0 = Home, 1 = Search, 2 = Library, 3 = Group Sync
    private val _currentTab = MutableStateFlow(0)
    val currentTab = _currentTab.asStateFlow()

    // Songs Catalog Flows
    val allSongs = repository.allSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val favoriteSongs = repository.favoriteSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val downloadedSongs = repository.downloadedSongs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val playlists = repository.allPlaylists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently viewed playlist (inside detail view)
    private val _activePlaylistId = MutableStateFlow<Long?>(null)
    val activePlaylistId = _activePlaylistId.asStateFlow()

    val activePlaylistWithSongs = _activePlaylistId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getPlaylistWithSongs(id)
            } else {
                flowOf(null)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Player Flows
    val currentSong = playerManager.currentSong
    val isPlaying = playerManager.isPlaying
    val currentPosition = playerManager.currentPosition
    val duration = playerManager.duration
    val isLoading = playerManager.isLoading
    val volumeBoost = playerManager.volumeBoost

    fun setVolumeBoost(multiplier: Float) {
        playerManager.setVolumeBoost(multiplier)
    }

    // Search query and matching list
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    val searchResults = _searchQuery
        .debounce(300)
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                flowOf(allSongs.value)
            } else {
                repository.searchSongs(query)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Recommendation Engine States
    private val _recommendationMood = MutableStateFlow("Lofi Beats")
    val recommendationMood = _recommendationMood.asStateFlow()

    private val _recommendedSongs = MutableStateFlow<List<Song>>(emptyList())
    val recommendedSongs = _recommendedSongs.asStateFlow()

    private val _isRecommendationLoading = MutableStateFlow(false)
    val isRecommendationLoading = _isRecommendationLoading.asStateFlow()

    // Offline Filter Only State (filters library only to downloaded)
    private val _isOfflineModeActive = MutableStateFlow(false)
    val isOfflineModeActive = _isOfflineModeActive.asStateFlow()

    // Share Card overlay state
    private val _sharedSong = MutableStateFlow<Song?>(null)
    val sharedSong = _sharedSong.asStateFlow()

    // Real-Time friends list simulation
    private val _groupSessionCode = MutableStateFlow<String?>(null)
    val groupSessionCode = _groupSessionCode.asStateFlow()

    private val _floatingEmojis = MutableStateFlow<List<String>>(emptyList())
    val floatingEmojis = _floatingEmojis.asStateFlow()

    // Simulated network users in party
    data class FriendSession(
        val name: String,
        val avatarUrl: String,
        val currentSongTitle: String,
        val currentSongArtist: String,
        val elapsedMs: Long,
        val maxDurationMs: Long,
        val isPlaying: Boolean,
        val matchedSongId: String
    )

    private val _friendSessions = MutableStateFlow<List<FriendSession>>(emptyList())
    val friendSessions = _friendSessions.asStateFlow()

    init {
        // Safe database seeding on background dispatcher
        viewModelScope.launch(Dispatchers.IO) {
            allSongs.first().let { songs ->
                if (songs.isEmpty()) {
                    val initialSongs = MusicDatabase.getInitialCatalog()
                    repository.insertSongs(initialSongs)
                    
                    // Create a default playlist to assist first run experience
                    val defaultPlaylistId = repository.createPlaylist(
                        "My Favorites Mix",
                        "Sleek, customizable mix to stream or download offline"
                    )
                    // Add first three songs to the playlist
                    repository.addSongToPlaylist(defaultPlaylistId, "broke_night_owl")
                    repository.addSongToPlaylist(defaultPlaylistId, "lakey_warm_nights")
                }
            }
            // Load initial recommendations on launch now that database is populated
            triggerRecommendations("Lofi Beats")
        }

        // Kickoff a background ticker to simulate other friends dynamic progress in their listening party
        startFriendSessionSimulation()
    }

    fun selectTab(tab: Int) {
        _currentTab.value = tab
    }

    fun setAndPlaySong(song: Song, queue: List<Song>) {
        val index = queue.indexOfFirst { it.id == song.id }
        if (index != -1) {
            playerManager.playSongFromQueue(queue, index)
        } else {
            playerManager.playSong(song)
        }
    }

    fun togglePlayPause() {
        playerManager.togglePlayPause()
    }

    fun seekTo(positionMs: Long) {
        playerManager.seekTo(positionMs)
    }

    fun playNext() {
        playerManager.playNext()
    }

    fun playPrevious() {
        playerManager.playPrevious()
    }

    // Toggle heart favorite
    fun toggleFavorite(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleFavorite(song.id, song.isFavorite)
        }
    }

    // Download flow with simulated progress indicator
    private val _downloadingSongsProgress = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadingSongsProgress = _downloadingSongsProgress.asStateFlow()

    fun triggerSongDownload(song: Song) {
        viewModelScope.launch {
            if (song.isDownloaded) {
                // Delete download
                repository.updateDownloadStatus(song.id, false)
                return@launch
            }

            // Simulate robust downloading progress
            val initialMap = _downloadingSongsProgress.value.toMutableMap()
            initialMap[song.id] = 0.05f
            _downloadingSongsProgress.value = initialMap

            for (p in 1..20) {
                delay(120) // chunk wait
                val progressMap = _downloadingSongsProgress.value.toMutableMap()
                progressMap[song.id] = (p / 20.0f)
                _downloadingSongsProgress.value = progressMap
            }

            // Write downloaded flag to DB
            repository.updateDownloadStatus(song.id, true)

            // Clear progress values
            val finalMap = _downloadingSongsProgress.value.toMutableMap()
            finalMap.remove(song.id)
            _downloadingSongsProgress.value = finalMap
        }
    }

    // Toggle global Offline screen mode filter
    fun toggleOfflineMode(active: Boolean) {
        _isOfflineModeActive.value = active
    }

    // Library - Create playlist
    fun createPlaylist(name: String, description: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            repository.createPlaylist(name, description)
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deletePlaylist(playlistId)
            if (_activePlaylistId.value == playlistId) {
                _activePlaylistId.value = null
            }
        }
    }

    fun selectPlaylist(playlistId: Long?) {
        _activePlaylistId.value = playlistId
    }

    fun addSongToPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addSongToPlaylist(playlistId, songId)
        }
    }

    fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.removeSongFromPlaylist(playlistId, songId)
        }
    }

    // Search query update
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // AI recommendation trigger
    fun triggerRecommendations(mood: String) {
        _recommendationMood.value = mood
        _isRecommendationLoading.value = true
        viewModelScope.launch {
            // Fetch catalog for references
            val catalog = allSongs.value.ifEmpty {
                // Fallback direct load if not fully initiated
                MusicDatabase.getInitialCatalog()
            }
            val ids = geminiService.getRecommendations(mood, currentSong.value, catalog)
            val filtered = catalog.filter { ids.contains(it.id) }
            _recommendedSongs.value = filtered.ifEmpty { catalog.shuffled().take(4) }
            _isRecommendationLoading.value = false
        }
    }

    // Sharing song display setter
    fun showShareCard(song: Song?) {
        _sharedSong.value = song
    }

    // Custom song insertion (Allows users to imports custom titles/URLs!)
    fun addCustomSong(title: String, artist: String, genre: String, audioUrl: String = "", lyrics: String = "") {
        viewModelScope.launch(Dispatchers.IO) {
            val randomId = "custom_" + System.currentTimeMillis()
            val finalAudioUrl = audioUrl.ifEmpty { "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3" }
            val newSong = Song(
                id = randomId,
                title = title,
                artist = artist,
                album = "Custom Upload",
                imageUrl = "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?w=300",
                audioUrl = finalAudioUrl,
                durationMs = 210000,
                durationString = "3:30",
                genre = genre,
                lyrics = lyrics.ifEmpty { "Custom lyrics line 1|Custom lyrics line 2|Feel the rhythm of your custom track!" }
            )
            repository.insertSongs(listOf(newSong))
        }
    }

    // Social Group listening session actions
    fun hostGroupSession() {
        val code = "S-7" + (100..999).random()
        _groupSessionCode.value = code
    }

    fun joinGroupSession(code: String) {
        _groupSessionCode.value = code.uppercase()
    }

    fun leaveGroupSession() {
        _groupSessionCode.value = null
    }

    fun syncPlaybackWithFriend(friend: FriendSession) {
        viewModelScope.launch {
            val matchedSong = allSongs.value.find { it.id == friend.matchedSongId }
            if (matchedSong != null) {
                // Instantly sync song
                playerManager.playSong(matchedSong)
                // Instantly seek to exact same timestamp of friend
                delay(120) // prepare sync delay
                playerManager.seekTo(friend.elapsedMs)
            }
        }
    }

    fun dispatchGroupEmoji(emoji: String) {
        val current = _floatingEmojis.value.toMutableList()
        current.add(emoji)
        _floatingEmojis.value = current
        viewModelScope.launch {
            delay(2500) // remove floating emoji after animation finished
            val post = _floatingEmojis.value.toMutableList()
            post.remove(emoji)
            _floatingEmojis.value = post
        }
    }

    // Simulating friends listening dynamically
    private fun startFriendSessionSimulation() {
        viewModelScope.launch(Dispatchers.Default) {
            val avatars = listOf(
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100", // F
                "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=100", // M
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=100", // F
                "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=100"  // M
            )

            val activeJob = kotlinx.coroutines.currentCoroutineContext()[kotlinx.coroutines.Job]
            while (activeJob != null && activeJob.isActive) {
                val catalog = MusicDatabase.getInitialCatalog()
                if (catalog.isNotEmpty()) {
                    val s1 = catalog[0] // Night Owl
                    val s2 = catalog[1] // Warm Nights
                    val s3 = catalog[4] // Dil Se Tum Pukar
                    val s4 = catalog[6] // Sunset Coffee

                    val elapsed1 = (System.currentTimeMillis() / 1000 % (s1.durationMs / 1000)) * 1000
                    val elapsed2 = (System.currentTimeMillis() / 1000 % (s2.durationMs / 1000)) * 1000
                    val elapsed3 = (System.currentTimeMillis() / 1000 % (s3.durationMs / 1000)) * 1000
                    val elapsed4 = (System.currentTimeMillis() / 1000 % (s4.durationMs / 1000)) * 1000

                    _friendSessions.value = listOf(
                        FriendSession("Ananya Singh", avatars[0], s1.title, s1.artist, elapsed1, s1.durationMs, true, s1.id),
                        FriendSession("Aarav Roy", avatars[1], s2.title, s2.artist, elapsed2, s2.durationMs, true, s2.id),
                        FriendSession("Ishita Mehta", avatars[2], s3.title, s3.artist, elapsed3, s3.durationMs, true, s3.id),
                        FriendSession("Dhruv Sharma", avatars[3], s4.title, s4.artist, elapsed4, s4.durationMs, false, s4.id)
                    )
                }
                delay(1000)
            }
        }
    }

    suspend fun exportPlaylistsBackup(): String {
        return withContext(Dispatchers.IO) {
            try {
                val playlistsToExport = repository.getAllPlaylistsDirect()
                val crossRefs = repository.getAllPlaylistSongRefs()
                val jsonArray = org.json.JSONArray()
                for (pl in playlistsToExport) {
                    val plObj = org.json.JSONObject()
                    plObj.put("name", pl.name)
                    plObj.put("description", pl.description)
                    
                    val songsArray = org.json.JSONArray()
                    val plSongsRefs = crossRefs.filter { it.playlistId == pl.id }
                    for (ref in plSongsRefs) {
                        songsArray.put(ref.songId)
                    }
                    plObj.put("songs", songsArray)
                    jsonArray.put(plObj)
                }
                jsonArray.toString(2)
            } catch (e: Exception) {
                "[]"
            }
        }
    }

    suspend fun importPlaylistsBackup(jsonString: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val jsonArray = org.json.JSONArray(jsonString)
                for (i in 0 until jsonArray.length()) {
                    val plObj = jsonArray.getJSONObject(i)
                    val name = plObj.getString("name")
                    val description = plObj.optString("description", "")
                    val songsArray = plObj.getJSONArray("songs")
                    
                    // Insert the playlist
                    val newPlaylistId = repository.createPlaylist(name, description)
                    for (j in 0 until songsArray.length()) {
                        val songId = songsArray.getString(j)
                        repository.addSongToPlaylist(newPlaylistId, songId)
                    }
                }
                true
            } catch (e: Exception) {
                false
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.release()
    }
}
