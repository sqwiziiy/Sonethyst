package com.mentality.sonethyst.data

import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.util.accentFor

// nullable-with-default fields per gson rule feedUrl is the stable id
data class Podcast(
    val feedUrl: String = "",
    val title: String? = "",
    val author: String? = "",
    val imageUrl: String? = "",
    val description: String? = "",
) {
    val displayTitle: String
        get() =
            title
                ?.takeIf {
                    it.isNotBlank()
                }
                .orEmpty()
}

data class PodcastEpisode(
    val id: String = "",
    val title: String = "",
    val audioUrl: String = "",
    val imageUrl: String = "",
    val durationSec: Int = 0,
    val pubDateMs: Long = 0,
    val description: String = "",
    val podcastTitle: String = "",
)

fun PodcastEpisode.toSong(
    podcastImage: String = "",
    fallbackTitle: String = "",
): Song = Song(
    id = "podcast:$id",
    title =
        if (title.isBlank()) {
            fallbackTitle
        } else {
            title
        },
    artist = podcastTitle,
    album = podcastTitle,
    artworkUrl = imageUrl.ifBlank { podcastImage },
    durationSec = durationSec,
    streamUrl = audioUrl,
    accent = accentFor("podcast:$id"),
)

fun Song.isPodcast(): Boolean = id.startsWith("podcast:")
