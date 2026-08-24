package com.mentality.sonethyst.ui.screens.detail

import android.content.Intent

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.ui.components.Artwork
import com.mentality.sonethyst.ui.components.Eyebrow
import com.mentality.sonethyst.ui.components.PlaylistCoverSheet
import com.mentality.sonethyst.ui.components.SectionHeader
import com.mentality.sonethyst.ui.components.SongRow
import com.mentality.sonethyst.viewmodel.DetailUiState

@Composable
fun DetailScreen(
    contentPadding: PaddingValues,
    state: DetailUiState,
    likedIds: Set<String>,
    currentSongId: String,
    isPlaying: Boolean,
    onBack: () -> Unit,
    onPlayAll: (List<Song>, Int) -> Unit,
    onShufflePlay: (List<Song>) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onRemoveFromPlaylist: ((Song) -> Unit)? = null,
    onRemoveSelectedFromPlaylist: ((List<Song>) -> Unit)? = null,
    onAddSelectedToQueue: ((List<Song>) -> Unit)? = null,
    onAddSelectedToPlaylist: ((List<Song>) -> Unit)? = null,
    onDownloadSelected: ((List<Song>) -> Unit)? = null,
    onSetSelectedLiked: ((List<Song>, Boolean) -> Unit)? = null,
    onReorderPlaylist: ((List<Song>) -> Unit)? = null,
    onReorderGrab: (() -> Unit)? = null,
    onToggleLike: (String) -> Unit,
    onOpenDetail: (String, String) -> Unit,
    itemKind: String,
    isItemLiked: Boolean,
    onToggleItemLike: () -> Unit,
    downloadedIds: Set<String>,
    onDownload: (Song) -> Unit,
    onRemoveDownload: (String) -> Unit,
    onDownloadAll: () -> Unit,
    onRemoveDownloads: () -> Unit,
    onEditPlaylist: (String, String) -> Unit,
    onAddSongsToPlaylist: (() -> Unit)? = null,
    onSetPlaylistCover: ((String, String?) -> Unit)? = null,
    onDeletePlaylist: () -> Unit,
    onLoadMore: () -> Unit = {},
    canDownload: Boolean = true,
    isPinned: Boolean = false,
    onTogglePin: () -> Unit = {},
    onEditTags: ((Song) -> Unit)? = null,
    serverTagEditing: Boolean = false,
    artistInfo: com.mentality.sonethyst.data.remote.ArtistInfo? = null,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var headerMenu by remember { mutableStateOf(false) }
    var showEdit by remember { mutableStateOf(false) }
    var showCoverSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val customCoverLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }

            onSetPlaylistCover?.invoke(
                "custom",
                uri.toString(),
            )

            showCoverSheet = false
        }
    }
    var query by remember { mutableStateOf("") }
    var searchOpen by remember { mutableStateOf(false) }
    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val data = state.data

    if (data == null) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Row(Modifier.fillMaxWidth().padding(top = topInset + 6.dp, start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack).padding(8.dp))
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (state.loading) com.mentality.sonethyst.ui.components.LottieLoader(modifier = Modifier.size(72.dp))
                else Text("Couldn't load", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }

    val info = data.info
    val tracks = data.tracks

    val playlistOrder = remember {
        androidx.compose.runtime.mutableStateListOf<Song>()
    }

    androidx.compose.runtime.LaunchedEffect(tracks.map { it.id }) {
        if (playlistOrder.map { it.id } != tracks.map { it.id }) {
            playlistOrder.clear()
            playlistOrder.addAll(tracks)
        }
    }

    var selectedTrackIds by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    val selectionMode = selectedTrackIds.isNotEmpty()
    var selectionMenuOpen by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(tracks.map { it.id }) {
        val validIds = tracks.map { it.id }.toSet()
        selectedTrackIds = selectedTrackIds.intersect(validIds)
    }

    var draggingSongId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    val reorderStepPx = with(density) { 68.dp.toPx() }
    // artists often lack a server image fall back to enriched wiki photo
    val effectiveArt = info.artUrl.ifBlank { artistInfo?.imageUrl.orEmpty() }
    val accent by com.mentality.sonethyst.util.rememberDominantColor(effectiveArt, info.accent)

    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(listState, state.canLoadMore) {
        androidx.compose.runtime.snapshotFlow {
            val li = listState.layoutInfo
            (li.visibleItemsInfo.lastOrNull()?.index ?: 0) to li.totalItemsCount
        }.collect { (last, count) ->
            if (state.canLoadMore && count > 0 && last >= count - 6) onLoadMore()
        }
    }

    LazyColumn(
        Modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(420.dp)) {
                Artwork(effectiveArt, info.accent, Modifier.matchParentSize(), corner = 0.dp)
                Box(
                    Modifier.matchParentSize().background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0f to Color.Transparent,
                                0.42f to MaterialTheme.colorScheme.background.copy(alpha = 0.10f),
                                0.74f to MaterialTheme.colorScheme.background.copy(alpha = 0.72f),
                                1f to MaterialTheme.colorScheme.background,
                            )
                        )
                    )
                )
                Box(
                    Modifier.fillMaxWidth().height(130.dp).background(
                        Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent))
                    )
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = topInset + 6.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack).padding(8.dp))
                    Spacer(Modifier.weight(1f))
                    Box {
                        Icon(Icons.Filled.MoreVert, "More", tint = Color.White, modifier = Modifier.size(40.dp).clip(CircleShape).clickable { headerMenu = true }.padding(8.dp))
                        val isPlaylist = info.typeLabel.equals("Playlist", true)
                        DropdownMenu(expanded = headerMenu, onDismissRequest = { headerMenu = false }) {
                            DropdownMenuItem(text = { Text("Play") }, enabled = tracks.isNotEmpty(), onClick = { headerMenu = false; onPlayAll(tracks, 0) }, leadingIcon = { Icon(Icons.Filled.PlayArrow, null) })
                            DropdownMenuItem(text = { Text("Shuffle") }, enabled = tracks.isNotEmpty(), onClick = { headerMenu = false; onShufflePlay(tracks) }, leadingIcon = { Icon(Icons.Filled.Shuffle, null) })
                            DropdownMenuItem(text = { Text("Add all to queue") }, enabled = tracks.isNotEmpty(), onClick = { headerMenu = false; tracks.forEach { onAddToQueue(it) } }, leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) })
                            DropdownMenuItem(
                                text = { Text(if (isPinned) "Unpin from Library" else "Pin to Library") },
                                onClick = { headerMenu = false; onTogglePin() },
                                leadingIcon = { Icon(if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin, null) },
                            )
                            if (isPlaylist) {
                                if (onSetPlaylistCover != null) {
                                    DropdownMenuItem(
                                        text = { Text("Change cover") },
                                        onClick = {
                                            headerMenu = false
                                            showCoverSheet = true
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Image, null)
                                        },
                                    )
                                }

                                DropdownMenuItem(text = { Text("Edit playlist") }, onClick = { headerMenu = false; showEdit = true }, leadingIcon = { Icon(Icons.Filled.Edit, null) })
                                DropdownMenuItem(text = { Text("Delete playlist") }, onClick = { headerMenu = false; onDeletePlaylist() }, leadingIcon = { Icon(Icons.Filled.Delete, null, tint = MaterialTheme.colorScheme.error) })
                            }
                        }
                    }
                }
                Column(Modifier.align(Alignment.BottomStart).padding(start = 20.dp, end = 20.dp, bottom = 14.dp)) {
                    Eyebrow(info.typeLabel.uppercase(), accent)
                    Spacer(Modifier.height(6.dp))
                    Text(info.title, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    Text(info.subtitle, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val onAccent = if (accent.luminance() > 0.6f) Color.Black else Color.White
                Row(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.78f))))
                        .clickable(enabled = tracks.isNotEmpty()) { onPlayAll(tracks, 0) }
                        .padding(horizontal = 28.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.PlayArrow, "Play", tint = onAccent, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Play", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Black, color = onAccent)
                }
                Spacer(Modifier.width(10.dp))
                Icon(
                    Icons.Filled.Shuffle, "Shuffle",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(46.dp).clip(CircleShape).clickable(enabled = tracks.isNotEmpty()) { onShufflePlay(tracks) }.padding(11.dp),
                )
                Spacer(Modifier.weight(1f))
                if (itemKind == "album" || itemKind == "playlist" || itemKind == "artist") {
                    Icon(
                        if (isItemLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        if (isItemLiked) "Unlike" else "Like",
                        tint = if (isItemLiked) accent else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(44.dp).clip(CircleShape).clickable { onToggleItemLike() }.padding(9.dp),
                    )
                }
                if (canDownload) {
                    val allDownloaded = tracks.isNotEmpty() && tracks.all { downloadedIds.contains(it.id) }
                    Icon(
                        if (allDownloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                        if (allDownloaded) "Remove downloads" else "Download",
                        tint = if (allDownloaded) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(44.dp).clip(CircleShape).clickable {
                            if (allDownloaded) onRemoveDownloads() else onDownloadAll()
                        }.padding(9.dp),
                    )
                }
            }
        }

        if (info.isArtist && artistInfo != null &&
            (artistInfo.bio.isNotBlank() || artistInfo.tags.isNotEmpty() ||
                artistInfo.country.isNotBlank() || artistInfo.yearsActive.isNotBlank())
        ) {
            item { ArtistAbout(artistInfo, accent) }
        }

        if (info.isArtist && data.albums.isNotEmpty()) {
            item {
                SectionHeader("Albums", Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
                Spacer(Modifier.height(6.dp))
                androidx.compose.foundation.lazy.LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                    items(
                        count = data.albums.size,
                        key = { i -> "album:${data.albums[i].id}" },
                        contentType = { "album-card" },
                    ) { i ->
                        com.mentality.sonethyst.ui.components.AlbumCard(
                            data.albums[i],
                            onClick = {
                                onOpenDetail("album", data.albums[i].id)
                            },
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (selectionMode) {
                    Icon(
                        Icons.Filled.Close,
                        "Clear selection",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable { selectedTrackIds = emptySet() }
                            .padding(8.dp),
                    )

                    Spacer(Modifier.width(4.dp))

                    Text(
                        "${selectedTrackIds.size} selected",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )

                    Icon(
                        Icons.Filled.SelectAll,
                        "Select all",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .clickable {
                                selectedTrackIds =
                                    (if (
                                        itemKind == "playlist" &&
                                        onReorderPlaylist != null
                                    ) playlistOrder else tracks)
                                        .map { it.id }
                                        .toSet()
                            }
                            .padding(9.dp),
                    )

                    if (onAddSelectedToPlaylist != null) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            "Add selected to playlist",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val selected =
                                        (if (
                                            itemKind == "playlist" &&
                                            onReorderPlaylist != null
                                        ) playlistOrder else tracks).filter {
                                            it.id in selectedTrackIds
                                        }

                                    onAddSelectedToPlaylist(selected)
                                    selectedTrackIds = emptySet()
                                }
                                .padding(9.dp),
                        )
                    }

                    if (onAddSelectedToQueue != null) {
                        Icon(
                            Icons.AutoMirrored.Filled.QueueMusic,
                            "Add selected to queue",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val selected =
                                        (if (
                                            itemKind == "playlist" &&
                                            onReorderPlaylist != null
                                        ) playlistOrder else tracks).filter {
                                            it.id in selectedTrackIds
                                        }
                                    onAddSelectedToQueue(selected)
                                    selectedTrackIds = emptySet()
                                }
                                .padding(9.dp),
                        )
                    }

                    if (
                        itemKind == "playlist" &&
                        onRemoveSelectedFromPlaylist != null
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            "Remove selected from playlist",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable {
                                    val selected =
                                        (if (
                                            itemKind == "playlist" &&
                                            onReorderPlaylist != null
                                        ) playlistOrder else tracks).filter {
                                            it.id in selectedTrackIds
                                        }
                                    onRemoveSelectedFromPlaylist(selected)
                                    selectedTrackIds = emptySet()
                                }
                                .padding(9.dp),
                        )
                    }

                    if (
                        onDownloadSelected != null ||
                        onSetSelectedLiked != null
                    ) {
                        Box {
                            Icon(
                                Icons.Filled.MoreVert,
                                "More selected actions",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .clickable { selectionMenuOpen = true }
                                    .padding(9.dp),
                            )

                            DropdownMenu(
                                expanded = selectionMenuOpen,
                                onDismissRequest = { selectionMenuOpen = false },
                            ) {
                                val selected =
                                    (if (
                                        itemKind == "playlist" &&
                                        onReorderPlaylist != null
                                    ) playlistOrder else tracks).filter {
                                        it.id in selectedTrackIds
                                    }

                                if (onDownloadSelected != null) {
                                    DropdownMenuItem(
                                        text = { Text("Download selected") },
                                        onClick = {
                                            selectionMenuOpen = false
                                            onDownloadSelected(selected)
                                            selectedTrackIds = emptySet()
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Download, null)
                                        },
                                    )
                                }

                                if (onSetSelectedLiked != null) {
                                    val allLiked =
                                        selected.isNotEmpty() &&
                                        selected.all { it.id in likedIds }

                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                if (allLiked) {
                                                    "Remove selected from liked"
                                                } else {
                                                    "Add selected to liked"
                                                }
                                            )
                                        },
                                        onClick = {
                                            selectionMenuOpen = false
                                            onSetSelectedLiked(
                                                selected,
                                                !allLiked,
                                            )
                                            selectedTrackIds = emptySet()
                                        },
                                        leadingIcon = {
                                            Icon(
                                                if (allLiked) {
                                                    Icons.Filled.Favorite
                                                } else {
                                                    Icons.Filled.FavoriteBorder
                                                },
                                                null,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        if (info.isArtist) "Popular" else "Tracks",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )

                    if (tracks.size > 5) {
                        Icon(
                            if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                            if (searchOpen) "Close search" else "Search tracks",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(40.dp).clip(CircleShape)
                                .clickable {
                                    searchOpen = !searchOpen
                                    if (!searchOpen) query = ""
                                }
                                .padding(8.dp),
                        )
                    }
                }
            }
        }

        if (tracks.size > 5) {
            item {
                androidx.compose.runtime.LaunchedEffect(searchOpen) {
                    if (searchOpen) runCatching { searchFocus.requestFocus() }
                }
                androidx.compose.animation.AnimatedVisibility(visible = searchOpen) {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp).focusRequester(searchFocus),
                        placeholder = { Text("Search in ${info.typeLabel.lowercase()}") },
                        leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
                        trailingIcon = { if (query.isNotEmpty()) Icon(Icons.Filled.Close, "Clear", modifier = Modifier.clip(CircleShape).clickable { query = "" }.padding(4.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = MaterialTheme.colorScheme.primary,
                        ),
                    )
                }
            }
        }

        val baseTracks =
            if (itemKind == "playlist" && onReorderPlaylist != null) playlistOrder
            else tracks

        val shown = if (query.isBlank() || !searchOpen) baseTracks
            else baseTracks.filter {
                it.title.contains(query, true) || it.artist.contains(query, true)
            }

        items(
            count = shown.size,
            key = { i -> shown[i].id },
        ) { i ->
            val s = shown[i]
            val isDragging = draggingSongId == s.id
            val isSelected = s.id in selectedTrackIds

            val toggleSelection = {
                selectedTrackIds =
                    if (isSelected) {
                        selectedTrackIds - s.id
                    } else {
                        selectedTrackIds + s.id
                    }
            }

            SongRow(
                song = s,
                isPlaying = s.id == currentSongId && isPlaying,
                isLiked = likedIds.contains(s.id),
                selected = isSelected,
                onClick = {
                    if (selectionMode) {
                        toggleSelection()
                    } else {
                        onPlayAll(shown, i)
                    }
                },
                onLongClick = toggleSelection,
                onArtworkClick = toggleSelection,
                onToggleLike = { onToggleLike(s.id) },
                modifier = (
                    if (isDragging) {
                        Modifier
                    } else {
                        Modifier.animateItem()
                    }
                )
                    .padding(horizontal = 8.dp)
                    .zIndex(if (isDragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (isDragging) dragOffsetY else 0f
                        shadowElevation = if (isDragging) 16f else 0f
                        scaleX = if (isDragging) 1.02f else 1f
                        scaleY = if (isDragging) 1.02f else 1f
                    },
                artworkModifier =
                    if (
                        itemKind == "playlist" &&
                        onReorderPlaylist != null &&
                        !searchOpen &&
                        !selectionMode
                    ) {
                        Modifier
                            .size(52.dp)
                            .pointerInput(s.id) {
                                detectDragGestures(
                                    onDragStart = {
                                        draggingSongId = s.id
                                        dragOffsetY = 0f
                                        onReorderGrab?.invoke()
                                    },
                                    onDragCancel = {
                                        draggingSongId = null
                                        dragOffsetY = 0f
                                        onReorderPlaylist(playlistOrder.toList())
                                    },
                                    onDragEnd = {
                                        draggingSongId = null
                                        dragOffsetY = 0f
                                        onReorderPlaylist(playlistOrder.toList())
                                    },
                                ) { change, dragAmount ->
                                    change.consume()
                                    dragOffsetY += dragAmount.y

                                    var currentIndex =
                                        playlistOrder.indexOfFirst { it.id == s.id }

                                    while (
                                        currentIndex >= 0 &&
                                        currentIndex < playlistOrder.lastIndex &&
                                        dragOffsetY > reorderStepPx / 2f
                                    ) {
                                        val moved =
                                            playlistOrder.removeAt(currentIndex)
                                        playlistOrder.add(currentIndex + 1, moved)

                                        currentIndex++
                                        dragOffsetY -= reorderStepPx
                                    }

                                    while (
                                        currentIndex > 0 &&
                                        dragOffsetY < -reorderStepPx / 2f
                                    ) {
                                        val moved =
                                            playlistOrder.removeAt(currentIndex)
                                        playlistOrder.add(currentIndex - 1, moved)

                                        currentIndex--
                                        dragOffsetY += reorderStepPx
                                    }
                                }
                            }
                    } else {
                        Modifier
                    },
                onAddToQueue = { onAddToQueue(s) },
                onPlayNext = { onPlayNext(s) },
                onAddToPlaylist = { onAddToPlaylist(s) },
                onRemoveFromPlaylist = onRemoveFromPlaylist?.let { cb -> { cb(s) } },
                onGoToAlbum = if (!info.isArtist && s.albumId.isNotBlank()) ({ onOpenDetail("album", s.albumId) }) else null,
                onGoToArtist = if (s.artistId.isNotBlank()) ({ onOpenDetail("artist", s.artistId) }) else null,
                isDownloaded = canDownload && downloadedIds.contains(s.id),
                onDownload = if (canDownload) ({ onDownload(s) }) else null,
                onRemoveDownload = if (canDownload) ({ onRemoveDownload(s.id) }) else null,
                onEditTags = onEditTags?.let { cb -> { cb(s) } },
                serverTagEditing = serverTagEditing,
            )
        }

        if (state.loadingMore) {
            item {
                Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
                    com.mentality.sonethyst.ui.components.LottieLoader(modifier = Modifier.size(40.dp))
                }
            }
        }
    }

    if (showCoverSheet && onSetPlaylistCover != null) {
        PlaylistCoverSheet(
            tracks = tracks,
            currentMode = info.playlistCoverMode
                .ifBlank { "automatic" }
                .lowercase(),
            onAutomatic = {
                onSetPlaylistCover("automatic", null)
                showCoverSheet = false
            },
            onFirstTrack = {
                onSetPlaylistCover("first", null)
                showCoverSheet = false
            },
            onCollage = {
                onSetPlaylistCover("collage", null)
                showCoverSheet = false
            },
            onTrack = { song ->
                onSetPlaylistCover("track", song.id)
                showCoverSheet = false
            },
            onChooseImage = {
                customCoverLauncher.launch(
                    arrayOf("image/*")
                )
            },
            onDismiss = {
                showCoverSheet = false
            },
        )
    }

    if (showEdit) {
        EditPlaylistDialog(
            initialName = info.title,
            initialDesc = info.subtitle,
            onSave = { name, desc -> onEditPlaylist(name, desc); showEdit = false },
            onDismiss = { showEdit = false },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ArtistAbout(info: com.mentality.sonethyst.data.remote.ArtistInfo, accent: Color) {
    var expanded by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        SectionHeader("About")
        Spacer(Modifier.height(10.dp))
        val meta = listOf(info.country, info.yearsActive).filter { it.isNotBlank() }.joinToString("  •  ")
        if (meta.isNotBlank()) {
            Text(meta, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = accent)
            Spacer(Modifier.height(8.dp))
        }
        if (info.bio.isNotBlank()) {
            Text(
                info.bio,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 5,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier.clickable { expanded = !expanded },
            )
            Text(
                if (expanded) "Show less" else "Show more",
                style = MaterialTheme.typography.labelLarge,
                color = accent,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable { expanded = !expanded }.padding(vertical = 4.dp),
            )
        }
        if (info.tags.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                info.tags.forEach { tag ->
                    Text(
                        tag,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EditPlaylistDialog(initialName: String, initialDesc: String, onSave: (String, String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    var desc by remember { mutableStateOf(initialDesc) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit playlist", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                androidx.compose.material3.OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                androidx.compose.material3.OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") })
            }
        },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { if (name.isNotBlank()) onSave(name.trim(), desc.trim()) }, enabled = name.isNotBlank()) { Text("Save") } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
