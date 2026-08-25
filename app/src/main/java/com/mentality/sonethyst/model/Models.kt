package com.mentality.sonethyst.model

import androidx.compose.ui.graphics.Color

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
)

data class LyricLine(val timeSec: Int, val text: String)

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
