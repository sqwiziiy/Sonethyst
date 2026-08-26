package com.mentality.sonethyst.data

import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.util.TrackMatch
import java.util.Locale

data class SongVersion(
    val song: Song,
    val label: String,
    val kind: String,
    val isOriginal: Boolean,
)

data class SongVersionGroup(
    val title: String,
    val artist: String,
    val versions: List<SongVersion>,
)

/*
 * Groups alternate versions/edits of one song without treating them as
 * duplicate files.
 *
 * Exact/near-exact copies are collapsed inside a semantic version so the
 * Duplicates feature remains responsible for duplicate management.
 */
object SongVersionFinder {

    private data class Parsed(
        val song: Song,
        val baseTitle: String,
        val descriptors: List<String>,
        val kind: String?,
    ) {
        val explicitVersion: Boolean
            get() = descriptors.isNotEmpty()
    }

    private val bracketSuffix =
        Regex(
            """^(.*?)\s*[\(\[]([^()\[\]]+)[\)\]]\s*$"""
        )

    private val dashSuffix =
        Regex(
            """^(.*?)\s+[–—-]\s+(.+?)\s*$"""
        )

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
    ): List<SongVersionGroup> =
        songs
            .distinctBy { it.id }
            .map(::parse)
            .filter { parsed ->
                val artist =
                    TrackMatch.norm(
                        parsed.song.artist
                    )

                TrackMatch
                    .norm(parsed.baseTitle)
                    .isNotBlank() &&
                    artist.isNotBlank() &&
                    artist != "unknown artist" &&
                    artist != "unknown"
            }
            .groupBy { parsed ->
                "${TrackMatch.norm(parsed.song.artist)}|" +
                    TrackMatch.norm(parsed.baseTitle)
            }
            .values
            .mapNotNull(::toGroup)
            .sortedWith(
                compareBy<SongVersionGroup> {
                    TrackMatch.norm(it.artist)
                }.thenBy {
                    TrackMatch.norm(it.title)
                }
            )

    private fun parse(
        song: Song,
    ): Parsed {
        var base =
            song.title.trim()

        val descriptors =
            mutableListOf<String>()

        while (base.isNotBlank()) {
            val match =
                bracketSuffix.matchEntire(base)
                    ?: dashSuffix.matchEntire(base)
                    ?: break

            val candidateBase =
                match.groupValues[1]
                    .trim()
                    .trimEnd(
                        '-',
                        '–',
                        '—',
                    )
                    .trim()

            val descriptor =
                match.groupValues[2]
                    .trim()

            if (
                candidateBase.isBlank() ||
                descriptorKind(descriptor) == null
            ) {
                break
            }

            descriptors.add(
                0,
                descriptor,
            )

            base = candidateBase
        }

        val kind =
            descriptors
                .asSequence()
                .mapNotNull(::descriptorKind)
                .firstOrNull()

        return Parsed(
            song = song,
            baseTitle =
                base.ifBlank {
                    song.title.trim()
                },
            descriptors = descriptors,
            kind = kind,
        )
    }

    private fun descriptorKind(
        descriptor: String,
    ): String? {
        val value =
            TrackMatch.norm(descriptor)

        return when {
            value.contains("slowed") &&
                value.contains("reverb") ->
                "slowed_reverb"

            value.contains("slowed") ->
                "slowed"

            value.contains("sped up") ||
                value.contains("speed up") ||
                value.contains("spedup") ->
                "sped_up"

            value.contains("nightcore") ->
                "nightcore"

            value.contains("remix") ->
                "remix"

            value.contains("remaster") ->
                "remaster"

            value.contains("live") ->
                "live"

            value.contains("acoustic") ||
                value.contains("unplugged") ->
                "acoustic"

            value.contains("instrumental") ->
                "instrumental"

            value.contains("extended") ->
                "extended"

            value.contains("radio edit") ->
                "radio_edit"

            value.contains("edit") ->
                "edit"

            value.contains("demo") ->
                "demo"

            value.contains("cover") ->
                "cover"

            value.contains("original mix") ->
                "original_mix"

            value.contains("vip") ->
                "vip"

            value.contains("dub") ->
                "dub"

            value.contains("version") ->
                "version"

            value == "mix" ||
                value.endsWith(" mix") ->
                "mix"

            else ->
                null
        }
    }

    private fun toGroup(
        candidates: List<Parsed>,
    ): SongVersionGroup? {
        val variants =
            collapseDuplicateCopies(
                candidates
            )

        if (variants.size < 2) {
            return null
        }

        val hasExplicitVersion =
            variants.any {
                it.explicitVersion
            }

        val knownDurations =
            variants
                .map {
                    it.song.durationSec
                }
                .filter {
                    it > 0
                }

        val hasAlternateLength =
            if (knownDurations.size >= 2) {
                val longest =
                    knownDurations.maxOrNull()
                        ?: 0

                val shortest =
                    knownDurations.minOrNull()
                        ?: 0

                longest - shortest >
                    TrackMatch.DURATION_TOLERANCE_SEC
            } else {
                false
            }

        if (
            !hasExplicitVersion &&
            !hasAlternateLength
        ) {
            return null
        }

        val unmarkedCount =
            variants.count {
                !it.explicitVersion
            }

        val ordered =
            variants.sortedWith(
                compareBy<Parsed> {
                    when {
                        !it.explicitVersion &&
                            unmarkedCount == 1 ->
                            0

                        !it.explicitVersion ->
                            1

                        else ->
                            2
                    }
                }.thenBy {
                    versionLabel(
                        parsed = it,
                        unmarkedCount = unmarkedCount,
                    ).lowercase(Locale.ROOT)
                }.thenBy {
                    it.song.durationSec
                }
            )

        val versions =
            ordered.map { parsed ->
                val original =
                    !parsed.explicitVersion &&
                        unmarkedCount == 1

                SongVersion(
                    song = parsed.song,
                    label =
                        versionLabel(
                            parsed = parsed,
                            unmarkedCount =
                                unmarkedCount,
                        ),
                    kind =
                        parsed.kind
                            ?: if (original) {
                                "original"
                            } else {
                                "alternate_length"
                            },
                    isOriginal = original,
                )
            }

        val representative =
            ordered.firstOrNull {
                !it.explicitVersion &&
                    unmarkedCount == 1
            } ?: ordered.first()

        return SongVersionGroup(
            title =
                representative.baseTitle,
            artist =
                representative.song.artist,
            versions = versions,
        )
    }

    private fun versionLabel(
        parsed: Parsed,
        unmarkedCount: Int,
    ): String =
        when {
            parsed.descriptors.isNotEmpty() ->
                parsed.descriptors
                    .joinToString(" + ")

            unmarkedCount == 1 ->
                "Original"

            else ->
                "Alternate length"
        }

    private fun collapseDuplicateCopies(
        candidates: List<Parsed>,
    ): List<Parsed> =
        candidates
            .groupBy {
                TrackMatch.norm(
                    it.song.title
                )
            }
            .values
            .flatMap(::clusterSameTitle)
            .map { cluster ->
                cluster.maxByOrNull { parsed ->
                    qualityScore(
                        parsed.song
                    )
                } ?: cluster.first()
            }

    private fun clusterSameTitle(
        candidates: List<Parsed>,
    ): List<List<Parsed>> {
        val result =
            mutableListOf<List<Parsed>>()

        val known =
            candidates
                .filter {
                    it.song.durationSec > 0
                }
                .sortedBy {
                    it.song.durationSec
                }

        val knownClusters =
            mutableListOf<MutableList<Parsed>>()

        for (candidate in known) {
            val current =
                knownClusters.lastOrNull()

            if (
                current != null &&
                candidate.song.durationSec -
                    current.first()
                        .song.durationSec <=
                    TrackMatch.DURATION_TOLERANCE_SEC
            ) {
                current += candidate
            } else {
                knownClusters +=
                    mutableListOf(candidate)
            }
        }

        knownClusters.forEach {
            result += it.toList()
        }

        val unknown =
            candidates.filter {
                it.song.durationSec <= 0
            }

        unknown
            .filter {
                TrackMatch.norm(
                    it.song.album
                ).isNotBlank()
            }
            .groupBy {
                TrackMatch.norm(
                    it.song.album
                )
            }
            .values
            .forEach {
                result += it
            }

        unknown
            .filter {
                TrackMatch.norm(
                    it.song.album
                ).isBlank()
            }
            .forEach {
                result += listOf(it)
            }

        return result
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
            score +=
                1_000_000_000_000L
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
