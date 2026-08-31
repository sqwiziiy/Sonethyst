package com.mentality.sonethyst.ui.screens.library

import com.mentality.sonethyst.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.data.DuplicateGroup
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.ui.components.Artwork
import com.mentality.sonethyst.ui.components.LottieLoader
import com.mentality.sonethyst.ui.components.displayAlbum
import com.mentality.sonethyst.ui.screens.settings.SettingsTopBar
import java.util.Locale

/** Result list of the duplicate scan: one card per suspected-duplicate group. */
@Composable
fun DuplicatesScreen(
    contentPadding: PaddingValues,
    loading: Boolean,
    scanned: Int,
    groups: List<DuplicateGroup>,
    currentSongId: String,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    canDelete: (Song) -> Boolean,
    onDelete: (Song) -> Unit,
    onPlay: (Song) -> Unit,
) {
    var deleteCandidate by
        remember {
            mutableStateOf<Song?>(null)
        }

    deleteCandidate?.let { song ->
        AlertDialog(
            onDismissRequest = {
                deleteCandidate = null
            },
            title = {
                Text(
                    stringResource(
                        R.string.duplicates_delete_title
                    )
                )
            },
            text = {
                Column {
                    Text(
                        stringResource(
                            R.string.duplicates_delete_message
                        )
                    )

                    if (song.path.isNotBlank()) {
                        Spacer(
                            Modifier.height(8.dp)
                        )

                        Text(
                            song.path,
                            style =
                                MaterialTheme.typography
                                    .bodySmall,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        deleteCandidate = null
                        onDelete(song)
                    }
                ) {
                    Text(
                        stringResource(
                            R.string.action_delete
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deleteCandidate = null
                    }
                ) {
                    Text(
                        stringResource(
                            R.string.action_cancel
                        )
                    )
                }
            },
        )
    }

    Column(
        Modifier.fillMaxSize()
    ) {
        SettingsTopBar(
            title =
                stringResource(
                    R.string.duplicates_title
                ),
            onBack = onBack,
        )

        ScanHeader(
            loading = loading,
            scanned = scanned,
            groups = groups,
            onRefresh = onRefresh,
        )

        when {
            loading -> {
                Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    LottieLoader(
                        modifier = Modifier.size(72.dp)
                    )
                }
            }

            groups.isEmpty() -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text =
                            stringResource(
                                R.string.duplicates_none
                            ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp),
                    contentPadding =
                        PaddingValues(
                            bottom =
                                contentPadding
                                    .calculateBottomPadding() +
                                    24.dp,
                        ),
                ) {
                    items(
                        count = groups.size,
                        key = { index ->
                            val group = groups[index]
                            "${group.artist}\u0001${group.title}\u0001$index"
                        },
                    ) { index ->
                        GroupCard(
                            group = groups[index],
                            currentSongId = currentSongId,
                            canDelete = canDelete,
                            onDeleteRequest = {
                                deleteCandidate = it
                            },
                            onPlay = onPlay,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScanHeader(
    loading: Boolean,
    scanned: Int,
    groups: List<DuplicateGroup>,
    onRefresh: () -> Unit,
) {
    val copies =
        groups.sumOf {
            it.songs.size
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    start = 20.dp,
                    end = 10.dp,
                    bottom = 6.dp,
                ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            Modifier.weight(1f)
        ) {
            Text(
                text =
                    if (loading) {
                        stringResource(
                            R.string.duplicates_scanning
                        )
                    } else {
                        pluralStringResource(
                            R.plurals.duplicates_tracks_scanned,
                            scanned,
                            scanned,
                        )
                    },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (!loading && groups.isNotEmpty()) {
                val groupCount =
                    pluralStringResource(
                        R.plurals.duplicates_group_count,
                        groups.size,
                        groups.size,
                    )

                val copyCount =
                    pluralStringResource(
                        R.plurals.duplicates_copy_count,
                        copies,
                        copies,
                    )

                Text(
                    text =
                        stringResource(
                            R.string.duplicates_summary,
                            groupCount,
                            copyCount,
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        IconButton(
            onClick = onRefresh,
            enabled = !loading,
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription =
                    stringResource(
                        R.string.duplicates_rescan
                    ),
                tint =
                    if (loading) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
            )
        }
    }
}

@Composable
private fun GroupCard(
    group: DuplicateGroup,
    currentSongId: String,
    canDelete: (Song) -> Boolean,
    onDeleteRequest: (Song) -> Unit,
    onPlay: (Song) -> Unit,
) {
    val bestQualitySongId =
        uniqueBestQualitySongId(
            group.songs
        )

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                MaterialTheme.colorScheme
                    .surfaceContainerHigh
                    .copy(alpha = 0.45f)
            )
            .padding(12.dp),
    ) {
        Text(
            text = group.title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val copiesLabel =
            pluralStringResource(
                R.plurals.duplicates_copy_count,
                group.songs.size,
                group.songs.size,
            )

        Text(
            text =
                stringResource(
                    R.string.duplicates_artist_copies,
                    group.artist,
                    copiesLabel,
                ),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(
            Modifier.height(8.dp)
        )

        group.songs.forEachIndexed { index, song ->
            DuplicateSongRow(
                song = song,
                bestQuality =
                    song.id ==
                        bestQualitySongId,
                playing =
                    song.id == currentSongId,
                localFile =
                    canDelete(song),
                onDeleteRequest =
                    onDeleteRequest,
                onPlay = onPlay,
            )
        }
    }
}

@Composable
private fun DuplicateSongRow(
    song: Song,
    bestQuality: Boolean,
    playing: Boolean,
    localFile: Boolean,
    onDeleteRequest: (Song) -> Unit,
    onPlay: (Song) -> Unit,
) {
    val resources =
        LocalContext.current.resources

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    onPlay(song)
                }
                .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Artwork(
            url = song.artworkUrl,
            accent = song.accent,
            modifier = Modifier.size(44.dp),
            corner = 10.dp,
        )

        Spacer(
            Modifier.width(10.dp)
        )

        Column(
            Modifier.weight(1f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                        text =
                            displayAlbum(song.album).ifBlank {
                            stringResource(
                                R.string.duplicates_unknown_album
                            )
                        },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }

            Text(
                text =
                    specLine(
                        resources,
                        song,
                    ),
                style = MaterialTheme.typography.labelSmall,
                color =
                    MaterialTheme.colorScheme
                        .onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            if (song.path.isNotBlank()) {
                Text(
                    text = song.path,
                    style =
                        MaterialTheme.typography
                            .labelSmall,
                    color =
                        MaterialTheme.colorScheme
                            .onSurfaceVariant
                            .copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow =
                        TextOverflow.Ellipsis,
                )
            }

            if (bestQuality) {
                Text(
                    text =
                        stringResource(
                            R.string.duplicates_best_quality
                        ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (playing) {
                Icon(
                    imageVector =
                        Icons.Filled.MusicNote,
                    contentDescription =
                        stringResource(
                            R.string.duplicates_playing
                        ),
                    tint =
                        MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )

                if (localFile) {
                    Spacer(
                        Modifier.width(4.dp)
                    )
                }
            }

            if (localFile) {
                IconButton(
                    onClick = {
                        onDeleteRequest(song)
                    }
                ) {
                    Icon(
                        imageVector =
                            Icons.Filled.DeleteOutline,
                        contentDescription =
                            stringResource(
                                R.string.duplicates_delete
                            ),
                        tint =
                            MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

private fun uniqueBestQualitySongId(
    songs: List<Song>,
): String? {
    if (songs.size < 2) {
        return null
    }

    val ranked =
        songs
            .map {
                it to duplicateQualityScore(it)
            }
            .sortedByDescending {
                it.second
            }

    val best = ranked[0]
    val second = ranked[1]

    /*
     * Equal quality means there is no honest "best" copy.
     */
    if (
        best.second <= 0L ||
        best.second == second.second
    ) {
        return null
    }

    return best.first.id
}

private fun duplicateQualityScore(
    song: Song,
): Long {
    var score = 0L

    if (
        song.suffix.lowercase(Locale.ROOT) in
            setOf(
                "flac",
                "alac",
                "wav",
                "wave",
                "aiff",
                "aif",
            )
    ) {
        score += 1_000_000_000_000L
    }

    score +=
        song.bitDepth
            .coerceAtLeast(0)
            .toLong() *
            1_000_000_000L

    score +=
        song.sampleRateHz
            .coerceAtLeast(0)
            .toLong() *
            1_000L

    score +=
        song.bitrateKbps
            .coerceAtLeast(0)
            .toLong()

    return score
}

private fun specLine(
    resources: android.content.res.Resources,
    song: Song,
): String {
    val parts =
        mutableListOf<String>()

    if (song.suffix.isNotBlank()) {
        parts +=
            song.suffix.uppercase(Locale.ROOT)
    }

    if (song.bitDepth > 0) {
        parts +=
            "${song.bitDepth}-bit"
    }

    if (song.sampleRateHz > 0) {
        parts +=
            sampleRateLabel(song.sampleRateHz)
    }

    if (song.bitrateKbps > 0) {
        parts +=
            "${song.bitrateKbps} kbps"
    }

    if (song.durationSec > 0) {
        parts +=
            "%d:%02d".format(
                Locale.ROOT,
                song.durationSec / 60,
                song.durationSec % 60,
            )
    }

    return parts
        .joinToString(" • ")
        .ifBlank {
            resources.getString(
                R.string.duplicates_quality_unavailable
            )
        }
}

private fun sampleRateLabel(
    sampleRateHz: Int,
): String =
    if (sampleRateHz % 1000 == 0) {
        "${sampleRateHz / 1000} kHz"
    } else {
        "%.1f kHz".format(
            Locale.ROOT,
            sampleRateHz / 1000.0,
        )
    }
