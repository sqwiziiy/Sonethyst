package com.mentality.sonethyst.data

import com.mentality.sonethyst.model.Album
import com.mentality.sonethyst.model.Artist
import com.mentality.sonethyst.model.Genre
import com.mentality.sonethyst.model.Playlist
import com.mentality.sonethyst.model.Song

// returns app domain models so callers are server-agnostic backend owns dto mapping auth and urls
interface MediaBackend {
    val session: Session

    suspend fun ping(): Boolean

    suspend fun home(): HomeData
    suspend fun allAlbums(): List<Album>
    suspend fun allArtists(): List<Artist>
    suspend fun allPlaylists(): List<Playlist>
    val supportsGenres: Boolean get() = false

    suspend fun allGenres(): List<Genre> = emptyList()

    val supportsRatings: Boolean get() = false

    suspend fun setRating(
        id: String,
        rating: Int,
    ): Boolean = false

    suspend fun allSongs(): List<Song>

    suspend fun songsByGenre(
        genreId: String,
        limit: Int = 200,
        offset: Int = 0,
    ): List<Song> = emptyList()

    suspend fun genreSongCount(
        genreId: String,
    ): Int? = null

    suspend fun librarySongs(limit: Int = 2000): List<Song> = allSongs()
    suspend fun starredSongs(): List<Song>
    suspend fun starredCount(): Int
    suspend fun starredIds(): Set<String>
    suspend fun songFor(id: String): Song?
    suspend fun search(query: String): SearchResults
    suspend fun scrobble(id: String)
    suspend fun radio(seedId: String): List<Song>
    suspend fun createPlaylist(name: String): Boolean
    suspend fun updatePlaylist(id: String, name: String?, comment: String?): Boolean
    suspend fun deletePlaylist(id: String): Boolean

    suspend fun createPlaylistWithId(name: String): String? = null

    suspend fun addToPlaylist(playlistId: String, trackIds: List<String>): Boolean = false
    suspend fun removeFromPlaylist(playlistId: String, trackIds: List<String>): Boolean = false
    suspend fun reorderPlaylist(playlistId: String, orderedTrackIds: List<String>): Boolean = false
    val supportsPlaylistReorder: Boolean get() = false
    val supportsPlaylistCoverManagement: Boolean get() = false

    suspend fun setPlaylistCover(
        playlistId: String,
        mode: String,
        value: String? = null,
    ): Boolean = false
    suspend fun setStarred(id: String, starred: Boolean, kind: String): Boolean
    suspend fun detail(kind: String, id: String): DetailData?

    suspend fun detailPage(kind: String, id: String, offset: Int): List<Song> = emptyList()

    suspend fun likedSongIds(ids: List<String>): Set<String> = emptySet()

    suspend fun profileImageUrl(): String = ""

    val supportsFolders: Boolean get() = false

    // folderId "" = root
    suspend fun browseFolder(folderId: String): FolderContent? = null

    val supportsServerTagEdit: Boolean get() = false

    suspend fun readMetadata(songId: String): AudioTags? = null

    suspend fun updateMetadata(songId: String, tags: AudioTags): Boolean = false

    suspend fun serverLyrics(song: Song): Lyrics?

    // lossless serves the original untouched file
    fun streamUrl(songId: String, maxBitrate: Int, lossless: Boolean): String

    fun coverArtUrl(id: String, size: Int = 600): String
}
