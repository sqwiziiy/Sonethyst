package com.mentality.sonethyst.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.model.LibraryFilter
import com.mentality.sonethyst.model.LibraryLayout
import com.mentality.sonethyst.model.LibrarySort
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.ui.components.Artwork
import com.mentality.sonethyst.ui.components.SongRow
import com.mentality.sonethyst.util.accentFor
import com.mentality.sonethyst.viewmodel.LibraryUiState

private data class LibRow(val title: String, val subtitle: String, val art: String, val accent: androidx.compose.ui.graphics.Color, val id: String, val kind: String, val circle: Boolean = false, val menu: Boolean = true)

private class LibActions(
    val isLiked: (String) -> Boolean,
    val onPlay: (LibRow) -> Unit,
    val onShuffle: (LibRow) -> Unit,
    val onQueue: (LibRow) -> Unit,
    val onToggleLike: (LibRow) -> Unit,
    val onDelete: (LibRow) -> Unit,
    val onEditSmart: (LibRow) -> Unit,
    val onDeleteSmart: (LibRow) -> Unit,
    val onExport: (LibRow) -> Unit,
)

@Composable
fun LibraryScreen(
    contentPadding: PaddingValues,
    state: LibraryUiState,
    username: String,
    likedIds: Set<String>,
    currentSongId: String,
    isPlaying: Boolean,
    onFilter: (LibraryFilter) -> Unit,
    onLoadMoreSongs: () -> Unit,
    onSort: (LibrarySort) -> Unit,
    onToggleLayout: () -> Unit,
    onOpenDrawer: () -> Unit,
    onPlayAll: (List<Song>, Int) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onToggleLike: (String) -> Unit,
    onOpenDetail: (String, String) -> Unit,
    downloadedIds: Set<String>,
    onDownload: (Song) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onOpenSearch: () -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onCreateSmart: () -> Unit,
    onEditSmart: (String) -> Unit,
    onDeleteSmart: (String) -> Unit,
    onImportM3u: () -> Unit,
    onExportPlaylist: (String, String, String) -> Unit,
    onOpenFolders: () -> Unit,
    onOpenRadio: () -> Unit = {},
    onOpenPodcasts: () -> Unit = {},
    onPlayCollection: (String, String) -> Unit,
    onShuffleCollection: (String, String) -> Unit,
    onQueueCollection: (String, String) -> Unit,
    onToggleLikeKind: (String, String) -> Unit,
    onDeletePlaylist: (String) -> Unit,
    canDownload: Boolean = true,
    pins: List<com.mentality.sonethyst.data.Pin> = emptyList(),
    onEditTags: ((Song) -> Unit)? = null,
    serverTagEditing: Boolean = false,
    onSetRating: ((Song, Int) -> Unit)? = null,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var showCreate by remember { mutableStateOf(false) }
    val filter = state.filter
    val sort = state.sort
    val layout = state.layout
    val libColumns = com.mentality.sonethyst.ui.theme.LocalUiPrefs.current.libraryColumns.coerceIn(2, 4)

    val actions = remember(
        likedIds,
        onPlayCollection,
        onShuffleCollection,
        onQueueCollection,
        onToggleLikeKind,
        onDeletePlaylist,
        onEditSmart,
        onDeleteSmart,
        onExportPlaylist,
    ) {
        LibActions(
            isLiked = { id -> likedIds.contains(id) },
            onPlay = { r -> onPlayCollection(r.id, r.kind) },
            onShuffle = { r -> onShuffleCollection(r.id, r.kind) },
            onQueue = { r -> onQueueCollection(r.id, r.kind) },
            onToggleLike = { r -> onToggleLikeKind(r.id, r.kind) },
            onDelete = { r -> onDeletePlaylist(r.id) },
            onEditSmart = { r -> onEditSmart(r.id) },
            onDeleteSmart = { r -> onDeleteSmart(r.id) },
            onExport = { r -> onExportPlaylist(r.id, r.kind, r.title) },
        )
    }

    val visibleFilters = remember(
        canDownload,
        state.supportsGenres,
    ) {
        LibraryFilter.entries.filter {
            (canDownload ||
                it != LibraryFilter.DOWNLOADED) &&
                (state.supportsGenres ||
                    it != LibraryFilter.GENRES)
        }
    }

    val userInitials = remember(username) {
        username.take(2).uppercase().ifBlank { "ME" }
    }

    Column(Modifier.fillMaxWidth().padding(top = topInset)) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
                    .clickable(onClick = onOpenDrawer),
                contentAlignment = Alignment.Center,
            ) { Text(userInitials, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary) }
            Spacer(Modifier.width(12.dp))
            Text("Your Library", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(Icons.Filled.Search, "Search", modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onOpenSearch).padding(8.dp))
            Icon(Icons.Filled.Add, "Create playlist", modifier = Modifier.size(40.dp).clip(CircleShape).clickable { showCreate = true }.padding(8.dp))
        }

        if (showCreate) {
            CreatePlaylistDialog(
                onCreate = { name -> onCreatePlaylist(name); showCreate = false },
                onCreateSmart = { showCreate = false; onCreateSmart() },
                onImportM3u = { showCreate = false; onImportM3u() },
                onDismiss = { showCreate = false },
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (filter != LibraryFilter.ALL) {
                item {
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable { onFilter(LibraryFilter.ALL) },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Close, "Clear", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }
                }
            }
            // local mode has no downloads
            items(
                count = visibleFilters.size,
                key = { i -> visibleFilters[i].name },
                contentType = { "library-filter" },
            ) { i ->
                val f = visibleFilters[i]
                if (f == LibraryFilter.ALL && filter != LibraryFilter.ALL) return@items
                FilterChip(f.label, selected = f == filter) { onFilter(f) }
            }
        }

        Spacer(Modifier.height(12.dp))

        var sortMenu by remember { mutableStateOf(false) }
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).clickable { sortMenu = true }.padding(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.SwapVert, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(sort.label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium)
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    LibrarySort.entries.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s.label) },
                            onClick = { onSort(s); sortMenu = false },
                            trailingIcon = { if (s == sort) Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary) },
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Icon(
                imageVector = if (layout == LibraryLayout.LIST) Icons.Filled.GridView else Icons.AutoMirrored.Filled.List,
                contentDescription = "Toggle layout",
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onToggleLayout).padding(8.dp),
            )
        }

        Spacer(Modifier.height(4.dp))

        val bottom = contentPadding.calculateBottomPadding() + 24.dp

        if (state.loading && state.albums.isEmpty() && state.playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                com.mentality.sonethyst.ui.components.LottieLoader(modifier = Modifier.size(72.dp))
            }
            return@Column
        }

        if (
            filter == LibraryFilter.GENRES &&
            state.genresLoading &&
            state.genres.isEmpty()
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                com.mentality.sonethyst.ui.components.LottieLoader(
                    modifier = Modifier.size(72.dp)
                )
            }
            return@Column
        }

        if (filter == LibraryFilter.SONGS) {
            val songs = remember(state.songs, sort) {
                sortedSongs(state.songs, sort)
            }

            val songsListState =
                androidx.compose.foundation.lazy.rememberLazyListState()

            androidx.compose.runtime.LaunchedEffect(
                songsListState,
                state.canLoadMoreSongs,
                state.songsLoadingMore,
            ) {
                androidx.compose.runtime.snapshotFlow {
                    val layout = songsListState.layoutInfo
                    val last =
                        layout.visibleItemsInfo.lastOrNull()?.index ?: -1
                    last to layout.totalItemsCount
                }.collect { (last, count) ->
                    if (
                        state.canLoadMoreSongs &&
                        !state.songsLoadingMore &&
                        count > 0 &&
                        last >= count - 12
                    ) {
                        onLoadMoreSongs()
                    }
                }
            }

            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                state = songsListState,
                contentPadding = PaddingValues(bottom = bottom),
            ) {
                items(
                    count = songs.size,
                    key = { i -> songs[i].id },
                    contentType = { "song" },
                ) { i ->
                    val s = songs[i]
                    SongRow(
                        s, isPlaying = s.id == currentSongId && isPlaying, isLiked = likedIds.contains(s.id),
                        onClick = { onPlayAll(songs, i) }, onToggleLike = { onToggleLike(s.id) },
                        onAddToQueue = { onAddToQueue(s) }, onPlayNext = { onPlayNext(s) },
                        onAddToPlaylist = { onAddToPlaylist(s) },
                        onGoToAlbum = if (s.albumId.isNotBlank()) ({ onOpenDetail("album", s.albumId) }) else null,
                        onGoToArtist = if (s.artistId.isNotBlank()) ({ onOpenDetail("artist", s.artistId) }) else null,
                        isDownloaded = canDownload && downloadedIds.contains(s.id),
                        onDownload = if (canDownload) ({ onDownload(s) }) else null,
                        onRemoveDownload = if (canDownload) ({ onRemoveDownload(s.id) }) else null,
                        onEditTags = onEditTags?.let { cb -> { cb(s) } },
                        serverTagEditing = serverTagEditing,
                        onSetRating =
                            onSetRating?.let { cb ->
                                { rating ->
                                    cb(s, rating)
                                }
                            },
                    )
                }

                if (state.songsLoadingMore) {
                    item(
                        key = "songs-loading-more",
                        contentType = "songs-loading",
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            com.mentality.sonethyst.ui.components.LottieLoader(
                                modifier = Modifier.size(44.dp),
                            )
                        }
                    }
                }
            }
            return@Column
        }

        if (filter == LibraryFilter.DOWNLOADED) {
            val dlRows = remember(state.downloadedRows) {
                state.downloadedRows.map {
                    LibRow(
                        it.title,
                        "${it.kind.replaceFirstChar { c -> c.uppercase() }} • Downloaded",
                        it.coverUrl,
                        it.accent,
                        it.id,
                        it.kind,
                    )
                }
            }
            if (dlRows.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No downloads yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                return@Column
            }
            if (layout == LibraryLayout.LIST) {
                LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentPadding = PaddingValues(bottom = bottom)) {
                    items(
                        count = dlRows.size,
                        key = { i -> "${dlRows[i].kind}:${dlRows[i].id}" },
                        contentType = { "library-row" },
                    ) { i ->
                        LibListItem(dlRows[i], actions) {
                            onOpenDetail(dlRows[i].kind, dlRows[i].id)
                        }
                    }
                }
            } else {
                LazyVerticalGrid(columns = GridCells.Fixed(libColumns), modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), contentPadding = PaddingValues(bottom = bottom), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(
                        count = dlRows.size,
                        key = { i -> "${dlRows[i].kind}:${dlRows[i].id}" },
                        contentType = { "library-grid" },
                    ) { i ->
                        LibGridItem(dlRows[i], actions) {
                            onOpenDetail(dlRows[i].kind, dlRows[i].id)
                        }
                    }
                }
            }
            return@Column
        }

        val rows = when (filter) {
            LibraryFilter.ALL -> remember(
                state.smartPlaylists,
                state.playlists,
                state.albums,
                state.artists,
                state.likedSongCount,
                state.likedCover,
                state.supportsFolders,
                filter,
                sort,
                pins,
            ) {
                buildRows(state, filter, sort, pins)
            }

            LibraryFilter.PLAYLISTS -> remember(
                state.smartPlaylists,
                state.playlists,
                state.likedSongCount,
                state.likedCover,
                filter,
                sort,
                pins,
            ) {
                buildRows(state, filter, sort, pins)
            }

            LibraryFilter.ALBUMS -> remember(
                state.albums,
                filter,
                sort,
            ) {
                buildRows(state, filter, sort, pins)
            }

            LibraryFilter.ARTISTS -> remember(
                state.artists,
                filter,
                sort,
            ) {
                buildRows(state, filter, sort, pins)
            }

            LibraryFilter.GENRES -> remember(
                state.genres,
                filter,
                sort,
            ) {
                buildRows(state, filter, sort, pins)
            }

            else -> emptyList()
        }

        val openRow: (LibRow) -> Unit = remember(
            onOpenFolders,
            onOpenRadio,
            onOpenPodcasts,
            onOpenDetail,
        ) {
            { r ->
                when (r.kind) {
                    "folders" -> onOpenFolders()
                    "radio" -> onOpenRadio()
                    "podcasts" -> onOpenPodcasts()
                    else -> onOpenDetail(r.kind, r.id)
                }
            }
        }
        if (layout == LibraryLayout.LIST) {
            LazyColumn(Modifier.fillMaxWidth().padding(horizontal = 8.dp), contentPadding = PaddingValues(bottom = bottom)) {
                items(
                    count = rows.size,
                    key = { i ->
                        "${rows[i].kind}:${rows[i].id.ifBlank { rows[i].title }}"
                    },
                    contentType = { "library-row" },
                ) { i ->
                    LibListItem(rows[i], actions) {
                        openRow(rows[i])
                    }
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(libColumns),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                contentPadding = PaddingValues(bottom = bottom),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(
                    count = rows.size,
                    key = { i ->
                        "${rows[i].kind}:${rows[i].id.ifBlank { rows[i].title }}"
                    },
                    contentType = { "library-grid" },
                ) { i ->
                    LibGridItem(rows[i], actions) {
                        openRow(rows[i])
                    }
                }
            }
        }
    }
}

private fun sortedSongs(songs: List<Song>, sort: LibrarySort): List<Song> = when (sort) {
    LibrarySort.ALPHABETICAL -> songs.sortedBy { it.title }
    LibrarySort.CREATOR -> songs.sortedBy { it.artist }
    else -> songs
}

private fun buildRows(
    state: LibraryUiState,
    filter: LibraryFilter,
    sort: LibrarySort,
    pins: List<com.mentality.sonethyst.data.Pin>,
): List<LibRow> {
    val base: List<LibRow> = when (filter) {
        LibraryFilter.ALL -> buildList<LibRow>(
            state.smartPlaylists.size +
                state.playlists.size +
                state.albums.size +
                state.artists.size
        ) {
            state.smartPlaylists.forEach {
                val n = it.rules.orEmpty().size
                add(
                    LibRow(
                        it.name ?: "Smart playlist",
                        "Smart playlist • $n rule${if (n == 1) "" else "s"}",
                        "",
                        accentFor(it.id ?: "smart"),
                        it.id ?: "",
                        "smart",
                    )
                )
            }

            state.playlists.forEach {
                add(
                    LibRow(
                        it.title,
                        "Playlist • ${it.songCount} songs",
                        it.coverUrl,
                        it.accent,
                        it.id,
                        "playlist",
                    )
                )
            }

            state.albums.forEach {
                add(
                    LibRow(
                        it.title,
                        "Album • ${it.artist}",
                        it.artworkUrl,
                        accentFor(it.id),
                        it.id,
                        "album",
                    )
                )
            }

            state.artists.forEach {
                add(
                    LibRow(
                        it.name,
                        "Artist",
                        it.imageUrl,
                        accentFor(it.id),
                        it.id,
                        "artist",
                        circle = true,
                    )
                )
            }
        }

        LibraryFilter.PLAYLISTS -> buildList<LibRow>(
            state.smartPlaylists.size + state.playlists.size
        ) {
            state.smartPlaylists.forEach {
                val n = it.rules.orEmpty().size
                add(
                    LibRow(
                        it.name ?: "Smart playlist",
                        "Smart playlist • $n rule${if (n == 1) "" else "s"}",
                        "",
                        accentFor(it.id ?: "smart"),
                        it.id ?: "",
                        "smart",
                    )
                )
            }

            state.playlists.forEach {
                add(
                    LibRow(
                        it.title,
                        "Playlist • ${it.songCount} songs",
                        it.coverUrl,
                        it.accent,
                        it.id,
                        "playlist",
                    )
                )
            }
        }

        LibraryFilter.ALBUMS -> state.albums.map {
            LibRow(
                it.title,
                "Album • ${it.artist}",
                it.artworkUrl,
                accentFor(it.id),
                it.id,
                "album",
            )
        }

        LibraryFilter.ARTISTS -> state.artists.map {
            LibRow(
                it.name,
                "Artist",
                it.imageUrl,
                accentFor(it.id),
                it.id,
                "artist",
                circle = true,
            )
        }

        LibraryFilter.GENRES -> state.genres.map {
            LibRow(
                it.name,
                if (it.songCount > 0) {
                    "Genre • ${it.songCount} song${if (it.songCount == 1) "" else "s"}"
                } else {
                    "Genre"
                },
                "",
                accentFor("genre:${it.name}"),
                it.id,
                "genre",
                menu = false,
            )
        }

        else -> return emptyList()
    }

    val sorted: List<LibRow> = when (sort) {
        LibrarySort.ALPHABETICAL -> base.sortedBy { it.title }
        LibrarySort.CREATOR -> base.sortedBy { it.subtitle }
        else -> base
    }

    if (
        filter == LibraryFilter.ALBUMS ||
        filter == LibraryFilter.ARTISTS ||
        filter == LibraryFilter.GENRES
    ) {
        return sorted
    }

    val pinned: Set<Pair<String, String>> = if (pins.isEmpty()) {
        emptySet()
    } else {
        pins.mapTo(mutableSetOf()) { it.kind to it.id }
    }

    val deduped = if (pinned.isEmpty()) {
        sorted
    } else {
        sorted.filterNot { (it.kind to it.id) in pinned }
    }

    return buildList<LibRow> {
        if (filter == LibraryFilter.ALL) {
            pins.forEach {
                add(
                    LibRow(
                        it.title,
                        "Pinned • ${it.kind.replaceFirstChar { c -> c.uppercase() }}",
                        it.coverUrl,
                        accentFor(it.id),
                        it.id,
                        it.kind,
                        circle = it.kind == "artist",
                    )
                )
            }
        }

        add(
            LibRow(
                "Liked Songs",
                "Playlist • ${state.likedSongCount} songs",
                state.likedCover,
                accentFor("liked"),
                "liked",
                "liked",
            )
        )

        if (filter == LibraryFilter.ALL && state.supportsFolders) {
            add(
                LibRow(
                    "Folders",
                    "Browse by folder",
                    "",
                    accentFor("folders"),
                    "",
                    "folders",
                    menu = false,
                )
            )
        }

        if (filter == LibraryFilter.ALL) {
            add(
                LibRow(
                    "Radio",
                    "Live internet stations",
                    "",
                    accentFor("radio"),
                    "",
                    "radio",
                    menu = false,
                )
            )
            add(
                LibRow(
                    "Podcasts",
                    "Shows & episodes",
                    "",
                    accentFor("podcasts"),
                    "",
                    "podcasts",
                    menu = false,
                )
            )
        }

        addAll(deduped)
    }
}

@Composable
private fun CreatePlaylistDialog(onCreate: (String) -> Unit, onCreateSmart: () -> Unit, onImportM3u: () -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Playlist name") },
                    singleLine = true,
                )
                TextButton(onClick = onCreateSmart, modifier = Modifier.padding(top = 6.dp)) {
                    Text("Create a smart playlist instead")
                }
                TextButton(onClick = onImportM3u) {
                    Text("Import an M3U file")
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onCreate(name.trim()) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected)
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))
    else
        Brush.linearGradient(listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.surfaceContainerHigh))
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .then(if (selected) Modifier else Modifier.border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(50)))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 9.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun LibListItem(row: LibRow, actions: LibActions, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(row.art, row.accent, Modifier.size(56.dp), corner = if (row.circle) 56.dp else 14.dp)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (row.menu) {
            var menuOpen by remember { mutableStateOf(false) }
            Box {
                Icon(
                    Icons.Filled.MoreVert, "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(34.dp).clip(CircleShape).clickable { menuOpen = true }.padding(6.dp),
                )
                CollectionMenu(row, actions, expanded = menuOpen, onDismiss = { menuOpen = false })
            }
        }
    }
}

@Composable
private fun LibGridItem(row: LibRow, actions: LibActions, onClick: () -> Unit) {
    Column(Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(6.dp)) {
        Artwork(row.art, row.accent, Modifier.fillMaxWidth().aspectRatio(1f), corner = if (row.circle) 200.dp else 12.dp)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(row.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(row.subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (row.menu) {
                var menuOpen by remember { mutableStateOf(false) }
                Box {
                    Icon(
                        Icons.Filled.MoreVert, "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp).clip(CircleShape).clickable { menuOpen = true }.padding(4.dp),
                    )
                    CollectionMenu(row, actions, expanded = menuOpen, onDismiss = { menuOpen = false })
                }
            }
        }
    }
}

@Composable
private fun CollectionMenu(row: LibRow, actions: LibActions, expanded: Boolean, onDismiss: () -> Unit) {
    // liked row is virtual no like/delete just playback
    val isVirtual = row.kind == "liked"
    val isSmart = row.kind == "smart"
    val isPlaylist = row.kind == "playlist"
    val liked = actions.isLiked(row.id)
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(text = { Text("Play") }, onClick = { onDismiss(); actions.onPlay(row) }, leadingIcon = { Icon(Icons.Filled.PlayArrow, null) })
        DropdownMenuItem(text = { Text("Shuffle") }, onClick = { onDismiss(); actions.onShuffle(row) }, leadingIcon = { Icon(Icons.Filled.Shuffle, null) })
        DropdownMenuItem(text = { Text("Add to queue") }, onClick = { onDismiss(); actions.onQueue(row) }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) })
        if (isPlaylist || isSmart || isVirtual) {
            DropdownMenuItem(
                text = { Text("Export as M3U") },
                onClick = { onDismiss(); actions.onExport(row) },
                leadingIcon = { Icon(Icons.Filled.IosShare, null) },
            )
        }
        if (isSmart) {
            DropdownMenuItem(
                text = { Text("Edit rules") },
                onClick = { onDismiss(); actions.onEditSmart(row) },
                leadingIcon = { Icon(Icons.Filled.Edit, null) },
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = { onDismiss(); actions.onDeleteSmart(row) },
                leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
            )
        }
        if (!isVirtual && !isSmart) {
            DropdownMenuItem(
                text = { Text(if (liked) "Unlike" else "Like") },
                onClick = { onDismiss(); actions.onToggleLike(row) },
                leadingIcon = { Icon(if (liked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, null) },
            )
            if (isPlaylist) {
                DropdownMenuItem(
                    text = { Text("Delete playlist") },
                    onClick = { onDismiss(); actions.onDelete(row) },
                    leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) },
                )
            }
        }
    }
}
