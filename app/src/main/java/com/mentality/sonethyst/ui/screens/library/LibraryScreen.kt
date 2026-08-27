package com.mentality.sonethyst.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.animation.core.tween
import kotlinx.coroutines.launch
import kotlin.math.abs
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitEachGesture
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
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material.icons.filled.VisibilityOff
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
import com.mentality.sonethyst.data.SongVersionGroup
import com.mentality.sonethyst.data.PlaylistFolder
import com.mentality.sonethyst.model.LibraryFilter
import com.mentality.sonethyst.model.LibraryLayout
import com.mentality.sonethyst.model.LibrarySort
import com.mentality.sonethyst.model.Playlist
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.ui.components.Artwork
import com.mentality.sonethyst.ui.components.SongRow
import com.mentality.sonethyst.util.accentFor
import com.mentality.sonethyst.viewmodel.LibraryUiState

private data class LibRow(
    val title: String,
    val subtitle: String,
    val art: String,
    val accent: androidx.compose.ui.graphics.Color,
    val id: String,
    val kind: String,
    val circle: Boolean = false,
    val menu: Boolean = true,
    val hideable: Boolean = false,
)

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
    val onHide: (LibRow) -> Unit,
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
    onCreatePlaylist: (String, String) -> Unit,
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
    onCreatePlaylistFolder: (String, String) -> Unit,
    onRenamePlaylistFolder: (String, String) -> Unit,
    onMovePlaylistFolder: (String, String) -> Unit,
    onDeletePlaylistFolder: (String) -> Unit,
    onMovePlaylistToFolder: (String, String) -> Unit,
    canDownload: Boolean = true,
    pins: List<com.mentality.sonethyst.data.Pin> = emptyList(),
    onEditTags: ((Song) -> Unit)? = null,
    serverTagEditing: Boolean = false,
    onSetRating: ((Song, Int) -> Unit)? = null,
    onEditCustomTags: ((Song) -> Unit)? = null,
    onHideSong: ((Song) -> Unit)? = null,
    onHideAlbum: (String, String, String, String) -> Unit =
        { _, _, _, _ -> },
    onRestoreHidden:
        (com.mentality.sonethyst.data.HiddenLibraryItem) -> Unit = {},
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var showCreateMenu by remember {
        mutableStateOf(false)
    }

    var showCreatePlaylist by remember {
        mutableStateOf(false)
    }
    var currentPlaylistFolderId by
        remember {
            mutableStateOf("")
        }

    var createFolderParentId by
        remember {
            mutableStateOf<String?>(null)
        }

    var renameFolder by
        remember {
            mutableStateOf<PlaylistFolder?>(null)
        }

    var movePlaylist by
        remember {
            mutableStateOf<Playlist?>(null)
        }

    var moveFolder by
        remember {
            mutableStateOf<PlaylistFolder?>(null)
        }

    androidx.compose.runtime.LaunchedEffect(
        state.playlistFolders,
        currentPlaylistFolderId,
    ) {
        // playlist-folder-navigation-validity
        if (
            currentPlaylistFolderId.isNotBlank() &&
            state.playlistFolders.none {
                it.id == currentPlaylistFolderId
            }
        ) {
            currentPlaylistFolderId = ""
        }
    }

    // playlist-folder-android-back
    androidx.activity.compose.BackHandler(
        enabled =
            state.filter == LibraryFilter.PLAYLISTS &&
                currentPlaylistFolderId.isNotBlank(),
    ) {
        val current =
            state.playlistFolders
                .firstOrNull {
                    it.id ==
                        currentPlaylistFolderId
                }

        currentPlaylistFolderId =
            current?.parentId.orEmpty()
    }

    val filter = state.filter
    val sort = state.sort
    val layout = state.layout

    // library-context-controls-state
    val availableSorts =
        when (filter) {
            LibraryFilter.SONGS ->
                listOf(
                    LibrarySort.RECENT,
                    LibrarySort.ALPHABETICAL,
                    LibrarySort.CREATOR,
                )

            LibraryFilter.ALBUMS ->
                listOf(
                    LibrarySort.ALPHABETICAL,
                    LibrarySort.CREATOR,
                )

            else ->
                emptyList()
        }

    val displayedSort =
        if (sort in availableSorts) {
            sort
        } else {
            availableSorts.firstOrNull()
                ?: sort
        }

    val showSortControl =
        availableSorts.size > 1

    val supportsLayoutToggle =
        when (filter) {
            LibraryFilter.ALL,
            LibraryFilter.PLAYLISTS,
            LibraryFilter.ALBUMS,
            LibraryFilter.ARTISTS,
            LibraryFilter.GENRES,
            LibraryFilter.TAGS,
            LibraryFilter.DOWNLOADED,
            -> true

            else -> false
        }

    androidx.compose.runtime.LaunchedEffect(
        filter
    ) {
        if (
            availableSorts.isNotEmpty() &&
            sort !in availableSorts
        ) {
            onSort(
                availableSorts.first()
            )
        }
    }
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
        onHideAlbum,
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
            onHide = { r ->
                if (r.kind == "album") {
                    onHideAlbum(
                        r.id,
                        r.title,
                        r.subtitle,
                        r.art,
                    )
                }
            },
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

    // library-category-pager
    val categoryPagerState =
        androidx.compose.foundation.pager
            .rememberPagerState(
                initialPage =
                    visibleFilters
                        .indexOf(filter)
                        .coerceAtLeast(0),
                pageCount = {
                    visibleFilters.size
                },
            )

    val filterRowState =
        androidx.compose.foundation.lazy
            .rememberLazyListState()

    val pagerDisplayFilter =
        visibleFilters
            .getOrNull(
                categoryPagerState.currentPage
            )
            ?: filter

    val drawerEdgeThreshold =
        with(LocalDensity.current) {
            72.dp.toPx()
        }

    /*
     * Chip -> pager.
     *
     * Clicking a Library category changes the ViewModel filter,
     * then the pager follows it with the same page animation used
     * by a real swipe.
     */
    androidx.compose.runtime.LaunchedEffect(
        filter,
        visibleFilters,
    ) {
        val target =
            visibleFilters.indexOf(filter)

        if (
            target >= 0 &&
            target !=
                categoryPagerState.currentPage
        ) {
            categoryPagerState
                .animateScrollToPage(
                    target
                )
        }
    }

    /*
     * Pager -> ViewModel.
     *
     * Do not change the real filter while the finger is still
     * dragging. Commit it only once the pager settles.
     */
    androidx.compose.runtime.LaunchedEffect(
        categoryPagerState.currentPage,
        categoryPagerState.isScrollInProgress,
        visibleFilters,
    ) {
        if (
            !categoryPagerState
                .isScrollInProgress
        ) {
            val settledFilter =
                visibleFilters
                    .getOrNull(
                        categoryPagerState
                            .currentPage
                    )

            if (
                settledFilter != null &&
                settledFilter != filter
            ) {
                onFilter(
                    settledFilter
                )
            }
        }
    }

    /*
     * Keep the selected chip visible while paging through more
     * categories than fit on screen.
     */
    androidx.compose.runtime.LaunchedEffect(
        categoryPagerState.currentPage,
        visibleFilters,
    ) {
        val index =
            categoryPagerState
                .currentPage
                .coerceIn(
                    0,
                    (visibleFilters.size - 1)
                        .coerceAtLeast(0),
                )

        if (visibleFilters.isNotEmpty()) {
            filterRowState
                .animateScrollToItem(
                    index
                )
        }
    }

    val userInitials = remember(username) {
        username.take(2).uppercase().ifBlank { "ME" }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(
                top = topInset
            )
    ) {
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
            Box {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create",
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                showCreateMenu = true
                            }
                            .padding(8.dp),
                )

                DropdownMenu(
                    expanded = showCreateMenu,
                    onDismissRequest = {
                        showCreateMenu = false
                    },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("New playlist")
                        },
                        onClick = {
                            showCreateMenu = false
                            showCreatePlaylist = true
                        },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                null,
                            )
                        },
                    )

                    DropdownMenuItem(
                        text = {
                            Text("New folder")
                        },
                        onClick = {
                            showCreateMenu = false

                            createFolderParentId =
                                if (
                                    filter ==
                                        LibraryFilter.PLAYLISTS
                                ) {
                                    currentPlaylistFolderId
                                } else {
                                    ""
                                }
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.CreateNewFolder,
                                null,
                            )
                        },
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Smart playlist")
                        },
                        onClick = {
                            showCreateMenu = false
                            onCreateSmart()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.GridView,
                                null,
                            )
                        },
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Import playlist")
                        },
                        onClick = {
                            showCreateMenu = false
                            onImportM3u()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.IosShare,
                                null,
                            )
                        },
                    )
                }
            }
        }

        createFolderParentId?.let { parentId ->
            PlaylistFolderNameDialog(
                title = "New folder",
                initialName = "",
                confirmLabel = "Create",
                existingNames =
                    (
                        state.playlistFolders
                            .filter {
                                it.parentId ==
                                    parentId
                            }
                            .map {
                                it.name
                            } +
                        state.playlists
                            .filter {
                                it.folderId ==
                                    parentId
                            }
                            .map {
                                it.title
                            }
                    ).toSet(),
                onConfirm = { name ->
                    onCreatePlaylistFolder(
                        name,
                        parentId,
                    )
                    createFolderParentId = null
                },
                onDismiss = {
                    createFolderParentId = null
                },
            )
        }

        renameFolder?.let { folder ->
            PlaylistFolderNameDialog(
                title = "Rename folder",
                initialName = folder.name,
                confirmLabel = "Rename",
                existingNames =
                    (
                        state.playlistFolders
                            .filter {
                                it.id != folder.id &&
                                    it.parentId ==
                                        folder.parentId
                            }
                            .map {
                                it.name
                            } +
                        state.playlists
                            .filter {
                                it.folderId ==
                                    folder.parentId
                            }
                            .map {
                                it.title
                            }
                    ).toSet(),
                onConfirm = { name ->
                    onRenamePlaylistFolder(
                        folder.id,
                        name,
                    )
                    renameFolder = null
                },
                onDismiss = {
                    renameFolder = null
                },
            )
        }

        moveFolder?.let { folder ->
            MovePlaylistFolderTreeDialog(
                folder = folder,
                folders = state.playlistFolders,
                onMove = { parentId ->
                    onMovePlaylistFolder(
                        folder.id,
                        parentId,
                    )
                    moveFolder = null
                },
                onDismiss = {
                    moveFolder = null
                },
            )
        }

        movePlaylist?.let { playlist ->
            MovePlaylistFolderDialog(
                playlist = playlist,
                folders = state.playlistFolders,
                onMove = { folderId ->
                    onMovePlaylistToFolder(
                        playlist.id,
                        folderId,
                    )
                    movePlaylist = null
                },
                onDismiss = {
                    movePlaylist = null
                },
            )
        }

        if (showCreatePlaylist) {
            val targetFolderId =
                if (
                    filter ==
                        LibraryFilter.PLAYLISTS
                ) {
                    currentPlaylistFolderId
                } else {
                    ""
                }

            val existingNames =
                (
                    state.playlists
                        .filter {
                            it.folderId ==
                                targetFolderId
                        }
                        .map {
                            it.title
                        } +
                    state.playlistFolders
                        .filter {
                            it.parentId ==
                                targetFolderId
                        }
                        .map {
                            it.name
                        }
                ).toSet()

            CreatePlaylistDialog(
                existingNames = existingNames,
                onCreate = { name ->
                    onCreatePlaylist(
                        name,
                        targetFolderId,
                    )

                    showCreatePlaylist = false
                },
                onDismiss = {
                    showCreatePlaylist = false
                },
            )
        }

        Spacer(Modifier.height(12.dp))

        LazyRow(
            state = filterRowState,
            contentPadding =
                PaddingValues(
                    horizontal = 16.dp
                ),
            horizontalArrangement =
                Arrangement.spacedBy(8.dp),
        ) {
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
                FilterChip(
                    f.label,
                    selected =
                        f == pagerDisplayFilter,
                ) {
                    onFilter(f)
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        // library-context-controls
        if (
            showSortControl ||
            supportsLayoutToggle
        ) {
            var sortMenu by
                remember {
                    mutableStateOf(false)
                }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                if (showSortControl) {
                    Box {
                        Row(
                            Modifier
                                .clip(
                                    RoundedCornerShape(50)
                                )
                                .clickable {
                                    sortMenu = true
                                }
                                .padding(6.dp),
                            verticalAlignment =
                                Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.SwapVert,
                                null,
                                modifier =
                                    Modifier.size(18.dp),
                            )

                            Spacer(
                                Modifier.width(6.dp)
                            )

                            Text(
                                displayedSort.label,
                                style =
                                    MaterialTheme.typography
                                        .labelLarge,
                                fontWeight =
                                    FontWeight.Medium,
                            )
                        }

                        DropdownMenu(
                            expanded = sortMenu,
                            onDismissRequest = {
                                sortMenu = false
                            },
                        ) {
                            availableSorts.forEach {
                                option ->

                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            option.label
                                        )
                                    },
                                    onClick = {
                                        onSort(option)
                                        sortMenu = false
                                    },
                                    trailingIcon = {
                                        if (
                                            option ==
                                            displayedSort
                                        ) {
                                            Icon(
                                                Icons.Filled.Check,
                                                null,
                                                tint =
                                                    MaterialTheme
                                                        .colorScheme
                                                        .primary,
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                }

                Spacer(
                    Modifier.weight(1f)
                )

                if (supportsLayoutToggle) {
                    Icon(
                        imageVector =
                            if (
                                layout ==
                                LibraryLayout.LIST
                            ) {
                                Icons.Filled.GridView
                            } else {
                                Icons.AutoMirrored
                                    .Filled.List
                            },
                        contentDescription =
                            "Toggle layout",
                        modifier =
                            Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .clickable(
                                    onClick =
                                        onToggleLayout
                                )
                                .padding(8.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        val bottom = contentPadding.calculateBottomPadding() + 24.dp




        androidx.compose.foundation.pager.HorizontalPager(
            state = categoryPagerState,
            modifier =
                Modifier
                    .fillMaxSize()
                    /*
                     * HorizontalPager owns horizontal drag
                     * consumption. This naturally cancels:
                     *
                     * - vertical list scrolling once horizontal
                     *   paging wins the gesture;
                     * - clicks on playlists/folders after a drag.
                     *
                     * This observer never consumes events. It only
                     * handles the special gesture beyond the first
                     * page: All -> drawer.
                     */
                    .pointerInput(
                        visibleFilters,
                    ) {
                        awaitEachGesture {
                            val down =
                                awaitFirstDown(
                                    requireUnconsumed =
                                        false
                                )

                            /*
                             * Capture page at finger-down.
                             *
                             * Critical: a swipe
                             * Playlists -> All must NOT also open
                             * the drawer when currentPage becomes
                             * zero halfway through that same drag.
                             */
                            val pageAtDown =
                                categoryPagerState
                                    .currentPage

                            var last =
                                down.position

                            while (true) {
                                val event =
                                    awaitPointerEvent()

                                val change =
                                    event.changes
                                        .firstOrNull {
                                            it.id ==
                                                down.id
                                        }
                                        ?: break

                                last =
                                    change.position

                                if (!change.pressed) {
                                    break
                                }
                            }

                            if (pageAtDown != 0) {
                                return@awaitEachGesture
                            }

                            val dx =
                                last.x -
                                    down.position.x

                            val dy =
                                last.y -
                                    down.position.y

                            if (
                                dx >
                                    drawerEdgeThreshold &&
                                abs(dx) >
                                    abs(dy) * 1.2f
                            ) {
                                onOpenDrawer()
                            }
                        }
                    },
        ) { page ->
            /*
             * Each page renders its real neighboring Library
             * category. Therefore the next/previous content is
             * visible before the finger is released.
             */
            val filter =
                visibleFilters[page]



        if (state.loading && state.albums.isEmpty() && state.playlists.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                com.mentality.sonethyst.ui.components.LottieLoader(modifier = Modifier.size(72.dp))
            }
            return@HorizontalPager
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
            return@HorizontalPager
        }

        if (
            filter == LibraryFilter.GENRES &&
            !state.genresLoading &&
            state.genres.isEmpty()
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment =
                    Alignment.Center,
            ) {
                Text(
                    "No genres found",
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                )
            }

            return@HorizontalPager
        }

        if (filter == LibraryFilter.PLAYLISTS) {
            // playlist-folder-browser

            val currentFolder =
                state.playlistFolders
                    .firstOrNull {
                        it.id ==
                            currentPlaylistFolderId
                    }

            val childFolders =
                state.playlistFolders
                    .filter {
                        it.parentId ==
                            currentPlaylistFolderId
                    }

            val folderPlaylists =
                state.playlists
                    .filter {
                        it.folderId ==
                            currentPlaylistFolderId
                    }

            Column(
                Modifier.fillMaxSize()
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                start = 14.dp,
                                end = 8.dp,
                                top = 2.dp,
                                bottom = 6.dp,
                            ),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    if (currentFolder != null) {
                        Icon(
                            imageVector =
                                Icons.Filled.ArrowBack,
                            contentDescription =
                                "Parent folder",
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        currentPlaylistFolderId =
                                            currentFolder
                                                .parentId
                                    }
                                    .padding(8.dp),
                        )

                        Spacer(
                            Modifier.width(4.dp)
                        )
                    }

                    Column(
                        Modifier.weight(1f)
                    ) {
                        Text(
                            text =
                                currentFolder?.name
                                    ?: "Playlists",
                            style =
                                MaterialTheme.typography
                                    .titleMedium,
                            fontWeight =
                                FontWeight.Bold,
                            maxLines = 1,
                            overflow =
                                TextOverflow.Ellipsis,
                        )

                        if (currentFolder != null) {
                            Text(
                                text =
                                    "${childFolders.size} folders • " +
                                        "${folderPlaylists.size} playlists",
                                style =
                                    MaterialTheme.typography
                                        .labelSmall,
                                color =
                                    MaterialTheme
                                        .colorScheme
                                        .onSurfaceVariant,
                            )
                        }
                    }

                }

                if (
                    childFolders.isEmpty() &&
                    folderPlaylists.isEmpty()
                ) {
                    Box(
                        modifier =
                            Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        Text(
                            text =
                                if (currentFolder == null) {
                                    "No playlists or folders yet"
                                } else {
                                    "This folder is empty"
                                },
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                        )
                    }

                    return@HorizontalPager
                }

                if (
                    layout ==
                    LibraryLayout.LIST
                ) {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 8.dp
                                ),
                        contentPadding =
                            PaddingValues(
                                bottom = bottom
                            ),
                    ) {
                        items(
                            count =
                                childFolders.size,
                            key = { i ->
                                "playlist-folder:" +
                                    childFolders[i].id
                            },
                            contentType = {
                                "playlist-folder"
                            },
                        ) { i ->
                            PlaylistFolderRow(
                                folder =
                                    childFolders[i],
                                onOpen = {
                                    currentPlaylistFolderId =
                                        childFolders[i].id
                                },
                                onRename = {
                                    renameFolder =
                                        childFolders[i]
                                },
                                onMove = {
                                    moveFolder =
                                        childFolders[i]
                                },
                                onDelete = {
                                    onDeletePlaylistFolder(
                                        childFolders[i].id
                                    )
                                },
                            )
                        }
    
                        items(
                            count =
                                folderPlaylists.size,
                            key = { i ->
                                "folder-playlist:" +
                                    folderPlaylists[i].id
                            },
                            contentType = {
                                "playlist"
                            },
                        ) { i ->
                            val playlist =
                                folderPlaylists[i]
    
                            PlaylistFolderPlaylistRow(
                                playlist = playlist,
                                onOpen = {
                                    onOpenDetail(
                                        "playlist",
                                        playlist.id,
                                    )
                                },
                                onMove = {
                                    movePlaylist =
                                        playlist
                                },
                                onDelete = {
                                    onDeletePlaylist(
                                        playlist.id
                                    )
                                },
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns =
                            GridCells.Fixed(
                                libColumns
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp
                                ),
                        contentPadding =
                            PaddingValues(
                                bottom = bottom
                            ),
                        horizontalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            ),
                        verticalArrangement =
                            Arrangement.spacedBy(
                                10.dp
                            ),
                    ) {
                        items(
                            count =
                                childFolders.size,
                            key = { i ->
                                "playlist-folder-grid:" +
                                    childFolders[i].id
                            },
                            contentType = {
                                "playlist-folder-grid"
                            },
                        ) { i ->
                            val folder =
                                childFolders[i]

                            PlaylistFolderGridItem(
                                folder = folder,
                                onOpen = {
                                    currentPlaylistFolderId =
                                        folder.id
                                },
                                onRename = {
                                    renameFolder =
                                        folder
                                },
                                onMove = {
                                    moveFolder =
                                        folder
                                },
                                onDelete = {
                                    onDeletePlaylistFolder(
                                        folder.id
                                    )
                                },
                            )
                        }

                        items(
                            count =
                                folderPlaylists.size,
                            key = { i ->
                                "playlist-grid:" +
                                    folderPlaylists[i].id
                            },
                            contentType = {
                                "playlist-grid"
                            },
                        ) { i ->
                            val playlist =
                                folderPlaylists[i]

                            PlaylistGridItem(
                                playlist =
                                    playlist,
                                onOpen = {
                                    onOpenDetail(
                                        "playlist",
                                        playlist.id,
                                    )
                                },
                                onMove = {
                                    movePlaylist =
                                        playlist
                                },
                                onDelete = {
                                    onDeletePlaylist(
                                        playlist.id
                                    )
                                },
                            )
                        }
                    }
                }
            }

            return@HorizontalPager
        }

        if (filter == LibraryFilter.VERSIONS) {
            when {
                state.versionsLoading &&
                    state.versionGroups.isEmpty() -> {
                    Box(
                        Modifier.fillMaxSize(),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        com.mentality.sonethyst.ui.components
                            .LottieLoader(
                                modifier =
                                    Modifier.size(72.dp)
                            )
                    }
                }

                state.versionGroups.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 28.dp),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        Text(
                            "No alternate song versions found",
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    val versionCount =
                        state.versionGroups.sumOf {
                            it.versions.size
                        }

                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp),
                        contentPadding =
                            PaddingValues(
                                bottom = bottom
                            ),
                    ) {
                        item(
                            key = "versions-summary",
                            contentType =
                                "versions-summary",
                        ) {
                            Text(
                                text =
                                    "${state.versionGroups.size} song " +
                                        "group${if (state.versionGroups.size == 1) "" else "s"}" +
                                        " • $versionCount versions",
                                style =
                                    MaterialTheme.typography
                                        .bodySmall,
                                color =
                                    MaterialTheme.colorScheme
                                        .onSurfaceVariant,
                                modifier =
                                    Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 6.dp,
                                    ),
                            )
                        }

                        items(
                            count =
                                state.versionGroups.size,
                            key = { i ->
                                val group =
                                    state.versionGroups[i]

                                "versions:" +
                                    group.artist +
                                    ":" +
                                    group.title
                            },
                            contentType = {
                                "song-version-group"
                            },
                        ) { i ->
                            SongVersionGroupCard(
                                group =
                                    state.versionGroups[i],
                                currentSongId =
                                    currentSongId,
                                isPlaying =
                                    isPlaying,
                                onPlay = { song ->
                                    onPlayAll(
                                        listOf(song),
                                        0,
                                    )
                                },
                            )
                        }
                    }
                }
            }

            return@HorizontalPager
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
                        onEditCustomTags =
                            onEditCustomTags?.let { cb ->
                                { cb(s) }
                            },
                        onHide =
                            onHideSong?.let { cb ->
                                { cb(s) }
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
            return@HorizontalPager
        }

        if (filter == LibraryFilter.HIDDEN) {
            val hidden = state.hiddenItems

            if (hidden.isEmpty()) {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "No hidden tracks or albums",
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                    )
                }
                return@HorizontalPager
            }

            LazyColumn(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                contentPadding =
                    PaddingValues(bottom = bottom),
            ) {
                items(
                    count = hidden.size,
                    key = { i -> hidden[i].key },
                    contentType = { "hidden-library-item" },
                ) { i ->
                    HiddenLibraryRow(
                        item = hidden[i],
                        onRestore = {
                            onRestoreHidden(hidden[i])
                        },
                    )
                }
            }

            return@HorizontalPager
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
                return@HorizontalPager
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
            return@HorizontalPager
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

            LibraryFilter.TAGS -> remember(
                state.customTags,
                filter,
                sort,
            ) {
                buildRows(state, filter, sort, pins)
            }

            else -> emptyList()
        }

        if (
            filter == LibraryFilter.TAGS &&
            rows.isEmpty()
        ) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "No custom tags yet",
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                )
            }
            return@HorizontalPager
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
                        hideable = true,
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
                hideable = true,
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

        LibraryFilter.TAGS -> state.customTags.map {
            LibRow(
                it.name,
                "Tag • ${it.songCount} song${if (it.songCount == 1) "" else "s"}",
                "",
                accentFor("tag:${it.name}"),
                it.name,
                "tag",
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
        filter == LibraryFilter.GENRES ||
        filter == LibraryFilter.TAGS
    ) {
        return sorted
    }

    val visibleAlbumIds =
        state.albums
            .mapTo(mutableSetOf()) { it.id }

    val displayPins =
        pins.filterNot {
            it.kind == "album" &&
                it.id !in visibleAlbumIds
        }

    val pinned: Set<Pair<String, String>> =
        if (displayPins.isEmpty()) {
            emptySet()
        } else {
            displayPins.mapTo(mutableSetOf()) {
                it.kind to it.id
            }
        }

    val deduped = if (pinned.isEmpty()) {
        sorted
    } else {
        sorted.filterNot { (it.kind to it.id) in pinned }
    }

    return buildList<LibRow> {
        if (filter == LibraryFilter.ALL) {
            displayPins.forEach {
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
private fun LibraryCollectionArtwork(
    row: LibRow,
    modifier: Modifier,
) {
    val virtualIcon =
        when (row.kind) {
            "folders" ->
                Icons.Filled.Folder

            "radio" ->
                Icons.Filled.Radio

            "podcasts" ->
                Icons.Filled.Podcasts

            else ->
                null
        }

    if (virtualIcon != null) {
        Box(
            modifier =
                modifier
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme
                            .primaryContainer
                    ),
            contentAlignment =
                Alignment.Center,
        ) {
            Icon(
                imageVector =
                    virtualIcon,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme
                        .onPrimaryContainer,
                modifier =
                    Modifier.fillMaxSize(0.48f),
            )
        }
    } else {
        Artwork(
            row.art,
            row.accent,
            modifier,
            corner =
                if (row.circle) {
                    200.dp
                } else {
                    14.dp
                },
        )
    }
}

@Composable
private fun SongVersionGroupCard(
    group: SongVersionGroup,
    currentSongId: String,
    isPlaying: Boolean,
    onPlay: (Song) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 5.dp)
                .clip(
                    RoundedCornerShape(18.dp)
                )
                .background(
                    MaterialTheme.colorScheme
                        .surfaceContainerHigh
                        .copy(alpha = 0.45f)
                )
                .padding(12.dp),
    ) {
        Text(
            text = group.title,
            style =
                MaterialTheme.typography
                    .titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Text(
            text =
                "${group.artist} • " +
                    "${group.versions.size} " +
                    "version${if (group.versions.size == 1) "" else "s"}",
            style =
                MaterialTheme.typography
                    .bodySmall,
            color =
                MaterialTheme.colorScheme
                    .onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(
            Modifier.height(8.dp)
        )

        group.versions.forEach { version ->
            val song = version.song

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            onPlay(song)
                        }
                        .padding(6.dp),
                verticalAlignment =
                    Alignment.CenterVertically,
            ) {
                Artwork(
                    song.artworkUrl,
                    song.accent,
                    Modifier.size(46.dp),
                    corner = 10.dp,
                )

                Spacer(
                    Modifier.width(10.dp)
                )

                Column(
                    Modifier.weight(1f)
                ) {
                    Text(
                        text = version.label,
                        style =
                            MaterialTheme.typography
                                .bodySmall,
                        fontWeight =
                            FontWeight.Bold,
                        color =
                            if (version.isOriginal) {
                                MaterialTheme
                                    .colorScheme
                                    .primary
                            } else {
                                MaterialTheme
                                    .colorScheme
                                    .onSurface
                            },
                        maxLines = 1,
                        overflow =
                            TextOverflow.Ellipsis,
                    )

                    Text(
                        text = songVersionSpec(song),
                        style =
                            MaterialTheme.typography
                                .labelSmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        maxLines = 2,
                        overflow =
                            TextOverflow.Ellipsis,
                    )
                }

                if (
                    isPlaying &&
                    song.id == currentSongId
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.PlayArrow,
                        contentDescription =
                            "Playing",
                        tint =
                            MaterialTheme.colorScheme
                                .primary,
                        modifier =
                            Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

private fun songVersionSpec(
    song: Song,
): String {
    val parts =
        mutableListOf<String>()

    if (song.album.isNotBlank()) {
        parts += song.album
    }

    if (song.suffix.isNotBlank()) {
        parts += song.suffix.uppercase()
    }

    if (song.durationSec > 0) {
        val minutes =
            song.durationSec / 60

        val seconds =
            song.durationSec % 60

        parts +=
            "$minutes:" +
                seconds
                    .toString()
                    .padStart(
                        2,
                        '0',
                    )
    }

    return parts
        .joinToString(" • ")
        .ifBlank {
            "Version information unavailable"
        }
}

@Composable
private fun PlaylistFolderGridItem(
    folder: PlaylistFolder,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember {
        mutableStateOf(false)
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .clickable(
                    onClick = onOpen
                )
                .padding(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        MaterialTheme
                            .colorScheme
                            .primaryContainer
                    ),
            contentAlignment =
                Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Folder,
                null,
                tint =
                    MaterialTheme.colorScheme
                        .onPrimaryContainer,
                modifier =
                    Modifier.size(54.dp),
            )
        }

        Spacer(
            Modifier.height(8.dp)
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    folder.name,
                    style =
                        MaterialTheme.typography
                            .titleSmall,
                    fontWeight =
                        FontWeight.Bold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis,
                )

                Text(
                    "Folder",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                )
            }

            Box {
                Icon(
                    Icons.Filled.MoreVert,
                    "More",
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                menuOpen = true
                            }
                            .padding(5.dp),
                )

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = {
                        menuOpen = false
                    },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("Rename")
                        },
                        onClick = {
                            menuOpen = false
                            onRename()
                        },
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Move folder")
                        },
                        onClick = {
                            menuOpen = false
                            onMove()
                        },
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Delete folder")
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistGridItem(
    playlist: Playlist,
    onOpen: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember {
        mutableStateOf(false)
    }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .clickable(
                    onClick = onOpen
                )
                .padding(6.dp),
    ) {
        Artwork(
            playlist.coverUrl,
            playlist.accent,
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            corner = 14.dp,
        )

        Spacer(
            Modifier.height(8.dp)
        )

        Row(
            verticalAlignment =
                Alignment.CenterVertically,
        ) {
            Column(
                Modifier.weight(1f)
            ) {
                Text(
                    playlist.title,
                    style =
                        MaterialTheme.typography
                            .titleSmall,
                    fontWeight =
                        FontWeight.Bold,
                    maxLines = 1,
                    overflow =
                        TextOverflow.Ellipsis,
                )

                Text(
                    "${playlist.songCount} songs",
                    style =
                        MaterialTheme.typography
                            .bodySmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                )
            }

            Box {
                Icon(
                    Icons.Filled.MoreVert,
                    "More",
                    modifier =
                        Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable {
                                menuOpen = true
                            }
                            .padding(5.dp),
                )

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = {
                        menuOpen = false
                    },
                ) {
                    DropdownMenuItem(
                        text = {
                            Text("Move to folder")
                        },
                        onClick = {
                            menuOpen = false
                            onMove()
                        },
                    )

                    DropdownMenuItem(
                        text = {
                            Text("Delete playlist")
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistFolderRow(
    folder: PlaylistFolder,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by
        remember {
            mutableStateOf(false)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                )
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .background(
                    MaterialTheme.colorScheme
                        .surfaceContainerHigh
                        .copy(alpha = 0.45f)
                )
                .clickable(
                    onClick = onOpen
                )
                .padding(8.dp),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(
                        RoundedCornerShape(14.dp)
                    )
                    .background(
                        MaterialTheme.colorScheme
                            .primaryContainer
                    ),
            contentAlignment =
                Alignment.Center,
        ) {
            Icon(
                imageVector =
                    Icons.Filled.Folder,
                contentDescription = null,
                tint =
                    MaterialTheme.colorScheme
                        .onPrimaryContainer,
                modifier =
                    Modifier.size(30.dp),
            )
        }

        Spacer(
            Modifier.width(14.dp)
        )

        Column(
            Modifier.weight(1f)
        ) {
            Text(
                text = folder.name,
                style =
                    MaterialTheme.typography
                        .titleSmall,
                fontWeight =
                    FontWeight.Bold,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
            )

            Text(
                text = "Playlist folder",
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
            )
        }

        Box {
            Icon(
                imageVector =
                    Icons.Filled.MoreVert,
                contentDescription = "More",
                tint =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            menuOpen = true
                        }
                        .padding(7.dp),
            )

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = {
                    menuOpen = false
                },
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Rename")
                    },
                    onClick = {
                        menuOpen = false
                        onRename()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Edit,
                            null,
                        )
                    },
                )

                DropdownMenuItem(
                    text = {
                        Text("Move folder")
                    },
                    onClick = {
                        menuOpen = false
                        onMove()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.DriveFileMove,
                            null,
                        )
                    },
                )

                DropdownMenuItem(
                    text = {
                        Text("Delete folder")
                    },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaylistFolderPlaylistRow(
    playlist: Playlist,
    onOpen: () -> Unit,
    onMove: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by
        remember {
            mutableStateOf(false)
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                )
                .clip(
                    RoundedCornerShape(16.dp)
                )
                .background(
                    MaterialTheme.colorScheme
                        .surfaceContainerHigh
                        .copy(alpha = 0.45f)
                )
                .clickable(
                    onClick = onOpen
                )
                .padding(8.dp),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Artwork(
            playlist.coverUrl,
            playlist.accent,
            Modifier.size(56.dp),
            corner = 14.dp,
        )

        Spacer(
            Modifier.width(14.dp)
        )

        Column(
            Modifier.weight(1f)
        ) {
            Text(
                text = playlist.title,
                style =
                    MaterialTheme.typography
                        .titleSmall,
                fontWeight =
                    FontWeight.Bold,
                maxLines = 1,
                overflow =
                    TextOverflow.Ellipsis,
            )

            Text(
                text =
                    "${playlist.songCount} song" +
                        if (
                            playlist.songCount == 1
                        ) {
                            ""
                        } else {
                            "s"
                        },
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
            )
        }

        Box {
            Icon(
                imageVector =
                    Icons.Filled.MoreVert,
                contentDescription = "More",
                tint =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .clickable {
                            menuOpen = true
                        }
                        .padding(7.dp),
            )

            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = {
                    menuOpen = false
                },
            ) {
                DropdownMenuItem(
                    text = {
                        Text("Move to folder")
                    },
                    onClick = {
                        menuOpen = false
                        onMove()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.DriveFileMove,
                            null,
                        )
                    },
                )

                DropdownMenuItem(
                    text = {
                        Text("Delete playlist")
                    },
                    onClick = {
                        menuOpen = false
                        onDelete()
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            null,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun PlaylistFolderNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    existingNames: Set<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by
        remember(
            initialName
        ) {
            mutableStateOf(
                initialName
            )
        }

    val duplicate =
        remember(
            name,
            existingNames,
        ) {
            val candidate =
                libraryCollectionNameKey(
                    name
                )

            candidate.isNotBlank() &&
                existingNames.any {
                    libraryCollectionNameKey(
                        it
                    ) == candidate
                }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title)
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                label = {
                    Text("Folder name")
                },
                singleLine = true,
                isError = duplicate,
                supportingText =
                    if (duplicate) {
                        {
                            Text(
                                "A playlist or folder with this name already exists here"
                            )
                        }
                    } else {
                        null
                    },
            )
        },
        confirmButton = {
            TextButton(
                enabled =
                    name.trim()
                        .isNotBlank() &&
                        !duplicate,
                onClick = {
                    val safe =
                        name.trim()

                    if (safe.isNotBlank()) {
                        onConfirm(safe)
                    }
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun MovePlaylistFolderTreeDialog(
    folder: PlaylistFolder,
    folders: List<PlaylistFolder>,
    onMove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val blockedIds =
        remember(
            folder.id,
            folders,
        ) {
            playlistFolderDescendantIds(
                folder.id,
                folders,
            ) + folder.id
        }

    val destinations =
        folders.filter {
            it.id !in blockedIds
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Move ${folder.name}"
            )
        },
        text = {
            LazyColumn {
                item(
                    key = "move-folder-root"
                ) {
                    Text(
                        text = "Playlists",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        12.dp
                                    )
                                )
                                .clickable {
                                    onMove("")
                                }
                                .padding(12.dp),
                        fontWeight =
                            FontWeight.Bold,
                    )
                }

                items(
                    count =
                        destinations.size,
                    key = { i ->
                        "move-folder-destination:" +
                            destinations[i].id
                    },
                ) { i ->
                    val destination =
                        destinations[i]

                    Text(
                        text =
                            playlistFolderDisplayName(
                                destination,
                                folders,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        12.dp
                                    )
                                )
                                .clickable {
                                    onMove(
                                        destination.id
                                    )
                                }
                                .padding(12.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
    )
}

private fun playlistFolderDescendantIds(
    folderId: String,
    folders: List<PlaylistFolder>,
): Set<String> {
    val result =
        mutableSetOf<String>()

    var frontier =
        setOf(folderId)

    while (frontier.isNotEmpty()) {
        val next =
            folders
                .filter {
                    it.parentId in frontier &&
                        it.id !in result
                }
                .mapTo(
                    mutableSetOf()
                ) {
                    it.id
                }

        result += next
        frontier = next
    }

    return result
}

@Composable
private fun MovePlaylistFolderDialog(
    playlist: Playlist,
    folders: List<PlaylistFolder>,
    onMove: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Move ${playlist.title}"
            )
        },
        text = {
            LazyColumn {
                item(
                    key = "playlist-folder-root"
                ) {
                    Text(
                        text = "Playlists",
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        12.dp
                                    )
                                )
                                .clickable {
                                    onMove("")
                                }
                                .padding(12.dp),
                        fontWeight =
                            FontWeight.Bold,
                    )
                }

                items(
                    count = folders.size,
                    key = { i ->
                        "move-folder:" +
                            folders[i].id
                    },
                ) { i ->
                    val folder =
                        folders[i]

                    Text(
                        text =
                            playlistFolderDisplayName(
                                folder,
                                folders,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(
                                    RoundedCornerShape(
                                        12.dp
                                    )
                                )
                                .clickable {
                                    onMove(
                                        folder.id
                                    )
                                }
                                .padding(12.dp),
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
    )
}

private fun libraryCollectionNameKey(
    value: String,
): String =
    value
        .trim()
        .replace(
            Regex("""\s+"""),
            " ",
        )
        .lowercase(
            java.util.Locale.ROOT
        )

private fun playlistFolderDisplayName(
    folder: PlaylistFolder,
    folders: List<PlaylistFolder>,
): String {
    val names =
        mutableListOf<String>()

    var current:
        PlaylistFolder? =
        folder

    val visited =
        mutableSetOf<String>()

    while (
        current != null &&
        current.id !in visited
    ) {
        visited += current.id
        names += current.name

        current =
            folders.firstOrNull {
                it.id ==
                    current.parentId
            }
    }

    return names
        .asReversed()
        .joinToString(" / ")
}

@Composable
private fun HiddenLibraryRow(
    item: com.mentality.sonethyst.data.HiddenLibraryItem,
    onRestore: () -> Unit,
) {
    val subtitle =
        if (item.kind == "album") {
            "Album • " +
                item.subtitle.removePrefix("Album • ")
        } else {
            "Track • ${item.subtitle}"
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 8.dp,
                    vertical = 4.dp,
                )
                .clip(RoundedCornerShape(16.dp))
                .background(
                    MaterialTheme.colorScheme
                        .surfaceContainerHigh
                        .copy(alpha = 0.45f)
                )
                .padding(8.dp),
        verticalAlignment =
            Alignment.CenterVertically,
    ) {
        Artwork(
            item.artworkUrl,
            accentFor(item.key),
            Modifier.size(56.dp),
            corner = 14.dp,
        )

        Spacer(Modifier.width(14.dp))

        Column(
            Modifier.weight(1f)
        ) {
            Text(
                item.title,
                style =
                    MaterialTheme.typography
                        .titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(2.dp))

            Text(
                subtitle,
                style =
                    MaterialTheme.typography
                        .bodySmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        TextButton(
            onClick = onRestore,
        ) {
            Text("Restore")
        }
    }
}

@Composable
private fun CreatePlaylistDialog(
    existingNames: Set<String>,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember {
        mutableStateOf("")
    }

    val duplicate =
        remember(
            name,
            existingNames,
        ) {
            val candidate =
                libraryCollectionNameKey(
                    name
                )

            candidate.isNotBlank() &&
                existingNames.any {
                    libraryCollectionNameKey(
                        it
                    ) == candidate
                }
        }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "New playlist",
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                },
                label = {
                    Text("Playlist name")
                },
                singleLine = true,
                isError = duplicate,
                supportingText =
                    if (duplicate) {
                        {
                            Text(
                                "A playlist or folder with this name already exists here"
                            )
                        }
                    } else {
                        null
                    },
            )
        },
        confirmButton = {
            TextButton(
                enabled =
                    name.trim()
                        .isNotBlank() &&
                        !duplicate,
                onClick = {
                    val safe =
                        name.trim()

                    if (safe.isNotBlank()) {
                        onCreate(safe)
                    }
                },
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
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
        LibraryCollectionArtwork(
            row = row,
            modifier = Modifier.size(56.dp),
        )
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
        LibraryCollectionArtwork(
            row = row,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f),
        )
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
        if (
            row.hideable &&
            row.kind == "album"
        ) {
            DropdownMenuItem(
                text = { Text("Hide album") },
                onClick = {
                    onDismiss()
                    actions.onHide(row)
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.VisibilityOff,
                        null,
                    )
                },
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
