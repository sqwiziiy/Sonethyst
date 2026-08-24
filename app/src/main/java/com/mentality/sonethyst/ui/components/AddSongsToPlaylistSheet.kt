package com.mentality.sonethyst.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSongsToPlaylistSheet(
    songs: List<Song>,
    loading: Boolean,
    saving: Boolean,
    onAdd: (List<Song>) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var selectedIds by remember {
        mutableStateOf<Set<String>>(emptySet())
    }

    val visible = remember(songs, query) {
        val q = query.trim()

        if (q.isBlank()) {
            songs
        } else {
            songs.filter { song ->
                song.title.contains(q, ignoreCase = true) ||
                    song.artist.contains(q, ignoreCase = true) ||
                    song.album.contains(q, ignoreCase = true)
            }
        }
    }

    val selectedSongs = remember(songs, selectedIds) {
        songs.filter { it.id in selectedIds }
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!saving) onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
        ) {
            Text(
                "Add songs",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 8.dp,
                ),
            )

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                singleLine = true,
                enabled = !saving,
                label = { Text("Search library") },
                leadingIcon = {
                    Icon(Icons.Filled.Search, null)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            )

            when {
                loading -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        LottieLoader(Modifier.size(64.dp))
                    }
                }

                songs.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "All library songs are already in this playlist",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                visible.isEmpty() -> {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "No matching songs",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 500.dp),
                    ) {
                        items(
                            items = visible,
                            key = { song -> song.id },
                            contentType = { "song-picker-row" },
                        ) { song ->
                            val selected = song.id in selectedIds

                            ListItem(
                                headlineContent = {
                                    Text(
                                        song.title,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                supportingContent = {
                                    Text(
                                        listOf(
                                            song.artist,
                                            song.album,
                                        )
                                            .filter { it.isNotBlank() }
                                            .joinToString(" • "),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                },
                                leadingContent = {
                                    Artwork(
                                        song.artworkUrl,
                                        song.accent,
                                        Modifier.size(48.dp),
                                        corner = 10.dp,
                                    )
                                },
                                trailingContent = {
                                    Checkbox(
                                        checked = selected,
                                        onCheckedChange = null,
                                    )
                                },
                                modifier = Modifier.clickable(
                                    enabled = !saving,
                                ) {
                                    selectedIds =
                                        if (selected) {
                                            selectedIds - song.id
                                        } else {
                                            selectedIds + song.id
                                        }
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color.Transparent,
                                ),
                            )
                        }
                    }
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                TextButton(
                    enabled = visible.isNotEmpty() && !saving,
                    onClick = {
                        val visibleIds =
                            visible.map { it.id }.toSet()

                        selectedIds =
                            if (
                                visibleIds.all {
                                    it in selectedIds
                                }
                            ) {
                                selectedIds - visibleIds
                            } else {
                                selectedIds + visibleIds
                            }
                    },
                ) {
                    Text("Select visible")
                }

                Button(
                    enabled =
                        selectedSongs.isNotEmpty() && !saving,
                    onClick = {
                        onAdd(selectedSongs)
                    },
                ) {
                    Text(
                        if (saving) {
                            "Adding…"
                        } else {
                            "Add (${selectedSongs.size})"
                        }
                    )
                }
            }
        }
    }
}
