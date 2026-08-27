package com.mentality.sonethyst.data

import android.net.Uri
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.util.accentFor

// nullable-with-default per gson rule uuid always written
data class RadioStation(
    val uuid: String = "",
    val name: String? = "",
    val streamUrl: String? = "",
    val faviconUrl: String? = "",
    val tags: String? = "",
    val country: String? = "",
    val codec: String? = "",
    val bitrate: Int? = 0,
    val homepage: String? = "",
    val custom: Boolean? = false,
) {
    val displayName: String get() = name?.takeIf { it.isNotBlank() } ?: "Radio station"
    val genre: String get() = tags?.split(",")?.firstOrNull()?.trim()?.takeIf { it.isNotBlank() }
        ?: country?.takeIf { it.isNotBlank() }
        ?: "Internet radio"
}

fun normalizedRadioStreamUrl(
    raw: String?,
): String {
    val input =
        raw.orEmpty()
            .trim()

    if (input.isBlank()) {
        return ""
    }

    val uri =
        runCatching {
            Uri.parse(input)
        }.getOrNull()
            ?: return input
                .trimEnd('/')
                .lowercase()

    val scheme =
        uri.scheme
            ?.lowercase()
            .orEmpty()

    val host =
        uri.host
            ?.lowercase()
            .orEmpty()

    if (
        scheme.isBlank() ||
        host.isBlank()
    ) {
        return input
            .trimEnd('/')
            .lowercase()
    }

    val port =
        uri.port

    val defaultPort =
        (scheme == "http" && port == 80) ||
            (scheme == "https" && port == 443)

    val authority =
        if (
            port >= 0 &&
            !defaultPort
        ) {
            "$host:$port"
        } else {
            host
        }

    val path =
        uri.encodedPath
            .orEmpty()
            .replace(
                Regex("/+"),
                "/",
            )
            .trimEnd('/')

    /*
     * Ignore fragments. Preserve query because radio endpoints
     * sometimes require tokens/parameters to identify the stream.
     */
    val query =
        uri.encodedQuery
            ?.takeIf {
                it.isNotBlank()
            }
            ?.let {
                "?$it"
            }
            .orEmpty()

    return "$scheme://$authority$path$query"
}

val BUILTIN_RADIO_FALLBACKS =
    listOf(
        RadioStation(
            uuid =
                "fallback:melodiafm-international",
            name =
                "Мелодія FM International",
            streamUrl =
                "https://online.melodiafm.ua/MelodiaFM_Int_HD",
            tags =
                "pop,90s,2000s,international",
            country =
                "Ukraine",
            codec =
                "MP3",
            bitrate =
                320,
            homepage =
                "https://play.tavr.media/melodiafm/int/",
            custom =
                false,
        ),
    )

fun dedupeRadioStations(
    stations: List<RadioStation>,
): List<RadioStation> {
    val seen =
        mutableSetOf<String>()

    return stations.filter { station ->
        val key =
            normalizedRadioStreamUrl(
                station.streamUrl
            )

        key.isNotBlank() &&
            seen.add(key)
    }
}

// durationSec 0 marks non-seekable live stream
fun RadioStation.toSong(): Song = Song(
    id = "radio:$uuid",
    title = displayName,
    artist = genre,
    album = "Internet radio",
    artworkUrl = faviconUrl.orEmpty(),
    durationSec = 0,
    streamUrl = streamUrl.orEmpty(),
    accent = accentFor("radio:$uuid"),
)

fun Song.isRadio(): Boolean = id.startsWith("radio:")
