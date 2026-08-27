package com.mentality.sonethyst.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.HiddenLibraryItem
import com.mentality.sonethyst.data.PlaylistFolder
import com.mentality.sonethyst.data.SongVersionFinder
import com.mentality.sonethyst.data.SongVersionGroup
import com.mentality.sonethyst.model.Album
import com.mentality.sonethyst.model.Artist
import com.mentality.sonethyst.model.CustomTag
import com.mentality.sonethyst.model.Genre
import com.mentality.sonethyst.model.LibraryFilter
import com.mentality.sonethyst.model.LibraryLayout
import com.mentality.sonethyst.model.LibrarySort
import com.mentality.sonethyst.model.Playlist
import com.mentality.sonethyst.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LibraryUiState(
    val filter: LibraryFilter = LibraryFilter.ALL,
    val sort: LibrarySort = LibrarySort.RECENT,
    val layout: LibraryLayout = LibraryLayout.LIST,
    val loading: Boolean = true,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val genres: List<Genre> = emptyList(),
    val customTags: List<CustomTag> = emptyList(),
    val hiddenItems: List<HiddenLibraryItem> = emptyList(),
    val versionGroups: List<SongVersionGroup> = emptyList(),
    val versionsLoading: Boolean = false,
    val versionsLoaded: Boolean = false,
    val genresLoading: Boolean = false,
    val genresLoaded: Boolean = false,
    val supportsGenres: Boolean = false,
    val playlists: List<Playlist> = emptyList(),
    val playlistFolders: List<PlaylistFolder> = emptyList(),
    val songs: List<Song> = emptyList(),
    val songsLoadingMore: Boolean = false,
    val canLoadMoreSongs: Boolean = false,
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

    private var songLimit = INITIAL_SONG_LIMIT

    init {
        viewModelScope.launch {
            container.offline.collect {
                resetSongWindow()
                resetGenres()
                resetVersions()
                load()
            }
        }
        viewModelScope.launch {
            container.accountEpoch.drop(1).collect {
                resetSongWindow()
                resetGenres()
                resetVersions()
                load()
            }
        }
        viewModelScope.launch {
            container.libraryReload.drop(1).collect {
                val refreshGenres =
                    _state.value.genresLoaded

                val refreshVersions =
                    _state.value.versionsLoaded

                load()

                if (refreshGenres) {
                    refreshGenres(force = true)
                }

                if (refreshVersions) {
                    refreshVersions(force = true)
                }
            }
        }
        viewModelScope.launch {
            container.playlistReload.drop(1).collect {
                refreshPlaylists()
            }
        }
        viewModelScope.launch {
            container.customTagReload.drop(1).collect {
                refreshCustomTags()
            }
        }
        viewModelScope.launch {
            container.hiddenReload.drop(1).collect {
                refreshHiddenItems()
                refreshVisibleLibrary()

                if (_state.value.versionsLoaded) {
                    refreshVersions(force = true)
                }
            }
        }
        viewModelScope.launch {
            container.ratingChanges.collect { change ->
                _state.update { current ->
                    current.copy(
                        songs =
                            current.songs.map { song ->
                                if (song.id == change.songId) {
                                    song.copy(
                                        rating = change.rating
                                    )
                                } else {
                                    song
                                }
                            }
                    )
                }
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
            val songs = container.repository.librarySongs(songLimit)
            val likedCount = container.repository.starredCount()
            _state.update {
                it.copy(
                    loading = false,
                    playlists = playlists,
                    playlistFolders =
                        container.repository
                            .playlistFolders(),
                    albums = albums,
                    artists = artists,
                    songs = songs,
                    songsLoadingMore = false,
                    canLoadMoreSongs = songs.size >= songLimit,
                    downloadedRows = container.repository.downloadedLibrary(),
                    likedSongCount = likedCount,
                    likedCover = songs.firstOrNull()?.artworkUrl ?: "",
                    supportsFolders = container.repository.supportsFolders,
                    supportsGenres = container.repository.supportsGenres,
                    customTags = container.repository.allCustomTags(),
                    hiddenItems = container.repository.hiddenLibraryItems(),
                )
            }
        }
    }

    fun loadMoreSongs() {
        val current = _state.value

        if (
            current.loading ||
            current.songsLoadingMore ||
            !current.canLoadMoreSongs
        ) {
            return
        }

        val nextLimit = songLimit + SONG_PAGE_SIZE

        _state.update {
            it.copy(songsLoadingMore = true)
        }

        viewModelScope.launch {
            val songs =
                container.repository.librarySongs(nextLimit)

            // Do not destroy an already visible list if an incremental
            // backend request transiently fails and returns no rows.
            if (songs.isEmpty() && current.songs.isNotEmpty()) {
                _state.update {
                    it.copy(songsLoadingMore = false)
                }
                return@launch
            }

            songLimit = nextLimit

            _state.update {
                it.copy(
                    songs = songs,
                    songsLoadingMore = false,
                    canLoadMoreSongs = songs.size >= nextLimit,
                )
            }
        }
    }

    private fun resetSongWindow() {
        songLimit = INITIAL_SONG_LIMIT
    }

    private fun refreshGenres(
        force: Boolean = false,
    ) {
        val current = _state.value

        if (
            !container.repository.supportsGenres ||
            current.genresLoading ||
            (!force && current.genresLoaded)
        ) {
            return
        }

        _state.update {
            it.copy(genresLoading = true)
        }

        viewModelScope.launch {
            val genres =
                runCatching {
                    container.repository.allGenres()
                }.getOrDefault(emptyList())

            _state.update {
                it.copy(
                    genres = genres,
                    genresLoading = false,
                    genresLoaded = true,
                )
            }
        }
    }

    private fun resetGenres() {
        _state.update {
            it.copy(
                genres = emptyList(),
                genresLoading = false,
                genresLoaded = false,
            )
        }
    }

    private fun refreshVersions(
        force: Boolean = false,
        refreshLocal: Boolean = false,
    ) {
        val current =
            _state.value

        if (
            current.versionsLoading ||
            (!force && current.versionsLoaded)
        ) {
            return
        }

        _state.update {
            it.copy(
                versionsLoading = true
            )
        }

        viewModelScope.launch {
            if (refreshLocal) {
                runCatching {
                    container.refreshLocalLibrary()
                }
            }

            val candidates =
                runCatching {
                    container.repository
                        .duplicateCandidates()
                }.getOrDefault(
                    emptyList()
                )

            val groups =
                withContext(
                    Dispatchers.Default
                ) {
                    SongVersionFinder.find(
                        candidates
                    )
                }

            _state.update {
                it.copy(
                    versionGroups = groups,
                    versionsLoading = false,
                    versionsLoaded = true,
                )
            }
        }
    }

    private fun resetVersions() {
        _state.update {
            it.copy(
                versionGroups = emptyList(),
                versionsLoading = false,
                versionsLoaded = false,
            )
        }
    }

    private fun refreshHiddenItems() {
        _state.update {
            it.copy(
                hiddenItems =
                    container.repository
                        .hiddenLibraryItems()
            )
        }
    }

    private fun refreshVisibleLibrary() {
        viewModelScope.launch {
            val albums =
                container.repository.allAlbums()

            val songs =
                container.repository
                    .librarySongs(songLimit)

            _state.update {
                it.copy(
                    albums = albums,
                    songs = songs,
                    canLoadMoreSongs =
                        songs.size >= songLimit,
                )
            }
        }
    }

    private fun refreshCustomTags() {
        _state.update {
            it.copy(
                customTags =
                    container.repository.allCustomTags()
            )
        }
    }

    private fun refreshPlaylists() {
        viewModelScope.launch {
            val playlists =
                container.repository
                    .allPlaylists()

            val folders =
                container.repository
                    .playlistFolders()

            _state.update {
                it.copy(
                    playlists = playlists,
                    playlistFolders = folders,
                )
            }
        }
    }

    private companion object {
        const val INITIAL_SONG_LIMIT = 200
        const val SONG_PAGE_SIZE = 500
    }

    fun setFilter(f: LibraryFilter) {
        _state.update {
            it.copy(filter = f)
        }

        if (f == LibraryFilter.GENRES) {
            refreshGenres()
        }

        if (f == LibraryFilter.VERSIONS) {
            refreshVersions(
                force = true,
                refreshLocal = true,
            )
        }
    }
    fun setSort(s: LibrarySort) = _state.update { it.copy(sort = s) }
    fun toggleLayout() = _state.update {
        it.copy(layout = if (it.layout == LibraryLayout.LIST) LibraryLayout.GRID else LibraryLayout.LIST)
    }
}
