package com.mentality.sonethyst.data

import android.content.Context
import android.util.Base64
import com.mentality.sonethyst.R

import com.mentality.sonethyst.model.Album
import com.mentality.sonethyst.model.Artist
import com.mentality.sonethyst.model.DetailInfo
import com.mentality.sonethyst.model.Genre
import com.mentality.sonethyst.model.Playlist
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.util.accentFor

internal fun activeLikeIds(
    persistedIds: Set<String>,
    availableIds: Set<String>,
): Set<String> =
    persistedIds.filterTo(LinkedHashSet()) { it in availableIds }

// mediabackend over on-device files only server-only ops are no-ops
class LocalBackend(
    private val context: Context,
    private val library: LocalLibrary,
    private val store: LocalStore,
    override val session: Session,
) : MediaBackend {

    private fun songCountText(count: Int): String =
        context.resources.getQuantityString(
            R.plurals.backend_song_count,
            count,
            count,
        )

    private fun Song.withRating(): Song {
        val stored = store.rating(id)

        return if (rating == stored) {
            this
        } else {
            copy(rating = stored)
        }
    }

    private fun List<Song>.withRatings(): List<Song> =
        map { song -> song.withRating() }

    private fun collageCover(tracks: List<Song>): String {
        val artwork = tracks
            .map { it.artworkUrl }
            .filter { it.isNotBlank() }
            .distinct()
            .take(4)

        if (artwork.size < 4) {
            return artwork.firstOrNull().orEmpty()
        }

        val encoded = artwork.map { url ->
            Base64.encodeToString(
                url.toByteArray(Charsets.UTF_8),
                Base64.URL_SAFE or Base64.NO_WRAP,
            )
        }

        return "sonethyst-collage:" + encoded.joinToString(".")
    }

    private fun automaticCover(tracks: List<Song>): String {
        val distinctArtwork = tracks
            .map { it.artworkUrl }
            .filter { it.isNotBlank() }
            .distinct()

        return if (distinctArtwork.size >= 4) {
            collageCover(tracks)
        } else {
            distinctArtwork.firstOrNull().orEmpty()
        }
    }

    private fun LocalPlaylist.resolvedCover(tracks: List<Song>): String {
        val automatic = automaticCover(tracks)

        return when (coverMode?.lowercase()?.ifBlank { "automatic" }) {
            "first" ->
                tracks.firstOrNull()?.artworkUrl.orEmpty()

            "collage" ->
                collageCover(tracks)

            "track" ->
                tracks.firstOrNull { it.id == coverValue }
                    ?.artworkUrl
                    ?.takeIf { it.isNotBlank() }
                    ?: automatic

            "custom" ->
                coverValue
                    ?.takeIf { it.isNotBlank() }
                    ?: automatic

            else ->
                automatic
        }
    }

    private fun LocalPlaylist.toPlaylist(): Playlist {
        val tracks = trackIds.orEmpty().mapNotNull { library.song(it) }
        return Playlist(
            id = id,
            title = title ?: "",
            subtitle =
                (subtitle ?: "").ifBlank {
                    songCountText(tracks.size)
                },
            coverUrl = resolvedCover(tracks),
            songCount = tracks.size,
            accent = accentFor(id),
        )
    }

    override fun customTagKey(
        id: String,
    ): String =
        "local$CUSTOM_TAG_KEY_SEP$id"

    override val customTagScopes: Set<String>
        get() = setOf("local")

    override suspend fun songForCustomTagKey(
        key: String,
    ): Song? {
        val prefix = "local$CUSTOM_TAG_KEY_SEP"

        if (!key.startsWith(prefix)) {
            return null
        }

        return songFor(
            key.removePrefix(prefix)
        )
    }

    override suspend fun ping(): Boolean = true

    override suspend fun home(): HomeData {
        library.ensureLoaded()
        val albums = library.albums
        return HomeData(
            newReleases = albums.take(20),                                   // locallibrary already sorts by date added
            mostPlayed = albums.sortedByDescending { it.songCount }.take(20),
            playlists = store.playlists().map { it.toPlaylist() },
            artists = library.artists.take(40),
            starred = starredSongs().take(40),
        )
    }

    override suspend fun allAlbums(): List<Album> { library.ensureLoaded(); return library.albums }
    override suspend fun allArtists(): List<Artist> { library.ensureLoaded(); return library.artists }
    override suspend fun allPlaylists(): List<Playlist> = store.playlists().map { it.toPlaylist() }

    override val supportsGenres: Boolean get() = true

    override val supportsRatings: Boolean get() = true

    override suspend fun allGenres(): List<Genre> {
        library.ensureLoaded()
        return library.genres
    }

    override suspend fun songsByGenre(
        genreId: String,
        limit: Int,
        offset: Int,
    ): List<Song> {
        library.ensureLoaded()
        return library.songsByGenre(genreId)
            .drop(offset.coerceAtLeast(0))
            .take(limit.coerceAtLeast(0))
            .withRatings()
    }

    override suspend fun genreSongCount(
        genreId: String,
    ): Int {
        library.ensureLoaded()
        return library.songsByGenre(genreId).size
    }
    override suspend fun allSongs(): List<Song> {
        library.ensureLoaded()
        return library.songs.withRatings()
    }

    override suspend fun librarySongs(limit: Int): List<Song> {
        library.ensureLoaded()
        return library.songs
            .take(limit.coerceAtLeast(0))
            .withRatings()
    }

    override suspend fun starredSongs(): List<Song> {
        library.ensureLoaded()
        val liked = activeStarredIds()
        return library.songs
            .filter { it.id in liked }
            .withRatings()
    }

    override suspend fun starredCount(): Int = starredSongs().size

    override suspend fun starredIds(): Set<String> = activeStarredIds()

    override suspend fun songFor(id: String): Song? {
        library.ensureLoaded()
        return library.song(id)?.withRating()
    }

    override suspend fun likedSongIds(ids: List<String>): Set<String> {
        val liked = activeStarredIds()
        return ids.filterTo(HashSet()) { it in liked }
    }

    private suspend fun activeStarredIds(): Set<String> {
        library.ensureLoaded()
        val availableIds = library.songs.asSequence().mapTo(HashSet()) { it.id }
        return activeLikeIds(store.likedIds(), availableIds)
    }

    override suspend fun search(query: String): SearchResults {
        library.ensureLoaded()
        val q = query.trim()
        if (q.isBlank()) return SearchResults()
        return SearchResults(
            songs =
                library.songs
                    .filter {
                        it.title.contains(q, true) ||
                            it.artist.contains(q, true) ||
                            it.album.contains(q, true)
                    }
                    .take(60)
                    .withRatings(),
            albums = library.albums.filter { it.title.contains(q, true) || it.artist.contains(q, true) }.take(30),
            artists = library.artists.filter { it.name.contains(q, true) }.take(30),
            playlists = store.playlists().map { it.toPlaylist() }.filter { it.title.contains(q, true) }.take(20),
        )
    }

    override suspend fun scrobble(id: String) { /* last.fm handled separately */ }

    override suspend fun radio(seedId: String): List<Song> {
        library.ensureLoaded()
        val seed =
            library.song(seedId)

        val sameArtist =
            seed
                ?.let { song ->
                    library
                        .primaryArtistId(song)
                        ?.let {
                            library
                                .songsByArtistId(it)
                        }
                }
                .orEmpty()
                .filter {
                    it.id != seedId
                }

        val rest =
            library.songs
                .filter {
                    it.id != seedId &&
                        it !in sameArtist
                }
                .shuffled()
        return (sameArtist.shuffled() + rest)
            .take(40)
            .withRatings()
    }

    override suspend fun detail(kind: String, id: String): DetailData? {
        library.ensureLoaded()
        return when (kind) {
            "album" -> {
                val tracks = library.songsByAlbumId(id).withRatings()
                val album = library.albums.firstOrNull { it.id == id } ?: return null
                DetailData(
                    info = DetailInfo(album.title, album.artist, album.artworkUrl, accentFor(id), false, tracks.size, "Album"),
                    tracks = tracks,
                )
            }
            "artist" -> {
                // Keep old queue/download records with MediaStore numeric IDs
                // resolvable while new scans use logical local artist IDs.
                val artistId =
                    if (library.artists.any { it.id == id }) id
                    else library.songs.firstOrNull { it.artistId == id }?.let(library::primaryArtistId)
                val artist = artistId?.let { resolved -> library.artists.firstOrNull { it.id == resolved } } ?: return null
                val tracks = library.songsByArtistId(artist.id).withRatings()
                val albums = library.albumsByArtistId(artist.id)
                DetailData(
                    info = DetailInfo(
                        artist.name,
                        songCountText(tracks.size),
                        artist.imageUrl,
                        accentFor(id),
                        true,
                        tracks.size,
                        "Artist",
                    ),
                    tracks = tracks,
                    albums = albums,
                )
            }
            "playlist" -> {
                val pl = store.playlist(id) ?: return null
                val tracks =
                    pl.trackIds.orEmpty()
                        .mapNotNull { library.song(it) }
                        .withRatings()
                DetailData(
                    info = DetailInfo(
                        pl.title ?: "",
                        pl.subtitle ?: "",
                        pl.resolvedCover(tracks),
                        accentFor(id),
                        false,
                        tracks.size,
                        "Playlist",
                        pl.coverMode?.ifBlank { "automatic" } ?: "automatic",
                    ),
                    tracks = tracks,
                )
            }
            "liked" -> {
                val tracks = starredSongs()
                DetailData(
                    info = DetailInfo(
                        context.getString(
                            R.string.backend_liked_songs
                        ),
                        songCountText(tracks.size),
                        tracks.firstOrNull()?.artworkUrl
                            ?: "",
                        accentFor("liked"),
                        false,
                        tracks.size,
                        "Liked",
                    ),
                    tracks = tracks,
                )
            }
            else -> null
        }
    }

    override suspend fun setRating(
        id: String,
        rating: Int,
    ): Boolean =
        store.setRating(
            id,
            rating.coerceIn(0, 5),
        )

    override suspend fun setStarred(id: String, starred: Boolean, kind: String): Boolean = store.setLiked(id, starred)

    override suspend fun createPlaylist(name: String): Boolean { store.createPlaylist(name); return true }

    override suspend fun createPlaylistWithId(name: String): String? = store.createPlaylist(name)

    override suspend fun updatePlaylist(id: String, name: String?, comment: String?): Boolean {
        store.updatePlaylist(id, name, comment); return true
    }

    override suspend fun deletePlaylist(id: String): Boolean { store.deletePlaylist(id); return true }

    override suspend fun addToPlaylist(playlistId: String, trackIds: List<String>): Boolean { store.addTracks(playlistId, trackIds); return true }
    override suspend fun removeFromPlaylist(playlistId: String, trackIds: List<String>): Boolean { store.removeTracks(playlistId, trackIds); return true }

    override val supportsPlaylistReorder: Boolean get() = true

    override val supportsPlaylistCoverManagement: Boolean get() = true

    override suspend fun setPlaylistCover(
        playlistId: String,
        mode: String,
        value: String?,
    ): Boolean {
        store.setPlaylistCover(playlistId, mode, value)
        return true
    }

    override suspend fun reorderPlaylist(
        playlistId: String,
        orderedTrackIds: List<String>,
    ): Boolean {
        store.reorderTracks(playlistId, orderedTrackIds)
        return true
    }

    // folder tree rooted at the deepest common directory

    override val supportsFolders: Boolean get() = true

    override suspend fun browseFolder(folderId: String): FolderContent? {
        library.ensureLoaded()
        val base = folderId.ifBlank { library.folderRoot }
        if (base.isBlank()) return null
        val (subdirs, tracks) = library.browse(base)
        return FolderContent(
            id = base,
            title =
                if (folderId.isBlank()) {
                    context.getString(
                        R.string.backend_folders
                    )
                } else {
                    base.substringAfterLast('/')
                },
            folders = subdirs.map { FolderNode("$base/$it", it) },
            songs = tracks.withRatings(),
        )
    }

    override suspend fun serverLyrics(song: Song): Lyrics? = null

    override fun streamUrl(songId: String, maxBitrate: Int, lossless: Boolean): String =
        library.song(songId)?.streamUrl ?: ""

    override fun coverArtUrl(id: String, size: Int): String =
        library.albums.firstOrNull { it.id == id }?.artworkUrl
            ?: library.song(id)?.artworkUrl ?: ""
}
