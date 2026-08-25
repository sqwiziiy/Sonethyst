package com.mentality.sonethyst.data

import com.mentality.sonethyst.model.Album
import com.mentality.sonethyst.model.Artist
import com.mentality.sonethyst.model.DetailInfo
import com.mentality.sonethyst.model.Genre
import android.net.Uri
import androidx.compose.ui.graphics.Color
import com.mentality.sonethyst.model.Playlist
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.util.accentFor
import java.io.File
import java.text.Normalizer
import java.util.Locale

data class HomeData(
    val newReleases: List<Album> = emptyList(),
    val recentlyPlayed: List<Album> = emptyList(),
    val mostPlayed: List<Album> = emptyList(),
    val random: List<Album> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val starred: List<Song> = emptyList(),
)

data class SearchResults(
    val songs: List<Song> = emptyList(),
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
)

data class DetailData(val info: DetailInfo, val tracks: List<Song>, val albums: List<Album> = emptyList())

data class FolderNode(val id: String, val name: String)

data class FolderContent(
    val id: String,
    val title: String,
    val folders: List<FolderNode> = emptyList(),
    val songs: List<Song> = emptyList(),
)

data class DownloadRow(
    val id: String,
    val kind: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val accent: Color,
)

// server-agnostic facade online delegates to backend offline serves downloaded files
class MusicRepository(
    private val backendProvider: () -> MediaBackend?,
    private val downloadManager: DownloadManager,
    private val offlineProvider: () -> Boolean = { false },
    private val currentServerIdProvider: () -> String = { "" },
    private val smartPlaylistsProvider: () -> List<SmartPlaylist> = { emptyList() },
    private val smartEngine: SmartPlaylistEngine? = null,
    private val onPlaylistChanged: () -> Unit = {},
) {
    private val backend: MediaBackend? get() = backendProvider()
    private val offline: Boolean get() = offlineProvider()

    private fun playlistMutationResult(ok: Boolean): Boolean {
        if (ok) onPlaylistChanged()
        return ok
    }

    // offline shows every servers downloads online scopes to the active server
    private fun visibleDownloads(): List<DownloadedSong> {
        val all = downloadManager.downloads.value.values
        val scoped = if (offline) all else all.filter { (it.serverId ?: "") == currentServerIdProvider() }
        return scoped.toList()
    }

    private fun visibleCollections(): List<DownloadedCollection> {
        val all = downloadManager.collections.value
        return if (offline) all else all.filter { (it.serverId ?: "") == currentServerIdProvider() }
    }

    fun downloadedSongs(): List<Song> = visibleDownloads()
        .sortedBy { it.title }.map { it.toSong() }

    private fun fileUri(path: String): String = if (path.isBlank()) "" else Uri.fromFile(File(path)).toString()

    fun downloadedLibrary(): List<DownloadRow> {
        val collections = visibleCollections()
        val colRows = collections.map { DownloadRow(it.id, it.kind, it.title, it.subtitle, fileUri(it.coverPath), accentFor(it.id)) }
        val recordedTracks = collections.flatMap { it.trackIds }.toSet()
        val colIds = collections.map { it.id }.toSet()
        val inferred = visibleDownloads()
            .filter { it.id !in recordedTracks && it.albumId.isNotBlank() && it.albumId !in colIds }
            .groupBy { it.albumId }
            .map { (aid, songs) ->
                val f = songs.first()
                DownloadRow(aid, "album", f.album.ifBlank { "Album" }, f.artist, fileUri(f.coverPath), accentFor(aid))
            }
        return (colRows + inferred).sortedBy { it.title }
    }

    private fun downloadedAlbums(): List<Album> = visibleDownloads()
        .filter { it.albumId.isNotBlank() }
        .groupBy { it.albumId }
        .map { (albumId, songs) ->
            val first = songs.first()
            Album(id = albumId, title = first.album.ifBlank { "Album" }, artist = first.artist, artworkUrl = first.toSong().artworkUrl, year = 0, songCount = songs.size)
        }
        .sortedBy { it.title }

    suspend fun home(): HomeData {
        if (offline) {
            val albums = downloadedAlbums()
            return HomeData(newReleases = albums, recentlyPlayed = albums, starred = downloadedSongs())
        }
        return backend?.home() ?: HomeData()
    }

    suspend fun allAlbums(): List<Album> =
        if (offline) downloadedAlbums() else backend?.allAlbums().orEmpty()

    suspend fun allArtists(): List<Artist> =
        if (offline) emptyList() else backend?.allArtists().orEmpty()

    val supportsGenres: Boolean
        get() =
            !offline &&
                backend?.supportsGenres == true

    val supportsRatings: Boolean
        get() =
            !offline &&
                backend?.supportsRatings == true

    suspend fun allGenres(): List<Genre> =
        if (offline) {
            emptyList()
        } else {
            backend?.allGenres().orEmpty()
        }

    suspend fun allPlaylists(): List<Playlist> =
        if (offline) emptyList() else backend?.allPlaylists().orEmpty()

    suspend fun allSongs(): List<Song> =
        if (offline) downloadedSongs() else backend?.allSongs().orEmpty()

    suspend fun librarySongs(limit: Int = 2000): List<Song> =
        if (offline) downloadedSongs() else backend?.librarySongs(limit).orEmpty()

    suspend fun starredSongs(): List<Song> = backend?.starredSongs().orEmpty()

    suspend fun starredCount(): Int = if (offline) starredSongs().size else (backend?.starredCount() ?: 0)

    suspend fun starredIds(): Set<String> = backend?.starredIds() ?: emptySet()

    suspend fun likedSongIds(ids: List<String>): Set<String> =
        if (offline) emptySet() else backend?.likedSongIds(ids).orEmpty()

    suspend fun profileImageUrl(): String = if (offline) "" else backend?.profileImageUrl().orEmpty()

    suspend fun songFor(id: String): Song? {
        downloadManager.get(id)?.let { return it.toSong() }
        return backend?.songFor(id)
    }

    suspend fun search(query: String): SearchResults {
        if (offline) {
            val q = query.trim()
            val songs = downloadedSongs().filter { it.title.contains(q, true) || it.artist.contains(q, true) || it.album.contains(q, true) }
            val albums = downloadedAlbums().filter { it.title.contains(q, true) || it.artist.contains(q, true) }
            return SearchResults(songs = songs, albums = albums, artists = emptyList())
        }
        return backend?.search(query) ?: SearchResults()
    }

    suspend fun scrobble(id: String) {
        if (offline) return
        backend?.scrobble(id)
    }

    suspend fun radio(seedId: String): List<Song> {
        if (offline) return emptyList()
        return backend?.radio(seedId).orEmpty()
    }

    suspend fun createPlaylist(name: String): Boolean =
        playlistMutationResult(backend?.createPlaylist(name) ?: false)

    suspend fun addToPlaylist(playlistId: String, trackIds: List<String>): Boolean =
        playlistMutationResult(backend?.addToPlaylist(playlistId, trackIds) ?: false)

    suspend fun removeFromPlaylist(playlistId: String, trackIds: List<String>): Boolean =
        playlistMutationResult(backend?.removeFromPlaylist(playlistId, trackIds) ?: false)

    val supportsPlaylistReorder: Boolean
        get() = !offline && backend?.supportsPlaylistReorder == true

    val supportsPlaylistCoverManagement: Boolean
        get() = !offline && backend?.supportsPlaylistCoverManagement == true

    suspend fun setPlaylistCover(
        playlistId: String,
        mode: String,
        value: String? = null,
    ): Boolean =
        playlistMutationResult(
            backend?.setPlaylistCover(playlistId, mode, value) ?: false
        )

    suspend fun reorderPlaylist(playlistId: String, orderedTrackIds: List<String>): Boolean =
        playlistMutationResult(
            backend?.reorderPlaylist(playlistId, orderedTrackIds) ?: false
        )

    suspend fun createPlaylistFromSongs(name: String, trackIds: List<String>): Boolean {
        val b = backend ?: return false
        val id = b.createPlaylistWithId(name) ?: return false

        val result = if (trackIds.isNotEmpty()) {
            b.addToPlaylist(id, trackIds)
        } else {
            true
        }

        // The playlist was created even if adding its first tracks failed,
        // so every cached library/detail screen must refresh.
        onPlaylistChanged()
        return result
    }

    suspend fun exportPlaylist(kind: String, id: String): String? =
        detail(kind, id)?.tracks?.takeIf { it.isNotEmpty() }?.let { M3u.write(it) }

    suspend fun importPlaylist(
        name: String,
        entries: List<M3u.Entry>,
    ): Pair<Int, Int>? {
        if (offline || entries.isEmpty()) return null

        // Import is an infrequent operation, so load the library once and use it
        // for exact path / filename / metadata matching before making searches.
        val library = runCatching {
            allSongs()
        }.getOrDefault(emptyList())

        val matched = entries
            .mapNotNull { matchEntry(it, library) }
            .distinctBy { it.id }

        val playlistId =
            backend?.createPlaylistWithId(name) ?: return null

        if (matched.isNotEmpty()) {
            backend?.addToPlaylist(
                playlistId,
                matched.map { it.id },
            )
        }

        onPlaylistChanged()
        return matched.size to entries.size
    }

    private fun norm(value: String): String =
        Normalizer.normalize(
            value,
            Normalizer.Form.NFKC,
        )
            .lowercase(Locale.ROOT)
            .replace(
                Regex("[^\\p{L}\\p{N}]+"),
                " ",
            )
            .trim()

    private fun locationKey(raw: String): String {
        val input = raw
            .trim()
            .trim('"')

        if (input.isBlank()) return ""

        val parsed = runCatching {
            Uri.parse(input)
        }.getOrNull()

        val pathLike =
            if (
                parsed?.scheme?.equals(
                    "file",
                    ignoreCase = true,
                ) == true
            ) {
                parsed.path.orEmpty()
            } else {
                Uri.decode(input)
            }

        return pathLike
            .replace('\\', '/')
            .replace(Regex("/+"), "/")
            .trim()
            .trimEnd('/')
            .lowercase(Locale.ROOT)
    }

    private fun basenameKey(raw: String): String =
        locationKey(raw).substringAfterLast('/')

    private fun matchScore(
        entry: M3u.Entry,
        song: Song,
    ): Int {
        val titleN = norm(entry.title)
        val artistN = norm(entry.artist)
        val songTitle = norm(song.title)
        val songArtist = norm(song.artist)

        var score = 0

        if (titleN.isNotBlank()) {
            score += when {
                songTitle == titleN -> 8
                songTitle.contains(titleN) ||
                    titleN.contains(songTitle) -> 3
                else -> 0
            }
        }

        if (artistN.isNotBlank()) {
            score += when {
                songArtist == artistN -> 5
                songArtist.contains(artistN) ||
                    artistN.contains(songArtist) -> 3
                else -> 0
            }
        }

        if (entry.durationSec > 0 && song.durationSec > 0) {
            val diff = kotlin.math.abs(
                song.durationSec - entry.durationSec
            )

            score += when {
                diff <= 2 -> 3
                diff <= 5 -> 1
                else -> 0
            }
        }

        return score
    }

    private fun bestMatch(
        entry: M3u.Entry,
        candidates: List<Song>,
        minimumScore: Int,
    ): Song? =
        candidates
            .map { song ->
                song to matchScore(entry, song)
            }
            .filter { (_, score) ->
                score >= minimumScore
            }
            .maxByOrNull { (_, score) ->
                score
            }
            ?.first

    private suspend fun matchEntry(
        entry: M3u.Entry,
        library: List<Song>,
    ): Song? {
        /*
         * Strongest match:
         * actual M3U path against MediaStore/server path.
         *
         * Relative M3U paths are supported through endsWith().
         */
        val entryLocation = locationKey(entry.location)

        if (entryLocation.isNotBlank()) {
            val pathMatches = library.filter { song ->
                val songPath = locationKey(song.path)

                songPath.isNotBlank() &&
                    (
                        songPath == entryLocation ||
                            (
                                entryLocation.contains('/') &&
                                    songPath.endsWith(
                                        "/$entryLocation"
                                    )
                            )
                    )
            }

            if (pathMatches.size == 1) {
                return pathMatches.first()
            }

            bestMatch(
                entry,
                pathMatches,
                minimumScore = 1,
            )?.let {
                return it
            }

            /*
             * A playlist moved between devices commonly has a different
             * absolute root but the same filename.
             */
            val entryBasename =
                entryLocation.substringAfterLast('/')

            if (entryBasename.isNotBlank()) {
                val filenameMatches = library.filter { song ->
                    song.path.isNotBlank() &&
                        basenameKey(song.path) == entryBasename
                }

                if (filenameMatches.size == 1) {
                    return filenameMatches.first()
                }

                bestMatch(
                    entry,
                    filenameMatches,
                    minimumScore = 3,
                )?.let {
                    return it
                }
            }
        }

        /*
         * Next try the already-loaded library. This avoids a server request
         * for every entry and works with all Unicode scripts.
         */
        bestMatch(
            entry,
            library,
            minimumScore = 8,
        )?.let {
            return it
        }

        /*
         * Final fallback for backends whose allSongs() result is incomplete.
         */
        val query = listOf(
            entry.artist,
            entry.title,
        )
            .filter { it.isNotBlank() }
            .joinToString(" ")

        if (query.isBlank()) return null

        val searched = search(query).songs.ifEmpty {
            if (entry.title.isNotBlank()) {
                search(entry.title).songs
            } else {
                emptyList()
            }
        }

        return bestMatch(
            entry,
            searched,
            minimumScore = 8,
        )
    }

    suspend fun updatePlaylist(id: String, name: String?, comment: String?): Boolean =
        playlistMutationResult(backend?.updatePlaylist(id, name, comment) ?: false)

    suspend fun deletePlaylist(id: String): Boolean =
        playlistMutationResult(backend?.deletePlaylist(id) ?: false)

    suspend fun setRating(
        id: String,
        rating: Int,
    ): Boolean {
        if (offline) return false

        return backend?.setRating(
            id,
            rating.coerceIn(0, 5),
        ) ?: false
    }

    // playlists arent server-starrable handled locally by the caller
    suspend fun setStarred(id: String, starred: Boolean, kind: String = "song"): Boolean =
        backend?.setStarred(id, starred, kind) ?: false

    suspend fun detail(kind: String, id: String): DetailData? {
        if (kind == "genre") {
            if (offline) return null

            val b = backend ?: return null

            val tracks =
                b.songsByGenre(
                    genreId = id,
                    limit = 200,
                    offset = 0,
                )

            val total =
                b.genreSongCount(id)
                    ?.coerceAtLeast(tracks.size)
                    ?: tracks.size

            return DetailData(
                info =
                    DetailInfo(
                        title = id,
                        subtitle =
                            "$total song${if (total == 1) "" else "s"}",
                        artUrl =
                            tracks.firstOrNull()
                                ?.artworkUrl
                                .orEmpty(),
                        accent = accentFor("genre:$id"),
                        isArtist = false,
                        songCount = total,
                        typeLabel = "Genre",
                    ),
                tracks = tracks,
            )
        }

        if (kind == "smart") {
            val sp = smartPlaylistsProvider().firstOrNull { it.id == id } ?: return null
            val tracks = smartEngine?.evaluate(sp, librarySongs()).orEmpty()
            return DetailData(
                DetailInfo(sp.name ?: "Smart playlist", "Smart playlist • ${tracks.size} songs", tracks.firstOrNull()?.artworkUrl ?: "", accentFor(id), false, tracks.size, "Smart playlist"),
                tracks,
            )
        }
        if (offline) {
            val dls = downloadedSongs()
            return when (kind) {
                "album" -> dls.filter { it.albumId == id }.takeIf { it.isNotEmpty() }?.let { tracks ->
                    val f = tracks.first()
                    DetailData(DetailInfo(f.album.ifBlank { "Album" }, "${f.artist} • Downloaded", f.artworkUrl, accentFor(id), false, tracks.size, "Album"), tracks)
                }
                "artist" -> dls.filter { it.artistId == id }.takeIf { it.isNotEmpty() }?.let { tracks ->
                    DetailData(DetailInfo(tracks.first().artist, "${tracks.size} downloaded tracks", tracks.first().artworkUrl, accentFor(id), true, tracks.size, "Artist"), tracks)
                }
                "playlist" -> downloadManager.collections.value.firstOrNull { it.id == id }?.let { col ->
                    val byId = downloadManager.downloads.value
                    val tracks = col.trackIds.mapNotNull { byId[it]?.toSong() }
                    DetailData(DetailInfo(col.title, col.subtitle, fileUri(col.coverPath), accentFor(id), false, tracks.size, "Playlist"), tracks)
                }
                else -> null
            }
        }
        return backend?.detail(kind, id)
    }

    suspend fun detailPage(
        kind: String,
        id: String,
        offset: Int,
    ): List<Song> {
        if (offline) return emptyList()

        return if (kind == "genre") {
            backend?.songsByGenre(
                genreId = id,
                limit = 200,
                offset = offset,
            ).orEmpty()
        } else {
            backend?.detailPage(
                kind,
                id,
                offset,
            ).orEmpty()
        }
    }

    val supportsFolders: Boolean get() = !offline && backend?.supportsFolders == true

    val supportsServerTagEdit: Boolean get() = !offline && backend?.supportsServerTagEdit == true

    suspend fun readMetadata(songId: String): AudioTags? =
        if (offline) null else backend?.readMetadata(songId)

    suspend fun updateMetadata(songId: String, tags: AudioTags): Boolean =
        if (offline) false else backend?.updateMetadata(songId, tags) ?: false

    suspend fun browseFolder(folderId: String): FolderContent? =
        if (offline) null else backend?.browseFolder(folderId)
}
