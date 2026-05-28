package com.example.data

import kotlinx.coroutines.flow.Flow

class MusicRepository(private val musicDao: MusicDao) {

    val allSongs: Flow<List<Song>> = musicDao.getAllSongs()
    val favoriteSongs: Flow<List<Song>> = musicDao.getFavoriteSongs()
    val downloadedSongs: Flow<List<Song>> = musicDao.getDownloadedSongs()
    val allPlaylists: Flow<List<Playlist>> = musicDao.getAllPlaylists()

    suspend fun getSongById(songId: String): Song? {
        return musicDao.getSongById(songId)
    }

    fun searchSongs(query: String): Flow<List<Song>> {
        return musicDao.searchSongs(query)
    }

    suspend fun toggleFavorite(songId: String, currentStatus: Boolean) {
        musicDao.updateFavoriteStatus(songId, !currentStatus)
    }

    suspend fun updateDownloadStatus(songId: String, isDownloaded: Boolean) {
        musicDao.updateDownloadStatus(songId, isDownloaded)
    }

    suspend fun createPlaylist(name: String, description: String = ""): Long {
        return musicDao.createPlaylist(Playlist(name = name, description = description))
    }

    suspend fun deletePlaylist(playlistId: Long) {
        musicDao.deletePlaylist(playlistId)
    }

    suspend fun addSongToPlaylist(playlistId: Long, songId: String) {
        musicDao.insertPlaylistSongCrossRef(PlaylistSongCrossRef(playlistId, songId))
    }

    suspend fun removeSongFromPlaylist(playlistId: Long, songId: String) {
        musicDao.removeSongFromPlaylist(playlistId, songId)
    }

    fun getPlaylistWithSongs(playlistId: Long): Flow<PlaylistWithSongs?> {
        return musicDao.getPlaylistWithSongs(playlistId)
    }

    suspend fun getAllPlaylistsDirect(): List<Playlist> {
        return musicDao.getAllPlaylistsDirect()
    }

    suspend fun getAllPlaylistSongRefs(): List<PlaylistSongCrossRef> {
        return musicDao.getAllPlaylistSongRefs()
    }

    suspend fun insertSongs(songs: List<Song>) {
        musicDao.insertSongs(songs)
    }
}
