package com.mentality.sonethyst.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Filter1
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.R
import com.mentality.sonethyst.model.Song

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistCoverSheet(
    tracks: List<Song>,
    currentMode: String,
    onAutomatic: () -> Unit,
    onFirstTrack: () -> Unit,
    onCollage: () -> Unit,
    onTrack: (Song) -> Unit,
    onChooseImage: () -> Unit,
    onDismiss: () -> Unit,
) {
    var choosingTrack by remember { mutableStateOf(false) }

    val trackArtworks = remember(tracks) {
        tracks.filter { it.artworkUrl.isNotBlank() }
    }
    val distinctArtworkCount = remember(trackArtworks) {
        trackArtworks
            .asSequence()
            .map { it.artworkUrl }
            .distinct()
            .count()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        if (choosingTrack) {
            ListItem(
                headlineContent = {
                    Text(
                        stringResource(R.string.cover_choose_track_artwork),
                        fontWeight = FontWeight.Bold,
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.action_back),
                    )
                },
                modifier = Modifier.clickable {
                    choosingTrack = false
                },
                colors = ListItemDefaults.colors(
                    containerColor = Color.Transparent,
                ),
            )

            if (trackArtworks.isEmpty()) {
                Text(
                    stringResource(R.string.cover_no_track_artwork),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(24.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                ) {
                    items(
                        items = trackArtworks,
                        key = { it.id },
                    ) { song ->
                        ListItem(
                            headlineContent = {
                                Text(
                                    displayTitle(song.title),
                                    maxLines = 1,
                                )
                            },
                            supportingContent = {
                                Text(
                                    displayArtist(song.artist),
                                    maxLines = 1,
                                )
                            },
                            leadingContent = {
                                Artwork(
                                    song.artworkUrl,
                                    song.accent,
                                    Modifier.size(52.dp),
                                    corner = 10.dp,
                                )
                            },
                            modifier = Modifier.clickable {
                                onTrack(song)
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent,
                            ),
                        )
                    }
                }
            }
        } else {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text(
                    stringResource(R.string.cover_playlist_cover),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(
                        horizontal = 24.dp,
                        vertical = 12.dp,
                    ),
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.cover_automatic)) },
                    supportingContent = {
                        Text(stringResource(R.string.cover_automatic_description))
                    },
                    leadingContent = {
                        Icon(Icons.Filled.AutoAwesome, null)
                    },
                    trailingContent = {
                        if (currentMode == "automatic") {
                            Icon(
                                Icons.Filled.Check,
                                stringResource(R.string.song_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    modifier = Modifier.clickable(onClick = onAutomatic),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.cover_first_track)) },
                    supportingContent = {
                        Text(stringResource(R.string.cover_first_track_description))
                    },
                    leadingContent = {
                        Icon(Icons.Filled.Filter1, null)
                    },
                    trailingContent = {
                        if (currentMode == "first") {
                            Icon(
                                Icons.Filled.Check,
                                stringResource(R.string.song_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    modifier = Modifier.clickable(
                        enabled = tracks.isNotEmpty(),
                        onClick = onFirstTrack,
                    ),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.cover_collage)) },
                    supportingContent = {
                        Text(
                            if (distinctArtworkCount >= 4) {
                                stringResource(R.string.cover_collage_description)
                            } else {
                                stringResource(R.string.cover_collage_requirement)
                            }
                        )
                    },
                    leadingContent = {
                        Icon(Icons.Filled.GridView, null)
                    },
                    trailingContent = {
                        if (currentMode == "collage") {
                            Icon(
                                Icons.Filled.Check,
                                stringResource(R.string.song_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    modifier = Modifier.clickable(
                        enabled = distinctArtworkCount >= 4,
                        onClick = onCollage,
                    ),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.cover_track_artwork)) },
                    supportingContent = {
                        Text(stringResource(R.string.cover_track_artwork_description))
                    },
                    leadingContent = {
                        Icon(Icons.Filled.MusicNote, null)
                    },
                    trailingContent = {
                        if (currentMode == "track") {
                            Icon(
                                Icons.Filled.Check,
                                stringResource(R.string.song_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    modifier = Modifier.clickable(
                        enabled = trackArtworks.isNotEmpty(),
                    ) {
                        choosingTrack = true
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                )

                ListItem(
                    headlineContent = { Text(stringResource(R.string.cover_device_image)) },
                    supportingContent = {
                        Text(stringResource(R.string.cover_device_image_description))
                    },
                    leadingContent = {
                        Icon(Icons.Filled.Image, null)
                    },
                    trailingContent = {
                        if (currentMode == "custom") {
                            Icon(
                                Icons.Filled.Check,
                                stringResource(R.string.song_selected),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    modifier = Modifier.clickable(onClick = onChooseImage),
                    colors = ListItemDefaults.colors(
                        containerColor = Color.Transparent,
                    ),
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
