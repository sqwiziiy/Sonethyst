package com.mentality.sonethyst.ui.screens.radio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mentality.sonethyst.R
import com.mentality.sonethyst.data.RadioStation
import com.mentality.sonethyst.data.toSong
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.ui.components.Artwork
import com.mentality.sonethyst.ui.components.LottieLoader
import com.mentality.sonethyst.util.accentFor
import com.mentality.sonethyst.viewmodel.RadioViewModel

private val GENRE_TAGS = listOf(
    "pop", "rock", "jazz", "classical", "news", "electronic", "hip hop",
    "country", "metal", "ambient", "lounge", "dance", "reggae", "blues", "talk",
)

@Composable
fun RadioScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPlay: (Song) -> Unit,
) {
    val vm: RadioViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    var searchOpen by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }

    var editingStation by
        remember {
            mutableStateOf<RadioStation?>(
                null
            )
        }

    var deletingStation by
        remember {
            mutableStateOf<RadioStation?>(
                null
            )
        }

    val defaultStationName =
        stringResource(
            R.string.radio_default_station
        )

    val internetRadioName =
        stringResource(
            R.string.radio_internet_radio
        )

    val playStation: (RadioStation) -> Unit = { st ->
        vm.registerPlay(st)
        onPlay(
            st.toSong(
                defaultStationName,
                internetRadioName,
            )
        )
    }

    val browsing = state.query.isBlank() && state.activeTag.isBlank()
    val listed = if (browsing) state.popular else state.results

    Column(Modifier.fillMaxSize().padding(top = topInset)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back),
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack).padding(8.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.radio_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(
                Icons.Filled.Add, stringResource(R.string.radio_add_custom_stream),
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable { showAdd = true }.padding(8.dp),
            )
            Icon(
                if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                if (searchOpen) {
                    stringResource(R.string.radio_close_search)
                } else {
                    stringResource(R.string.radio_search_stations)
                },
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable {
                    searchOpen = !searchOpen
                    if (!searchOpen) { query = ""; vm.clearSearch() }
                }.padding(8.dp),
            )
        }

        AnimatedVisibility(visible = searchOpen) {
            TextField(
                value = query,
                onValueChange = { query = it; vm.search(it) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text(stringResource(R.string.radio_search_placeholder)) },
                leadingIcon = { Icon(Icons.Filled.Search, null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
            )
        }

        val bottom = contentPadding.calculateBottomPadding() + 24.dp
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = bottom)) {
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        count = GENRE_TAGS.size,
                        key = { i -> "genre:${GENRE_TAGS[i]}" },
                        contentType = { "radio-genre" },
                    ) { i ->
                        val tag = GENRE_TAGS[i]
                        val selected = state.activeTag == tag
                        Text(
                            stringResource(
                                radioGenreLabelRes(tag)
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable { if (selected) vm.loadPopular() else vm.byTag(tag) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (favorites.isNotEmpty()) {
                item { SectionLabel(stringResource(R.string.radio_your_stations)) }
                items(
                    count = favorites.size,
                    key = { i -> "favorite:${favorites[i].uuid}" },
                    contentType = { "radio-station" },
                ) { i ->
                    val st = favorites[i]
                    StationRow(
                        st,
                        isFavorite =
                            vm.isFavorite(st),
                        onPlay = {
                            playStation(st)
                        },
                        onToggleFavorite = {
                            vm.toggleFavorite(st)
                        },
                        onEdit =
                            if (
                                st.custom == true
                            ) ({
                                editingStation = st
                            }) else null,
                        onDelete =
                            if (
                                st.custom == true
                            ) ({
                                deletingStation = st
                            }) else null,
                    )
                }
            }

            item {
                SectionLabel(
                    when {
                        state.query.isNotBlank() -> stringResource(R.string.radio_results)
                        state.activeTag.isNotBlank() ->
                            stringResource(
                                radioGenreLabelRes(
                                    state.activeTag
                                )
                            )
                        else -> stringResource(R.string.radio_popular_worldwide)
                    }
                )
            }

            if (state.loading) {
                item { Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) { LottieLoader(modifier = Modifier.size(64.dp)) } }
            } else if (listed.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
                        Text(
                            if (state.failed) {
                                stringResource(R.string.radio_directory_unavailable)
                            } else {
                                stringResource(R.string.radio_no_stations)
                            },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(
                    count = listed.size,
                    key = { i -> "listed:${listed[i].uuid}" },
                    contentType = { "radio-station" },
                ) { i ->
                    val st = listed[i]
                    StationRow(
                        st,
                        isFavorite =
                            vm.isFavorite(st),
                        onPlay = {
                            playStation(st)
                        },
                        onToggleFavorite = {
                            vm.toggleFavorite(st)
                        },
                        onEdit =
                            if (
                                st.custom == true
                            ) ({
                                editingStation = st
                            }) else null,
                        onDelete =
                            if (
                                st.custom == true
                            ) ({
                                deletingStation = st
                            }) else null,
                    )
                }
            }
        }
    }

    if (showAdd) {
        StreamEditorDialog(
            title = stringResource(R.string.radio_add_stream),
            confirmLabel = stringResource(R.string.action_create),
            initialName = "",
            initialUrl = "",
            onSave = { name, url ->
                vm.addCustom(
                    name,
                    url,
                )
            },
            onDismiss = {
                showAdd = false
            },
        )
    }

    editingStation?.let { station ->
        StreamEditorDialog(
            title = stringResource(R.string.radio_edit_stream),
            confirmLabel = stringResource(R.string.action_save),
            initialName =
                station.name.orEmpty(),
            initialUrl =
                station.streamUrl.orEmpty(),
            onSave = { name, url ->
                vm.editCustom(
                    station,
                    name,
                    url,
                )
            },
            onDismiss = {
                editingStation = null
            },
        )
    }

    deletingStation?.let { station ->
        AlertDialog(
            onDismissRequest = {
                deletingStation = null
            },
            title = {
                Text(
                    stringResource(R.string.radio_delete_station_title),
                    fontWeight =
                        FontWeight.Bold,
                )
            },
            text = {
                Text(
                    stringResource(
                        R.string.radio_delete_station_message,
                        if (
                            station.displayName.isBlank()
                        ) {
                            defaultStationName
                        } else {
                            station.displayName
                        },
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteCustom(
                            station
                        )

                        deletingStation =
                            null
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingStation =
                            null
                    }
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

private fun radioGenreLabelRes(
    tag: String,
): Int =
    when (tag) {
        "pop" -> R.string.radio_genre_pop
        "rock" -> R.string.radio_genre_rock
        "jazz" -> R.string.radio_genre_jazz
        "classical" -> R.string.radio_genre_classical
        "news" -> R.string.radio_genre_news
        "electronic" -> R.string.radio_genre_electronic
        "hip hop" -> R.string.radio_genre_hip_hop
        "country" -> R.string.radio_genre_country
        "metal" -> R.string.radio_genre_metal
        "ambient" -> R.string.radio_genre_ambient
        "lounge" -> R.string.radio_genre_lounge
        "dance" -> R.string.radio_genre_dance
        "reggae" -> R.string.radio_genre_reggae
        "blues" -> R.string.radio_genre_blues
        "talk" -> R.string.radio_genre_talk
        else -> R.string.radio_title
    }


@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 6.dp),
    )
}

@Composable
private fun StationRow(
    station: RadioStation,
    isFavorite: Boolean,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
) {
    val stationName =
        if (station.displayName.isBlank()) {
            stringResource(
                R.string.radio_default_station
            )
        } else {
            station.displayName
        }

    val stationGenre =
        if (station.genre.isBlank()) {
            stringResource(
                R.string.radio_internet_radio
            )
        } else {
            station.genre
        }

    var menuOpen by
        remember(station.uuid) {
            mutableStateOf(false)
        }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)).clickable(onClick = onPlay).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            Artwork(station.faviconUrl.orEmpty(), accentFor("radio:${station.uuid}"), Modifier.size(48.dp), corner = 10.dp)
            if (station.faviconUrl.isNullOrBlank()) {
                Icon(Icons.Filled.Radio, null, tint = Color.White, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stationName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = buildList {
                add(stationGenre)
                station.codec?.takeIf { it.isNotBlank() }?.let { add(it.uppercase()) }
                station.bitrate?.takeIf { it > 0 }?.let { add("${it}k") }
            }.joinToString(" • ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (
            onEdit != null ||
            onDelete != null
        ) {
            Box {
                Icon(
                    Icons.Filled.MoreVert,
                    stringResource(R.string.radio_custom_station_settings),
                    tint =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                    modifier =
                        Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .clickable {
                                menuOpen = true
                            }
                            .padding(8.dp),
                )

                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = {
                        menuOpen = false
                    },
                ) {
                    if (onEdit != null) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.action_edit))
                            },
                            onClick = {
                                menuOpen = false
                                onEdit()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Edit,
                                    null,
                                )
                            },
                        )
                    }

                    if (onDelete != null) {
                        DropdownMenuItem(
                            text = {
                                Text(stringResource(R.string.action_delete))
                            },
                            onClick = {
                                menuOpen = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Filled.Delete,
                                    null,
                                    tint =
                                        MaterialTheme
                                            .colorScheme
                                            .error,
                                )
                            },
                        )
                    }
                }
            }
        }

        if (station.custom != true) {
            Icon(
                if (isFavorite) {
                    Icons.Filled.Favorite
                } else {
                    Icons.Filled.FavoriteBorder
                },
                if (isFavorite) {
                    stringResource(R.string.radio_unfavorite)
                } else {
                    stringResource(R.string.radio_favorite)
                },
                tint =
                    if (isFavorite) {
                        MaterialTheme
                            .colorScheme
                            .primary
                    } else {
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant
                    },
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable(
                            onClick =
                                onToggleFavorite
                        )
                        .padding(8.dp),
            )
        }
        Icon(
            Icons.Filled.PlayArrow, stringResource(R.string.action_play),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onPlay).padding(8.dp),
        )
    }
}

@Composable
private fun StreamEditorDialog(
    title: String,
    confirmLabel: String,
    initialName: String,
    initialUrl: String,
    onSave: (String, String) -> String?,
    onDismiss: () -> Unit,
) {
    var name by
        remember(initialName) {
            mutableStateOf(
                initialName
            )
        }

    var url by
        remember(initialUrl) {
            mutableStateOf(
                initialUrl
            )
        }

    var error by
        remember(
            initialName,
            initialUrl,
        ) {
            mutableStateOf<String?>(
                null
            )
        }

    AlertDialog(
        onDismissRequest =
            onDismiss,
        title = {
            Text(
                title,
                fontWeight =
                    FontWeight.Bold,
            )
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = null
                    },
                    label = {
                        Text(
                            stringResource(R.string.radio_name_optional)
                        )
                    },
                    singleLine = true,
                )

                Spacer(
                    Modifier.height(10.dp)
                )

                OutlinedTextField(
                    value = url,
                    onValueChange = {
                        url = it
                        error = null
                    },
                    label = {
                        Text(stringResource(R.string.radio_stream_url))
                    },
                    singleLine = true,
                    placeholder = {
                        Text("https://…")
                    },
                    isError =
                        error != null,
                )

                if (error != null) {
                    Spacer(
                        Modifier.height(6.dp)
                    )

                    Text(
                        error.orEmpty(),
                        style =
                            MaterialTheme
                                .typography
                                .bodySmall,
                        color =
                            MaterialTheme
                                .colorScheme
                                .error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled =
                    url.isNotBlank(),
                onClick = {
                    val validationError =
                        onSave(
                            name,
                            url,
                        )

                    if (
                        validationError ==
                        null
                    ) {
                        onDismiss()
                    } else {
                        error =
                            validationError
                    }
                },
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick =
                    onDismiss
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
