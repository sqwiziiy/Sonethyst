package com.mentality.sonethyst.data

import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.util.TrackMatch
import java.util.Locale

data class DuplicateGroup(
    val title: String,
    val artist: String,
    val songs: List<Song>,
)

/*
 * Metadata-first duplicate detection.
 *
 * This intentionally detects likely copies of the same recording rather
 * than collapsing remixes/live/slowed/remastered/etc. Version semantics
 * are handled separately by the next Phase 2 feature.
 */
object DuplicateFinder {

    private val losslessFormats =
        setOf(
            "flac",
            "alac",
            "wav",
            "wave",
            "aiff",
            "aif",
        )

    fun find(
        songs: List<Song>,
    ): List<DuplicateGroup> =
        songs
            .distinctBy { it.id }
            .groupBy {
                TrackMatch.key(
                    it.artist,
                    it.title,
                )
            }
            .values
            .filter { group ->
                group.size >= 2 &&
                    TrackMatch
                        .norm(group.first().title)
                        .isNotBlank()
            }
            .flatMap(::clusterByDuration)
            .filter {
                it.songs.size >= 2
            }
            .sortedWith(
                compareBy<DuplicateGroup> {
                    TrackMatch.norm(it.artist)
                }.thenBy {
                    TrackMatch.norm(it.title)
                }
            )

    private fun clusterByDuration(
        group: List<Song>,
    ): List<DuplicateGroup> {
        val known =
            group
                .filter { it.durationSec > 0 }
                .sortedBy { it.durationSec }

        val clusters =
            ArrayList<MutableList<Song>>()

        for (song in known) {
            val current =
                clusters.lastOrNull()

            if (
                current != null &&
                song.durationSec -
                    current.first().durationSec <=
                    TrackMatch.DURATION_TOLERANCE_SEC
            ) {
                current += song
            } else {
                clusters += mutableListOf(song)
            }
        }

        val result =
            clusters
                .filter { it.size >= 2 }
                .map(::toGroup)
                .toMutableList()

        /*
         * Unknown durations must not be mixed blindly with normal tracks.
         * Only consider them duplicates when the album metadata also
         * agrees. This trades a few false negatives for far fewer
         * destructive false positives.
         */
        group
            .filter { it.durationSec <= 0 }
            .groupBy {
                TrackMatch.norm(it.album)
            }
            .filter { (album, copies) ->
                album.isNotBlank() &&
                    copies.size >= 2
            }
            .values
            .mapTo(result, ::toGroup)

        return result
    }

    private fun toGroup(
        songs: List<Song>,
    ): DuplicateGroup {
        val ordered =
            songs.sortedWith(
                compareByDescending<Song>(
                    ::qualityScore
                ).thenBy {
                    it.album.lowercase(Locale.ROOT)
                }
            )

        val representative =
            ordered.first()

        return DuplicateGroup(
            title = representative.title,
            artist = representative.artist,
            songs = ordered,
        )
    }

    private fun qualityScore(
        song: Song,
    ): Long {
        var score = 0L

        if (
            song.suffix
                .lowercase(Locale.ROOT) in
                losslessFormats
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
}
