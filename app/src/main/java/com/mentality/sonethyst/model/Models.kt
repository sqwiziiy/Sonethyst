package com.mentality.sonethyst.model

import androidx.compose.ui.graphics.Color

const val UNKNOWN_TITLE_SENTINEL = "\u0000sonethyst_unknown_title"

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val artworkUrl: String,
    val durationSec: Int,
    val liked: Boolean = false,
    val explicit: Boolean = false,
    val accent: Color = Color(0xFF28D572),
    val streamUrl: String = "",
    val albumId: String = "",
    val artistId: String = "",
    val suffix: String = "",
    val bitrateKbps: Int = 0,
    val sampleRateHz: Int = 0,
    val bitDepth: Int = 0,
    val replayGainTrack: Float = 0f,
    val replayGainAlbum: Float = 0f,
    val genres: List<String> = emptyList(),
    val rating: Int = 0,            // 0 = unrated, 1..5 = personal rating
    val path: String = "",   // source file path when the backend exposes one (M3U export)

    /*
     * Metadata identity.
     *
     * `artist` remains the ready-to-display legacy string.
     * `artists` preserves individual track artists when the
     * source actually exposes them.
     *
     * Album Artist is intentionally independent: a compilation
     * can contain many track artists while belonging to one
     * album artist.
     */
    val albumArtist: String = "",
    val artists: List<String> = emptyList(),
)

data class Album(
    val id: String,
    val title: String,
    val artist: String,
    val artworkUrl: String,
    val year: Int,
    val songCount: Int,
)

data class Artist(
    val id: String,
    val name: String,
    val imageUrl: String,
    val monthlyListeners: Long,
)

data class Genre(
    val id: String,
    val name: String,
    val songCount: Int = 0,
)

data class CustomTag(
    val name: String,
    val songCount: Int = 0,
)

data class Playlist(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val songCount: Int,
    val accent: Color = Color(0xFF28D572),
    val folderId: String = "",
)

data class LyricLine(
    val timeSec: Int,
    val text: String,
    val timeMs: Long =
        if (timeSec >= 0) {
            timeSec.toLong() * 1000L
        } else {
            -1L
        },
)

data class DetailInfo(
    val title: String,
    val subtitle: String,
    val artUrl: String,
    val accent: Color,
    val isArtist: Boolean,
    val songCount: Int,
    val typeLabel: String,
    val playlistCoverMode: String = "",
)

enum class LibraryFilter(val label: String) {
    ALL("All"),
    PLAYLISTS("Playlists"),
    ALBUMS("Albums"),
    ARTISTS("Artists"),
    GENRES("Genres"),
    TAGS("Tags"),
    VERSIONS("Versions"),
    HIDDEN("Hidden"),
    SONGS("Songs"),
    DOWNLOADED("Downloaded"),
}

enum class LibrarySort(val label: String) {
    RECENT("Recently added"),
    ALPHABETICAL("Alphabetical"),
    CREATOR("Creator"),
    MOST_PLAYED("Most played"),
}

enum class LibraryLayout { LIST, GRID }
