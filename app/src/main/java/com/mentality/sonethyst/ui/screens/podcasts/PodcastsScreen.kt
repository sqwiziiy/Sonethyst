package com.mentality.sonethyst.ui.screens.podcasts

import com.mentality.sonethyst.R
import androidx.compose.ui.res.stringResource
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mentality.sonethyst.data.Podcast
import com.mentality.sonethyst.ui.components.Artwork
import com.mentality.sonethyst.ui.components.LottieLoader
import com.mentality.sonethyst.util.accentFor
import com.mentality.sonethyst.viewmodel.PodcastViewModel

private data class PodcastSeed(
    val query: String,
    val labelRes: Int,
)

private val SEED_TERMS =
    listOf(
        PodcastSeed("News", R.string.podcast_topic_news),
        PodcastSeed("Comedy", R.string.podcast_topic_comedy),
        PodcastSeed(
            "Technology",
            R.string.podcast_topic_technology,
        ),
        PodcastSeed("Science", R.string.podcast_topic_science),
        PodcastSeed(
            "True Crime",
            R.string.podcast_topic_true_crime,
        ),
        PodcastSeed("History", R.string.podcast_topic_history),
        PodcastSeed(
            "Business",
            R.string.podcast_topic_business,
        ),
        PodcastSeed("Sports", R.string.podcast_topic_sports),
        PodcastSeed("Health", R.string.podcast_topic_health),
        PodcastSeed("Music", R.string.podcast_topic_music),
    )

@Composable
fun PodcastsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onOpenPodcast: (Podcast) -> Unit,
) {
    val vm: PodcastViewModel = viewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val subs by vm.subscriptions.collectAsStateWithLifecycle()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // seed from vm so field stays in sync after popping back from detail
    var searchOpen by remember { mutableStateOf(state.query.isNotBlank()) }
    var query by remember { mutableStateOf(state.query) }

    Column(Modifier.fillMaxSize().padding(top = topInset)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                stringResource(R.string.action_back),
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onBack).padding(8.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.podcasts_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(
                if (searchOpen) Icons.Filled.Close else Icons.Filled.Search,
                if (searchOpen) {
                    stringResource(
                        R.string.podcasts_close_search
                    )
                } else {
                    stringResource(
                        R.string.podcasts_search
                    )
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
                placeholder = {
                    Text(
                        stringResource(
                            R.string.podcasts_search_placeholder
                        )
                    )
                },
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
                    items(SEED_TERMS.size) { i ->
                        val seed =
                            SEED_TERMS[i]

                        Text(
                            stringResource(seed.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .clickable {
                                    searchOpen = true
                                    query = seed.query
                                    vm.search(seed.query)
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            if (state.query.isBlank() && subs.isNotEmpty()) {
                item {
                    SectionLabel(
                        stringResource(
                            R.string.podcasts_subscribed
                        )
                    )
                }
                items(subs.size) { i ->
                    val p = subs[i]
                    PodcastRow(p, onClick = { onOpenPodcast(p) })
                }
            }

            if (state.query.isNotBlank()) {
                item {
                    SectionLabel(
                        stringResource(
                            R.string.podcasts_results
                        )
                    )
                }
                if (state.loading) {
                    item { Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) { LottieLoader(modifier = Modifier.size(64.dp)) } }
                } else if (state.results.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth().padding(vertical = 36.dp), contentAlignment = Alignment.Center) {
                            Text(
                                stringResource(
                                    if (state.failed) {
                                        R.string.podcasts_directory_unavailable
                                    } else {
                                        R.string.podcasts_no_shows
                                    }
                                ), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                } else {
                    items(state.results.size) { i ->
                        val p = state.results[i]
                        PodcastRow(p, onClick = { onOpenPodcast(p) })
                    }
                }
            } else if (subs.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 60.dp, start = 32.dp, end = 32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(
                                R.string.podcasts_get_started
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
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
private fun PodcastRow(podcast: Podcast, onClick: () -> Unit) {
    val podcastTitle =
        if (podcast.displayTitle.isBlank()) {
            stringResource(
                R.string.podcasts_default_show
            )
        } else {
            podcast.displayTitle
        }

    Row(
        Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Artwork(podcast.imageUrl.orEmpty(), accentFor(podcast.feedUrl), Modifier.size(56.dp), corner = 12.dp)
            if (podcast.imageUrl.isNullOrBlank()) Icon(Icons.Filled.Podcasts, null, tint = Color.White, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                podcastTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            podcast.author?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
