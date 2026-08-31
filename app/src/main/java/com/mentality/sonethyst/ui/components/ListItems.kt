package com.mentality.sonethyst.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueuePlayNext
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Explicit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.R
import com.mentality.sonethyst.model.Album
import com.mentality.sonethyst.model.Artist
import com.mentality.sonethyst.model.Playlist
import com.mentality.sonethyst.model.Song

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SongRow(
    song: Song,
    isPlaying: Boolean,
    isLiked: Boolean,
    onClick: () -> Unit,
    onToggleLike: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onArtworkClick: (() -> Unit)? = null,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
    index: Int? = null,
    onPlayNext: (() -> Unit)? = null,
    onAddToQueue: (() -> Unit)? = null,
    onAddToPlaylist: (() -> Unit)? = null,
    onRemoveFromPlaylist: (() -> Unit)? = null,
    onGoToAlbum: (() -> Unit)? = null,
    onGoToArtist: (() -> Unit)? = null,
    isDownloaded: Boolean = false,
    onDownload: (() -> Unit)? = null,
    onRemoveDownload: (() -> Unit)? = null,
    onEditTags: (() -> Unit)? = null,
    serverTagEditing: Boolean = false,
    onSetRating: ((Int) -> Unit)? = null,
    onEditCustomTags: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
) {
    var menuOpen by remember { mutableStateOf(false) }
    var ratingOpen by remember(song.id) {
        mutableStateOf(false)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f)
                }
            )
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = artworkModifier.then(
                if (onArtworkClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .combinedClickable(
                            onClick = onArtworkClick,
                            onLongClick = onLongClick,
                        )
                } else {
                    Modifier
                }
            ),
            contentAlignment = Alignment.Center,
        ) {
            Artwork(song.artworkUrl, song.accent, Modifier.size(52.dp), corner = 10.dp)
            if (isPlaying) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.GraphicEq, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                }
            }
            if (selected) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.48f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.CheckCircle,
                        stringResource(R.string.song_selected),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                displayTitle(song.title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.explicit) {
                    Icon(
                        Icons.Outlined.Explicit, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp).padding(end = 0.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    displayArtist(song.artist),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        if (isDownloaded) {
            Icon(Icons.Filled.DownloadDone, stringResource(R.string.song_downloaded), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
        }
        Icon(
            imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription =
                stringResource(
                    if (isLiked) {
                        R.string.song_remove_from_liked
                    } else {
                        R.string.song_add_to_liked
                    }
                ),
            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onToggleLike)
                .padding(6.dp),
        )
        Text(
            formatTime(song.durationSec),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        Box {
            Icon(
                Icons.Outlined.MoreVert, stringResource(R.string.action_more),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp).clip(CircleShape).clickable { menuOpen = true }.padding(4.dp),
            )
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_play)) },
                    onClick = { menuOpen = false; onClick() },
                    leadingIcon = { Icon(Icons.Filled.PlayArrow, null) },
                )
                if (onPlayNext != null) DropdownMenuItem(
                    text = { Text(stringResource(R.string.song_play_next)) },
                    onClick = { menuOpen = false; onPlayNext() },
                    leadingIcon = { Icon(Icons.Filled.QueuePlayNext, null) },
                )
                if (onAddToQueue != null) DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_add_to_queue)) },
                    onClick = { menuOpen = false; onAddToQueue() },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.QueueMusic, null) },
                )
                if (onAddToPlaylist != null) DropdownMenuItem(
                    text = { Text(stringResource(R.string.song_add_to_playlist)) },
                    onClick = { menuOpen = false; onAddToPlaylist() },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, null) },
                )
                if (onRemoveFromPlaylist != null) DropdownMenuItem(
                    text = { Text(stringResource(R.string.song_remove_from_playlist)) },
                    onClick = { menuOpen = false; onRemoveFromPlaylist() },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Delete,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            stringResource(
                                if (isLiked) {
                                    R.string.song_remove_from_liked
                                } else {
                                    R.string.song_add_to_liked
                                }
                            )
                        )
                    },
                    onClick = { menuOpen = false; onToggleLike() },
                    leadingIcon = { Icon(if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder, null) },
                )
                if (onEditCustomTags != null) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.song_tags)) },
                        onClick = {
                            menuOpen = false
                            onEditCustomTags()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Label,
                                null,
                            )
                        },
                    )
                }

                if (onHide != null) {
                    DropdownMenuItem(
                        text = {
                            Text(stringResource(R.string.song_hide_from_library))
                        },
                        onClick = {
                            menuOpen = false
                            onHide()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.VisibilityOff,
                                null,
                            )
                        },
                    )
                }

                if (onSetRating != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                if (song.rating > 0) {
                                    stringResource(
                                        R.string.song_rating,
                                        song.rating,
                                    )
                                } else {
                                    stringResource(
                                        R.string.song_rate
                                    )
                                }
                            )
                        },
                        onClick = {
                            menuOpen = false
                            ratingOpen = true
                        },
                        leadingIcon = {
                            Icon(
                                if (song.rating > 0) {
                                    Icons.Filled.Star
                                } else {
                                    Icons.Outlined.StarBorder
                                },
                                null,
                            )
                        },
                    )
                }
                if (isDownloaded && onRemoveDownload != null) DropdownMenuItem(
                    text = { Text(stringResource(R.string.song_remove_download)) },
                    onClick = { menuOpen = false; onRemoveDownload() },
                    leadingIcon = { Icon(Icons.Filled.DownloadDone, null, tint = MaterialTheme.colorScheme.primary) },
                ) else if (onDownload != null) DropdownMenuItem(
                    text = { Text(stringResource(R.string.song_download)) },
                    onClick = { menuOpen = false; onDownload() },
                    leadingIcon = { Icon(Icons.Filled.Download, null) },
                )
                if (onGoToAlbum != null) DropdownMenuItem(
                    text = { Text(stringResource(R.string.song_go_to_album)) },
                    onClick = { menuOpen = false; onGoToAlbum() },
                    leadingIcon = { Icon(Icons.Filled.Album, null) },
                )
                if (onGoToArtist != null) DropdownMenuItem(
                    text = { Text(stringResource(R.string.song_go_to_artist)) },
                    onClick = { menuOpen = false; onGoToArtist() },
                    leadingIcon = { Icon(Icons.Filled.Person, null) },
                )
                // tag edit only on content:// files or backends with metadata write
                if (onEditTags != null && (song.streamUrl.startsWith("content://") || serverTagEditing)) DropdownMenuItem(
                    text = { Text(stringResource(R.string.action_edit_tags)) },
                    onClick = { menuOpen = false; onEditTags() },
                    leadingIcon = { Icon(Icons.Filled.Edit, null) },
                )
            }
        }
    }

    if (
        ratingOpen &&
        onSetRating != null
    ) {
        SongRatingDialog(
            song = song,
            onSelect = { rating ->
                ratingOpen = false
                onSetRating(rating)
            },
            onDismiss = {
                ratingOpen = false
            },
        )
    }
}

@Composable
private fun SongRatingDialog(
    song: Song,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    R.string.rating_title,
                    displayTitle(song.title),
                )
            )
        },
        text = {
            Column {
                Text(
                    if (song.rating > 0) {
                        stringResource(
                            R.string.rating_current,
                            song.rating,
                        )
                    } else {
                        stringResource(
                            R.string.rating_choose
                        )
                    },
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant,
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceEvenly,
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    (1..5).forEach { value ->
                        Icon(
                            imageVector =
                                if (value <= song.rating) {
                                    Icons.Filled.Star
                                } else {
                                    Icons.Outlined.StarBorder
                                },
                            contentDescription =
                                androidx.compose.ui.res.pluralStringResource(
                                    R.plurals.rating_stars,
                                    value,
                                    value,
                                ),
                            tint =
                                if (value <= song.rating) {
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
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        onSelect(value)
                                    }
                                    .padding(7.dp),
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        dismissButton = {
            if (song.rating > 0) {
                TextButton(
                    onClick = {
                        onSelect(0)
                    },
                ) {
                    Text(stringResource(R.string.rating_clear))
                }
            }
        },
    )
}

@Composable
fun PlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    width: androidx.compose.ui.unit.Dp = 156.dp,
) {
    Column(
        modifier = modifier
            .width(width)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Artwork(playlist.coverUrl, playlist.accent, Modifier.size(width - 16.dp), corner = 14.dp)
        Spacer(Modifier.height(10.dp))
        Text(
            playlist.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        Text(
            playlist.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun AlbumCard(album: Album, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(156.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Artwork(album.artworkUrl, MaterialTheme.colorScheme.secondary, Modifier.size(140.dp), corner = 14.dp)
        Spacer(Modifier.height(10.dp))
        Text(album.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            "${displayArtist(album.artist)} • ${album.year}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ArtistCircle(artist: Artist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(124.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Artwork(artist.imageUrl, MaterialTheme.colorScheme.tertiary, Modifier.size(108.dp), corner = 108.dp)
        Spacer(Modifier.height(8.dp))
        Text(artist.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            stringResource(R.string.library_artist),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun RecentTile(playlist: Playlist, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(playlist.coverUrl, playlist.accent, Modifier.size(60.dp), corner = 0.dp)
        Text(
            playlist.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 10.dp).weight(1f),
        )
    }
}
