package com.mentality.sonethyst.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.model.Album
import com.mentality.sonethyst.model.Artist
import com.mentality.sonethyst.model.LibraryFilter
import com.mentality.sonethyst.model.LibraryLayout
import com.mentality.sonethyst.model.LibrarySort
import com.mentality.sonethyst.model.Playlist
import com.mentality.sonethyst.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryUiState(
    val filter: LibraryFilter = LibraryFilter.ALL,
    val sort: LibrarySort = LibrarySort.RECENT,
    val layout: LibraryLayout = LibraryLayout.LIST,
    val loading: Boolean = true,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val songs: List<Song> = emptyList(),
    val downloadedRows: List<com.mentality.sonethyst.data.DownloadRow> = emptyList(),
    val likedSongCount: Int = 0,
    val likedCover: String = "",
    val supportsFolders: Boolean = false,
    val smartPlaylists: List<com.mentality.sonethyst.data.SmartPlaylist> = emptyList(),
)

class LibraryViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as SonethystApplication).container
    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch { container.offline.collect { load() } }
        viewModelScope.launch { container.accountEpoch.drop(1).collect { load() } }
        viewModelScope.launch { container.libraryReload.drop(1).collect { load() } }
        viewModelScope.launch {
            container.playlistReload.drop(1).collect {
                refreshPlaylists()
            }
        }
        viewModelScope.launch {
            container.downloadManager.downloads.collect {
                _state.update { s -> s.copy(downloadedRows = container.repository.downloadedLibrary()) }
            }
        }
        viewModelScope.launch {
            container.downloadManager.collections.collect {
                _state.update { s -> s.copy(downloadedRows = container.repository.downloadedLibrary()) }
            }
        }
        viewModelScope.launch {
            container.settingsStore.smartPlaylists.collect { sps ->
                _state.update { s -> s.copy(smartPlaylists = sps) }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val playlists = container.repository.allPlaylists()
            val albums = container.repository.allAlbums()
            val artists = container.repository.allArtists()
            val songs = container.repository.allSongs()
            val likedCount = container.repository.starredCount()
            _state.update {
                it.copy(loading = false, playlists = playlists, albums = albums, artists = artists, songs = songs, downloadedRows = container.repository.downloadedLibrary(), likedSongCount = likedCount, likedCover = songs.firstOrNull()?.artworkUrl ?: "", supportsFolders = container.repository.supportsFolders)
            }
        }
    }

    private fun refreshPlaylists() {
        viewModelScope.launch {
            val playlists = container.repository.allPlaylists()
            _state.update { it.copy(playlists = playlists) }
        }
    }

    fun setFilter(f: LibraryFilter) = _state.update { it.copy(filter = f) }
    fun setSort(s: LibrarySort) = _state.update { it.copy(sort = s) }
    fun toggleLayout() = _state.update {
        it.copy(layout = if (it.layout == LibraryLayout.LIST) LibraryLayout.GRID else LibraryLayout.LIST)
    }
}
