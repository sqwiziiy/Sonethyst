package com.mentality.sonethyst.data

import com.mentality.sonethyst.model.Album
import com.mentality.sonethyst.model.Artist
import com.mentality.sonethyst.model.CustomTag
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
    private val localStore: LocalStore,
    private val offlineProvider: () -> Boolean = { false },
    private val currentServerIdProvider: () -> String = { "" },
    private val smartPlaylistsProvider: () -> List<SmartPlaylist> = { emptyList() },
    private val smartEngine: SmartPlaylistEngine? = null,
    private val onPlaylistChanged: () -> Unit = {},
) {
    private val backend: MediaBackend? get() = backendProvider()
    private val offline: Boolean get() = offlineProvider()

    private fun playlistStorageKey(
        playlistId: String,
    ): String? {
        if (playlistId.isBlank()) {
            return null
        }

        val activeBackend =
            backend ?: return null

        return activeBackend
            .customTagKey(playlistId)
            .takeIf {
                it.isNotBlank()
            }
    }

    fun playlistFolders(): List<PlaylistFolder> =
        localStore.playlistFolders()

    fun createPlaylistFolder(
        name: String,
        parentId: String = "",
    ): String? {
        val id =
            localStore.createPlaylistFolder(
                name = name,
                parentId = parentId,
            )

        if (id != null) {
            onPlaylistChanged()
        }

        return id
    }

    fun renamePlaylistFolder(
        id: String,
        name: String,
    ): Boolean {
        val updated =
            localStore.renamePlaylistFolder(
                id = id,
                name = name,
            )

        if (updated) {
            onPlaylistChanged()
        }

        return updated
    }

    fun movePlaylistFolder(
        id: String,
        parentId: String,
    ): Boolean {
        val updated =
            localStore.movePlaylistFolder(
                id = id,
                parentId = parentId,
            )

        if (updated) {
            onPlaylistChanged()
        }

        return updated
    }

    fun deletePlaylistFolder(
        id: String,
    ): Boolean {
        val updated =
            localStore.deletePlaylistFolder(id)

        if (updated) {
            onPlaylistChanged()
        }

        return updated
    }

    fun movePlaylistToFolder(
        playlistId: String,
        folderId: String,
    ): Boolean {
        val key =
            playlistStorageKey(
                playlistId
            ) ?: return false

        val updated =
            localStore.setPlaylistFolder(
                playlistKey = key,
                folderId = folderId,
            )

        if (updated) {
            onPlaylistChanged()
        }

        return updated
    }

    private fun hiddenStorageIdentity(
        kind: String,
        id: String,
    ): Pair<String, String>? {
        val activeBackend =
            backend ?: return null

        if (
            kind != "song" &&
            kind != "album"
        ) {
            return null
        }

        val persistent =
            activeBackend.customTagKey(id)

        val separator =
            persistent.indexOf(CUSTOM_TAG_KEY_SEP)

        if (separator <= 0) {
            return null
        }

        val scope =
            persistent.substring(
                0,
                separator,
            )

        val key =
            "$kind$HIDDEN_ITEM_KEY_SEP$persistent"

        return key to scope
    }

    fun isLocalLibrarySong(
        songId: String,
    ): Boolean {
        val activeBackend =
            backend ?: return false

        val persistent =
            activeBackend.customTagKey(songId)

        return persistent.startsWith(
            "local$CUSTOM_TAG_KEY_SEP"
        )
    }

    fun hiddenLibraryItems(): List<HiddenLibraryItem> {
        val activeBackend =
            backend ?: return emptyList()

        return localStore.hiddenItems(
            activeBackend.customTagScopes
        )
    }

    fun isHiddenItem(
        kind: String,
        id: String,
    ): Boolean {
        val identity =
            hiddenStorageIdentity(
                kind,
                id,
            ) ?: return false

        return localStore.isHidden(
            identity.first
        )
    }

    fun setHiddenItem(
        kind: String,
        id: String,
        title: String,
        subtitle: String,
        artworkUrl: String,
        hidden: Boolean,
    ): Boolean {
        val identity =
            hiddenStorageIdentity(
                kind,
                id,
            ) ?: return false

        val (key, scope) = identity

        return localStore.setHidden(
            HiddenLibraryItem(
                key = key,
                scope = scope,
                kind = kind,
                title = title,
                subtitle = subtitle,
                artworkUrl = artworkUrl,
            ),
            hidden,
        )
    }

    fun restoreHiddenItem(
        key: String,
    ): Boolean =
        localStore.removeHidden(key)

    private fun visibleAlbums(
        albums: List<Album>,
    ): List<Album> =
        albums.filterNot { album ->
            isHiddenItem(
                kind = "album",
                id = album.id,
            )
        }

    private fun visibleSongs(
        songs: List<Song>,
    ): List<Song> =
        songs.filterNot { song ->
            isHiddenItem(
                kind = "song",
                id = song.id,
            ) ||
                (
                    song.albumId.isNotBlank() &&
                        isHiddenItem(
                            kind = "album",
                            id = song.albumId,
                        )
                )
        }

    private fun visibleSearch(
        results: SearchResults,
    ): SearchResults =
        results.copy(
            songs = visibleSongs(results.songs),
            albums = visibleAlbums(results.albums),
        )

    private fun visibleHome(
        data: HomeData,
    ): HomeData =
        data.copy(
            newReleases =
                visibleAlbums(data.newReleases),
            recentlyPlayed =
                visibleAlbums(data.recentlyPlayed),
            mostPlayed =
                visibleAlbums(data.mostPlayed),
            random =
                visibleAlbums(data.random),
            starred =
                visibleSongs(data.starred),
        )

    private fun visibleDetail(
        kind: String,
        data: DetailData,
    ): DetailData {
        if (kind == "playlist") {
            return data
        }

        return data.copy(
            tracks = visibleSongs(data.tracks),
            albums = visibleAlbums(data.albums),
        )
    }

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
                DownloadRow(
                    aid,
                    "album",
                    f.album.ifBlank { "Album" },
                    f.albumArtist
                        ?.takeIf { it.isNotBlank() }
                        ?: f.artist,
                    fileUri(f.coverPath),
                    accentFor(aid),
                )
            }
        return (colRows + inferred).sortedBy { it.title }
    }

    private fun downloadedAlbums(): List<Album> = visibleDownloads()
        .filter { it.albumId.isNotBlank() }
        .groupBy { it.albumId }
        .map { (albumId, songs) ->
            val first = songs.first()
            Album(
                id = albumId,
                title = first.album.ifBlank { "Album" },
                artist =
                    first.albumArtist
                        ?.takeIf { it.isNotBlank() }
                        ?: first.artist,
                artworkUrl = first.toSong().artworkUrl,
                year = 0,
                songCount = songs.size,
            )
        }
        .sortedBy { it.title }

    suspend fun home(): HomeData {
        if (offline) {
            val albums = downloadedAlbums()

            return visibleHome(
                HomeData(
                    newReleases = albums,
                    recentlyPlayed = albums,
                    starred = downloadedSongs(),
                )
            )
        }

        return visibleHome(
            backend?.home() ?: HomeData()
        )
    }

    suspend fun allAlbums(): List<Album> =
        visibleAlbums(
            if (offline) {
                downloadedAlbums()
            } else {
                backend?.allAlbums().orEmpty()
            }
        )

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

    fun customTagsFor(
        songId: String,
    ): List<String> {
        val activeBackend =
            backend ?: return emptyList()

        return localStore.customTags(
            activeBackend.customTagKey(songId)
        )
    }

    fun allCustomTags(): List<CustomTag> {
        val activeBackend =
            backend ?: return emptyList()

        val counts =
            linkedMapOf<String, Pair<String, Int>>()

        localStore
            .customTagAssignments(
                activeBackend.customTagScopes
            )
            .values
            .forEach { tags ->
                tags.forEach { tag ->
                    val key =
                        tag.lowercase(Locale.ROOT)

                    val previous = counts[key]

                    counts[key] =
                        if (previous == null) {
                            tag to 1
                        } else {
                            previous.first to
                                (previous.second + 1)
                        }
                }
            }

        return counts.values
            .map { (name, count) ->
                CustomTag(
                    name = name,
                    songCount = count,
                )
            }
            .sortedBy {
                it.name.lowercase(Locale.ROOT)
            }
    }

    suspend fun setCustomTags(
        songId: String,
        tags: Collection<String>,
    ): Boolean {
        val activeBackend =
            backend ?: return false

        return localStore.setCustomTags(
            activeBackend.customTagKey(songId),
            tags,
        )
    }

    suspend fun songsByCustomTag(
        tag: String,
    ): List<Song> {
        val activeBackend =
            backend ?: return emptyList()

        val target =
            tag.trim().lowercase(Locale.ROOT)

        if (target.isBlank()) {
            return emptyList()
        }

        val assignments =
            localStore.customTagAssignments(
                activeBackend.customTagScopes
            )

        val songs = linkedMapOf<String, Song>()

        for ((key, tags) in assignments) {
            val matches =
                tags.any {
                    it.lowercase(Locale.ROOT) ==
                        target
                }

            if (!matches) {
                continue
            }

            val song =
                activeBackend
                    .songForCustomTagKey(key)
                    ?: continue

            songs[song.id] = song
        }

        return songs.values.toList()
    }

    suspend fun allPlaylists(): List<Playlist> {
        if (offline) {
            return emptyList()
        }

        val activeBackend =
            backend ?: return emptyList()

        return activeBackend
            .allPlaylists()
            .map { playlist ->
                val key =
                    playlistStorageKey(
                        playlist.id
                    )

                val folderId =
                    if (key == null) {
                        ""
                    } else {
                        localStore.playlistFolderId(
                            key
                        )
                    }

                playlist.copy(
                    folderId = folderId
                )
            }
    }

    suspend fun allSongs(): List<Song> =
        if (offline) downloadedSongs() else backend?.allSongs().orEmpty()

    suspend fun smartPlaylistTracks(
        playlist: SmartPlaylist,
    ): List<Song> {
        val engine =
            smartEngine ?: return emptyList()

        return visibleSongs(
            engine.evaluate(
                playlist,
                librarySongs(),
            )
        )
    }

    suspend fun smartPlaylistCovers(
        playlists: List<SmartPlaylist>,
    ): Map<String, String> {
        if (playlists.isEmpty()) {
            return emptyMap()
        }

        val engine = smartEngine
        val source =
            if (engine == null) {
                emptyList()
            } else {
                librarySongs()
            }

        return playlists
            .mapNotNull { playlist ->
                val id =
                    playlist.id
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: return@mapNotNull null

                val tracks =
                    if (engine == null) {
                        emptyList()
                    } else {
                        visibleSongs(
                            engine.evaluate(
                                playlist,
                                source,
                            )
                        )
                    }

                id to
                    resolveSmartPlaylistCover(
                        playlist,
                        tracks,
                    )
            }
            .toMap()
    }

    suspend fun duplicateCandidates(): List<Song> {
        val songs =
            if (offline) {
                downloadedSongs()
            } else {
                backend
                    ?.duplicateCandidates()
                    .orEmpty()
            }

        /*
         * Hidden is a Sonethyst library visibility decision.
         * A track intentionally hidden by the user should not reappear
         * just because the duplicate scanner bypasses merged dedup.
         */
        return visibleSongs(songs)
            .distinctBy { it.id }
    }

    suspend fun librarySongs(
        limit: Int = 2000,
    ): List<Song> {
        val safeLimit =
            limit.coerceAtLeast(0)

        if (safeLimit == 0) {
            return emptyList()
        }

        if (offline) {
            return visibleSongs(
                downloadedSongs()
            ).take(safeLimit)
        }

        val activeBackend =
            backend ?: return emptyList()

        /*
         * Filtering after fetching exactly N rows would make pagination
         * falsely look exhausted whenever hidden items are inside that N.
         * Expand the backend window until we have enough visible rows or
         * the backend itself is exhausted.
         */
        var requestLimit = safeLimit
        var attempts = 0

        var raw =
            activeBackend.librarySongs(
                requestLimit
            )

        var visible =
            visibleSongs(raw)

        while (
            visible.size < safeLimit &&
            raw.size >= requestLimit &&
            attempts < 8
        ) {
            requestLimit += 500
            attempts++

            raw =
                activeBackend.librarySongs(
                    requestLimit
                )

            visible =
                visibleSongs(raw)
        }

        return visible.take(safeLimit)
    }

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

    suspend fun search(
        query: String,
    ): SearchResults {
        if (offline) {
            val q = query.trim()

            val songs =
                downloadedSongs().filter {
                    it.title.contains(q, true) ||
                        it.artist.contains(q, true) ||
                        it.album.contains(q, true)
                }

            val albums =
                downloadedAlbums().filter {
                    it.title.contains(q, true) ||
                        it.artist.contains(q, true)
                }

            return visibleSearch(
                SearchResults(
                    songs = songs,
                    albums = albums,
                    artists = emptyList(),
                )
            )
        }

        return visibleSearch(
            backend?.search(query)
                ?: SearchResults()
        )
    }

    suspend fun scrobble(id: String) {
        if (offline) return
        backend?.scrobble(id)
    }

    suspend fun radio(seedId: String): List<Song> {
        if (offline) return emptyList()
        return backend?.radio(seedId).orEmpty()
    }

    suspend fun createPlaylist(
        name: String,
        folderId: String = "",
    ): Boolean {
        val activeBackend =
            backend ?: return false

        val safeName =
            name.trim()

        if (safeName.isBlank()) {
            return false
        }

        if (
            folderId.isNotBlank() &&
            localStore.playlistFolders()
                .none {
                    it.id == folderId
                }
        ) {
            return false
        }

        /*
         * Prefer the backend operation that gives us the new id immediately.
         * MergedBackend returns the correctly wrapped id here.
         */
        var playlistId =
            activeBackend.createPlaylistWithId(
                safeName
            )

        /*
         * Compatibility fallback for a backend that cannot return the
         * created id directly.
         */
        if (playlistId == null) {
            val before =
                activeBackend.allPlaylists()
                    .mapTo(
                        mutableSetOf()
                    ) {
                        it.id
                    }

            if (
                !activeBackend.createPlaylist(
                    safeName
                )
            ) {
                return false
            }

            playlistId =
                activeBackend.allPlaylists()
                    .firstOrNull {
                        it.id !in before
                    }
                    ?.id
        }

        if (playlistId == null) {
            onPlaylistChanged()
            return folderId.isBlank()
        }

        if (folderId.isNotBlank()) {
            val key =
                playlistStorageKey(
                    playlistId
                ) ?: return false

            if (
                !localStore.setPlaylistFolder(
                    playlistKey = key,
                    folderId = folderId,
                )
            ) {
                return false
            }
        }

        onPlaylistChanged()
        return true
    }

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
        if (kind == "tag") {
            val tracks =
                songsByCustomTag(id)

            return DetailData(
                info =
                    DetailInfo(
                        title = id,
                        subtitle =
                            "${tracks.size} song${if (tracks.size == 1) "" else "s"}",
                        artUrl =
                            tracks.firstOrNull()
                                ?.artworkUrl
                                .orEmpty(),
                        accent = accentFor("tag:$id"),
                        isArtist = false,
                        songCount = tracks.size,
                        typeLabel = "Tag",
                    ),
                tracks = visibleSongs(tracks),
            )
        }

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
                tracks = visibleSongs(tracks),
            )
        }

        if (kind == "smart") {
            val sp =
                smartPlaylistsProvider()
                    .firstOrNull {
                        it.id == id
                    }
                    ?: return null

            val tracks =
                smartPlaylistTracks(sp)

            return DetailData(
                DetailInfo(
                    title =
                        sp.name
                            ?: "Smart playlist",
                    subtitle =
                        "Smart playlist • ${tracks.size} songs",
                    artUrl =
                        resolveSmartPlaylistCover(
                            sp,
                            tracks,
                        ),
                    accent = accentFor(id),
                    isArtist = false,
                    songCount = tracks.size,
                    typeLabel =
                        "Smart playlist",
                    playlistCoverMode =
                        sp.coverMode
                            .orEmpty()
                            .ifBlank {
                                "automatic"
                            },
                ),
                tracks,
            )
        }
        if (offline) {
            val dls = downloadedSongs()
            return when (kind) {
                "album" -> visibleSongs(
                    dls.filter { it.albumId == id }
                ).takeIf { it.isNotEmpty() }?.let { tracks ->
                    val f = tracks.first()
                    DetailData(DetailInfo(f.album.ifBlank { "Album" }, "${f.artist} • Downloaded", f.artworkUrl, accentFor(id), false, tracks.size, "Album"), tracks)
                }
                "artist" -> visibleSongs(
                    dls.filter { it.artistId == id }
                ).takeIf { it.isNotEmpty() }?.let { tracks ->
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
        return backend
            ?.detail(kind, id)
            ?.let { visibleDetail(kind, it) }
    }

    suspend fun detailPage(
        kind: String,
        id: String,
        offset: Int,
    ): List<Song> {
        if (offline) return emptyList()

        val tracks =
            if (kind == "genre") {
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

        return if (kind == "playlist") {
            tracks
        } else {
            visibleSongs(tracks)
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
