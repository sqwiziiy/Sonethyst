package com.mentality.sonethyst.ui.screens.profile

import com.mentality.sonethyst.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.model.Artist
import com.mentality.sonethyst.model.Playlist
import com.mentality.sonethyst.ui.components.Artwork
import com.mentality.sonethyst.ui.components.ArtistCircle
import com.mentality.sonethyst.ui.components.PlaylistCard
import com.mentality.sonethyst.ui.components.SectionHeader

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    username: String,
    server: String,
    serverLabel: String,
    isLocal: Boolean = false,
    avatarUrl: String,
    playlists: List<Playlist>,
    artists: List<Artist>,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenDetail: (String, String) -> Unit,
) {
    val topInset =
        WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()

    val initials =
        username
            .take(2)
            .uppercase()
            .ifBlank {
                stringResource(
                    R.string.user_avatar_fallback
                )
            }

    val listenerLabel =
        stringResource(
            R.string.home_listener
        )
    LazyColumn(
        Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Box(Modifier.fillMaxWidth().height(280.dp)) {
                Box(
                    Modifier.fillMaxWidth().height(220.dp)
                        .background(Brush.verticalGradient(listOf(MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.background))),
                )
                Row(
                    Modifier.fillMaxWidth().padding(top = topInset + 6.dp, start = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        stringResource(R.string.action_back), modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack).padding(8.dp))
                    Spacer(Modifier.weight(1f))
                }
                Column(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(110.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (avatarUrl.isNotBlank()) {
                            Artwork(avatarUrl, MaterialTheme.colorScheme.primary, Modifier.matchParentSize(), corner = 55.dp)
                        } else {
                            Text(initials, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        if (isLocal) stringResource(R.string.accounts_local_library) else username.ifBlank {
                            listenerLabel
                        }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text(serverLabel.ifBlank { server.removePrefix("http://").removePrefix("https://") }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stat(
                    "${playlists.size}",
                    stringResource(
                        R.string.profile_playlists
                    ),
                )
                Stat(
                    "${artists.size}",
                    stringResource(
                        R.string.profile_artists
                    ),
                )
                Stat(
                    serverLabel.ifBlank {
                        stringResource(
                            R.string.profile_server
                        )
                    },
                    stringResource(
                        R.string.profile_server
                    ),
                )
            }
        }

        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary).clickable(onClick = onOpenSettings).padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Edit, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(
                                R.string.settings_title
                            ), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }

        if (artists.isNotEmpty()) {
            item {
                SectionHeader(
                    stringResource(
                        R.string.profile_top_artists
                    ), Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                    items(
                        count = artists.size,
                        key = { i -> "artist:${artists[i].id}" },
                        contentType = { "artist-circle" },
                    ) { i ->
                        ArtistCircle(
                            artists[i],
                            onClick = {
                                onOpenDetail("artist", artists[i].id)
                            },
                        )
                    }
                }
            }
        }

        if (playlists.isNotEmpty()) {
            item {
                SectionHeader(
                    stringResource(
                        R.string.profile_your_playlists
                    ), Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.height(10.dp))
                LazyRow(contentPadding = PaddingValues(horizontal = 8.dp)) {
                    items(
                        count = playlists.size,
                        key = { i -> "playlist:${playlists[i].id}" },
                        contentType = { "playlist-card" },
                    ) { i ->
                        PlaylistCard(
                            playlists[i],
                            onClick = {
                                onOpenDetail("playlist", playlists[i].id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
