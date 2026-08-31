package com.mentality.sonethyst.ui.screens.podcasts

import com.mentality.sonethyst.R
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mentality.sonethyst.data.Podcast
import com.mentality.sonethyst.data.PodcastEpisode
import com.mentality.sonethyst.data.toSong
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.ui.components.Artwork
import com.mentality.sonethyst.ui.components.LottieLoader
import com.mentality.sonethyst.util.accentFor
import com.mentality.sonethyst.viewmodel.PodcastViewModel
import java.util.Date

@Composable
fun PodcastDetailScreen(
    contentPadding: PaddingValues,
    feedUrl: String,
    title: String,
    imageUrl: String,
    author: String,
    onBack: () -> Unit,
    onPlay: (Song) -> Unit,
) {
    val vm: PodcastViewModel = viewModel()
    val episodesState by vm.episodes.collectAsStateWithLifecycle()
    val subs by vm.subscriptions.collectAsStateWithLifecycle()
    val topInset =
        WindowInsets.statusBars
            .asPaddingValues()
            .calculateTopPadding()

    val podcastFallback =
        stringResource(
            R.string.podcasts_default_show
        )

    val episodeFallback =
        stringResource(
            R.string.podcasts_default_episode
        )

    LaunchedEffect(feedUrl) { vm.loadEpisodes(feedUrl, title, imageUrl) }

    val showTitle =
        episodesState.channelTitle
            .ifBlank { title }
            .ifBlank { podcastFallback }
    val showImage = episodesState.channelImage.ifBlank { imageUrl }
    val subscribed = subs.any { it.feedUrl == feedUrl }
    val accent = accentFor(feedUrl)

    Column(Modifier.fillMaxSize().padding(top = topInset)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.action_back),
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack).padding(8.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(showTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        }

        val bottom = contentPadding.calculateBottomPadding() + 24.dp
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = bottom)) {
            item {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Artwork(showImage, accent, Modifier.size(96.dp), corner = 16.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(showTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        author.takeIf { it.isNotBlank() }?.let {
                            Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.clip(RoundedCornerShape(50))
                                .background(if (subscribed) MaterialTheme.colorScheme.surfaceContainerHighest else accent)
                                .clickable {
                                    vm.toggleSubscribe(Podcast(feedUrl = feedUrl, title = showTitle, author = author, imageUrl = showImage))
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                if (subscribed) Icons.Filled.Check else Icons.Filled.Add, null,
                                tint = if (subscribed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                stringResource(
                                    if (subscribed) {
                                        R.string.podcasts_subscribed
                                    } else {
                                        R.string.podcasts_subscribe
                                    }
                                ),
                                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold,
                                color = if (subscribed) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    stringResource(
                        R.string.podcasts_episodes
                    ),
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, bottom = 4.dp),
                )
            }

            if (episodesState.loading) {
                item { Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) { LottieLoader(modifier = Modifier.size(64.dp)) } }
            } else if (episodesState.episodes.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(
                                if (episodesState.failed) {
                                    R.string.podcasts_feed_unavailable
                                } else {
                                    R.string.podcasts_no_episodes
                                }
                            ), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(episodesState.episodes.size) { i ->
                    val ep = episodesState.episodes[i]
                    EpisodeRow(ep, onPlay = { onPlay(ep.toSong(
                            showImage,
                            episodeFallback,
                        )) })
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episode: PodcastEpisode,
    onPlay: () -> Unit,
) {
    val context =
        LocalContext.current

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)).clickable(onClick = onPlay).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                episode.title.ifBlank {
                    stringResource(
                        R.string.podcasts_default_episode
                    )
                }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(
                episode.pubDateMs
                    .takeIf { it > 0 }
                    ?.let {
                        formatDate(
                            context,
                            it,
                        )
                    },
                episode.durationSec
                    .takeIf { it > 0 }
                    ?.let {
                        formatDuration(
                            context.resources,
                            it,
                        )
                    },
            ).joinToString(" • ")
            if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            Icons.Filled.PlayArrow,
            stringResource(R.string.action_play),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onPlay).padding(9.dp),
        )
    }
}

private fun formatDate(
    context: android.content.Context,
    ms: Long,
): String =
    runCatching {
        android.text.format.DateFormat
            .getMediumDateFormat(context)
            .format(
                Date(ms)
            )
    }.getOrDefault("")

private fun formatDuration(
    resources: android.content.res.Resources,
    sec: Int,
): String {
    val h =
        sec / 3600

    val m =
        (sec % 3600) / 60

    return when {
        h > 0 -> {
            val hours =
                resources.getQuantityString(
                    R.plurals.podcast_hours,
                    h,
                    h,
                )

            val minutes =
                resources.getQuantityString(
                    R.plurals.podcast_minutes,
                    m,
                    m,
                )

            resources.getString(
                R.string.podcast_hours_minutes,
                hours,
                minutes,
            )
        }

        m > 0 ->
            resources.getQuantityString(
                R.plurals.podcast_minutes,
                m,
                m,
            )

        else ->
            resources.getQuantityString(
                R.plurals.podcast_seconds,
                sec,
                sec,
            )
    }
}
