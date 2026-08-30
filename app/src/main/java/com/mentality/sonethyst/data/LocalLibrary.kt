package com.mentality.sonethyst.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.mentality.sonethyst.model.Album
import com.mentality.sonethyst.model.Artist
import com.mentality.sonethyst.model.Genre
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.util.TrackMatch
import com.mentality.sonethyst.util.accentFor
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LocalLibrary(
    private val context: Context,
    // scanned replaygain overlaid by path since mediastore tags rarely carry it
    private val gainProvider: (String) -> Pair<Float, Float>? = { null },
) {

    @Volatile private var loaded = false
    private val mutex = Mutex()

    private val metadataEnrichMutex =
        Mutex()

    private val tagMetadataCache =
        LocalTagMetadataCache(context)

    @Volatile var songs: List<Song> = emptyList(); private set
    @Volatile var albums: List<Album> = emptyList(); private set
    @Volatile var artists: List<Artist> = emptyList(); private set
    @Volatile var genres: List<Genre> = emptyList(); private set
    private var byId: Map<String, Song> = emptyMap()

    @Volatile private var matchIndex: Map<String, List<Song>> = emptyMap()

    @Volatile private var dirOf: Map<String, String> = emptyMap()

    @Volatile
    private var excludedFolders: Set<String> = emptySet()

    @Volatile
    private var includedFolders: Set<String> = emptySet()

    @Volatile
    private var includeOnlyFolders: Boolean = false

    @Volatile
    private var detectedFolders: List<String> = emptyList()

    @Volatile var folderRoot: String = ""; private set

    suspend fun ensureLoaded() {
        if (loaded) return
        mutex.withLock {
            if (!loaded) { scan(); loaded = true }
        }
    }

    suspend fun refresh() {
        mutex.withLock { scan(); loaded = true }
    }

    /*
     * Populate real file-tag identity in the background.
     *
     * scan() itself never parses every audio file, preserving
     * fast MediaStore-backed library loading.
     */
    suspend fun enrichMissingTagMetadata():
        Boolean =
        metadataEnrichMutex.withLock {
            ensureLoaded()

            val paths =
                songs
                    .map { it.path }
                    .filter {
                        it.isNotBlank()
                    }
                    .distinct()

            tagMetadataCache
                .enrichMissing(paths)
        }

    /*
     * Used after editing one local file. Only that file is
     * reparsed, so metadata changes can appear immediately.
     */
    suspend fun refreshTagMetadata(
        path: String,
    ): Boolean =
        tagMetadataCache.refresh(path)

    fun invalidateTagMetadata(
        path: String,
    ) {
        tagMetadataCache.invalidate(path)
    }

    fun song(id: String): Song? = byId[id]

    fun detectedMusicFolders(): List<String> =
        detectedFolders

    fun setFolderPolicy(prefs: LocalFolderPrefs) {
        excludedFolders =
            prefs.excluded
                .map { normalizeFolder(it) }
                .filter { it.isNotBlank() }
                .toSet()

        includedFolders =
            prefs.included
                .map { normalizeFolder(it) }
                .filter { it.isNotBlank() }
                .toSet()

        includeOnlyFolders = prefs.includeOnly
    }

    private fun normalizeFolder(path: String): String =
        path.replace('\\', '/')
            .trim()
            .trimEnd('/')

    private fun shouldIncludeFolder(path: String): Boolean {
        val normalized = normalizeFolder(path)

        if (includeOnlyFolders) {
            if (normalized.isBlank()) return false

            return includedFolders.any { included ->
                normalized == included ||
                    normalized.startsWith("$included/")
            }
        }

        if (normalized.isBlank()) return true

        return excludedFolders.none { excluded ->
            normalized == excluded ||
                normalized.startsWith("$excluded/")
        }
    }

    // only substitute on a single unambiguous match so a different version is never swapped in
    fun findMatch(artist: String, title: String, durationSec: Int): Song? {
        if (title.isBlank()) return null
        val candidates = matchIndex[TrackMatch.key(artist, title)] ?: return null
        val byDuration = candidates.filter { durationSec > 0 && it.durationSec > 0 && abs(it.durationSec - durationSec) <= TrackMatch.DURATION_TOLERANCE_SEC }
        if (byDuration.isNotEmpty()) return byDuration.minByOrNull { abs(it.durationSec - durationSec) }
        return candidates.singleOrNull()
    }

    fun browse(path: String): Pair<List<String>, List<Song>> {
        val base = path.ifBlank { folderRoot }
        if (base.isBlank()) return emptyList<String>() to emptyList()
        val here = songs.filter { dirOf[it.id] == base }.sortedBy { it.title.lowercase() }
        val subdirs = dirOf.values.asSequence()
            .filter { it != base && it.startsWith("$base/") }
            .map { it.removePrefix("$base/").substringBefore('/') }
            .distinct().sortedBy { it.lowercase() }.toList()
        return subdirs to here
    }

    private fun commonDir(dirs: Collection<String>): String {
        if (dirs.isEmpty()) return ""
        var prefix = dirs.first().split('/')
        for (d in dirs) {
            val seg = d.split('/')
            var i = 0
            while (i < prefix.size && i < seg.size && prefix[i] == seg[i]) i++
            prefix = prefix.subList(0, i)
        }
        return prefix.joinToString("/")
    }
    fun songsIn(album: Album): List<Song> =
        songs.filter {
            it.albumId == album.id
        }

    fun songsByAlbumId(
        albumId: String,
    ): List<Song> =
        songs.filter {
            it.albumId == albumId
        }

    private fun artistNamesFor(
        song: Song,
    ): List<String> {
        val explicit =
            song.artists
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy {
                    it.lowercase()
                }

        if (explicit.isNotEmpty()) {
            return explicit
        }

        return song.artist
            .trim()
            .takeIf {
                it.isNotBlank() &&
                    it != "Unknown artist"
            }
            ?.let(::listOf)
            .orEmpty()
    }

    private fun localArtistId(
        name: String,
    ): String {
        val normalized =
            name
                .trim()
                .lowercase()

        val uuid =
            java.util.UUID
                .nameUUIDFromBytes(
                    normalized.toByteArray(
                        Charsets.UTF_8
                    )
                )

        return "local_artist_$uuid"
    }

    fun primaryArtistId(
        song: Song,
    ): String? =
        artistNamesFor(song)
            .firstOrNull()
            ?.let(::localArtistId)

    fun songsByArtistId(
        artistId: String,
    ): List<Song> {
        /*
         * New artist pages use stable logical IDs.
         */
        val logicalName =
            artists
                .firstOrNull {
                    it.id == artistId
                }
                ?.name

        if (logicalName != null) {
            return songs.filter { song ->
                artistNamesFor(song)
                    .any {
                        it.equals(
                            logicalName,
                            ignoreCase = true,
                        )
                    }
            }
        }

        /*
         * Compatibility for older references using the raw
         * MediaStore ARTIST_ID.
         */
        return songs.filter {
            it.artistId == artistId
        }
    }

    fun songsByGenre(genre: String): List<Song> =
        songs.filter { song ->
            song.genres.any {
                it.equals(genre, ignoreCase = true)
            }
        }
    fun albumsByArtistId(
        artistId: String,
    ): List<Album> =
        songsByArtistId(artistId)
            .map { it.albumId }
            .filter { it.isNotBlank() }
            .distinct()
            .mapNotNull { aid ->
                albums.firstOrNull {
                    it.id == aid
                }
            }

    private fun albumArtUri(albumId: Long): String =
        if (albumId <= 0) "" else ContentUris.withAppendedId(ALBUM_ART_BASE, albumId).toString()

    private fun suffixFrom(displayName: String?, mime: String?): String {
        displayName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() && it.length in 2..4 }?.let { return it.lowercase() }
        val m = mime?.lowercase() ?: return ""
        return when {
            m.contains("flac") -> "flac"
            m.contains("mpeg") || m.contains("mp3") -> "mp3"
            m.contains("aac") || m.contains("mp4") || m.contains("m4a") -> "m4a"
            m.contains("opus") -> "opus"
            m.contains("ogg") || m.contains("vorbis") -> "ogg"
            m.contains("wav") -> "wav"
            m.contains("aiff") || m.contains("aif") -> "aiff"
            else -> ""
        }
    }

    private fun splitGenres(raw: String?): List<String> =
        raw.orEmpty()
            .split(';')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }

    private fun legacyGenresForAudioId(id: Long): List<String> {
        if (Build.VERSION.SDK_INT >= 30) {
            return emptyList()
        }

        return runCatching {
            val uri =
                MediaStore.Audio.Genres.getContentUriForAudioId(
                    "external",
                    id.toInt(),
                )

            context.contentResolver.query(
                uri,
                arrayOf(MediaStore.Audio.Genres.NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                val nameCol =
                    cursor.getColumnIndex(
                        MediaStore.Audio.Genres.NAME
                    )

                if (nameCol < 0) {
                    emptyList()
                } else {
                    buildList {
                        while (cursor.moveToNext()) {
                            cursor.getString(nameCol)
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?.let(::add)
                        }
                    }.distinctBy { it.lowercase() }
                }
            }.orEmpty()
        }.getOrDefault(emptyList())
    }

    private suspend fun scan() = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val cols = arrayListOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ARTIST_ID,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.YEAR,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.MIME_TYPE,
            @Suppress("DEPRECATION") MediaStore.Audio.Media.DATA,
        )
        if (Build.VERSION.SDK_INT >= 30) {
            cols.add(MediaStore.Audio.Media.BITRATE)
            cols.add(MediaStore.Audio.Media.GENRE)
        }
        val projection = cols.toTypedArray()
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sort = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"
        val out = ArrayList<Song>()
        val albumDateAdded = HashMap<String, Long>()
        val albumYear = HashMap<String, Int>()
        val dirs = HashMap<String, String>()
        val allDetectedDirs = linkedSetOf<String>()

        runCatching {
            context.contentResolver.query(collection, projection, selection, null, sort)?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val artistIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val yearCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
                val addedCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
                val nameCol = c.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)
                val mimeCol = c.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE)
                val bitrateCol = c.getColumnIndex(MediaStore.Audio.Media.BITRATE)
                val genreCol = c.getColumnIndex(MediaStore.Audio.Media.GENRE)
                @Suppress("DEPRECATION") val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)
                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val albumId = c.getLong(albumIdCol)
                    val artistId = c.getLong(artistIdCol)
                    val title = c.getString(titleCol) ?: continue
                    val artistName = c.getString(artistCol)?.takeIf { it.isNotBlank() && it != "<unknown>" } ?: "Unknown artist"
                    val albumName = c.getString(albumCol)?.takeIf { it.isNotBlank() } ?: "Unknown album"
                    val durSec = (c.getLong(durCol) / 1000L).toInt()
                    val year = runCatching { c.getInt(yearCol) }.getOrDefault(0)
                    val added = runCatching { c.getLong(addedCol) }.getOrDefault(0L)
                    val display = if (nameCol >= 0) c.getString(nameCol) else null
                    val mime = if (mimeCol >= 0) c.getString(mimeCol) else null
                    val suffix = suffixFrom(display, mime)
                    val bitrateKbps = if (bitrateCol >= 0) (runCatching { c.getInt(bitrateCol) }.getOrDefault(0) / 1000) else 0
                    val genres =
                        if (genreCol >= 0) {
                            splitGenres(c.getString(genreCol))
                        } else {
                            legacyGenresForAudioId(id)
                        }
                    val art = albumArtUri(albumId)
                    val uri = ContentUris.withAppendedId(collection, id).toString()
                    val data =
                        if (dataCol >= 0) {
                            c.getString(dataCol).orEmpty()
                        } else {
                            ""
                        }

                    /*
                     * Fast path: disk cache only.
                     *
                     * A cache miss deliberately falls back to
                     * MediaStore instead of opening the audio
                     * file inside the main library scan.
                     */
                    val tagIdentity =
                        if (data.isNotBlank()) {
                            tagMetadataCache
                                .get(data)
                        } else {
                            null
                        }

                    val effectiveArtists =
                        tagIdentity
                            ?.artists
                            .orEmpty()
                            .ifEmpty {
                                if (
                                    artistName ==
                                        "Unknown artist"
                                ) {
                                    emptyList()
                                } else {
                                    listOf(
                                        artistName
                                    )
                                }
                            }

                    val displayArtist =
                        effectiveArtists
                            .joinToString(", ")
                            .ifBlank {
                                artistName
                            }

                    val directory =
                        if (data.contains('/')) {
                            normalizeFolder(
                                data.substringBeforeLast('/')
                            )
                        } else {
                            ""
                        }

                    if (directory.isNotBlank()) {
                        allDetectedDirs += directory
                    }

                    // Folder policy affects Sonethyst's in-memory library
                    // only. MediaStore and the actual file are never modified.
                    if (!shouldIncludeFolder(directory)) {
                        continue
                    }

                    if (directory.isNotBlank()) {
                        dirs[id.toString()] = directory
                    }

                    val rg =
                        if (data.isNotBlank()) {
                            gainProvider(data)
                        } else {
                            null
                        }
                    val sidAlbum = albumId.toString()
                    if (added > (albumDateAdded[sidAlbum] ?: 0L)) albumDateAdded[sidAlbum] = added
                    if (year > 0 && albumYear[sidAlbum] == null) albumYear[sidAlbum] = year
                    out += Song(
                        id = id.toString(),
                        title = title,
                        artist = displayArtist,
                        album = albumName,
                        artworkUrl =
                            tagIdentity
                                ?.artworkUrl
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                                ?: art,
                        durationSec = durSec,
                        accent = accentFor(id.toString()),
                        streamUrl = uri,
                        albumId = sidAlbum,
                        artistId = artistId.toString(),
                        suffix = suffix,
                        bitrateKbps = bitrateKbps,
                        path = data,
                        replayGainTrack = rg?.first ?: 0f,
                        replayGainAlbum = rg?.second ?: 0f,
                        genres = genres,
                        albumArtist =
                            tagIdentity
                                ?.albumArtist
                                .orEmpty(),
                        artists =
                            effectiveArtists,
                    )
                }
            }
        }
        detectedFolders =
            allDetectedDirs
                .sortedWith(
                    compareBy<String>(
                        { it.count { ch -> ch == '/' } },
                        { it.lowercase() },
                    )
                )

        songs = out

        val genreBuckets =
            linkedMapOf<
                String,
                Pair<String, MutableSet<String>>,
            >()

        out.forEach { song ->
            song.genres.forEach { rawGenre ->
                val name = rawGenre.trim()
                if (name.isBlank()) return@forEach

                val key = name.lowercase()
                val bucket =
                    genreBuckets.getOrPut(key) {
                        name to linkedSetOf()
                    }

                bucket.second += song.id
            }
        }

        genres =
            genreBuckets.values
                .map { (name, songIds) ->
                    Genre(
                        id = name,
                        name = name,
                        songCount = songIds.size,
                    )
                }
                .sortedBy { it.name.lowercase() }

        byId = out.associateBy { it.id }
        matchIndex = out.groupBy { TrackMatch.key(it.artist, it.title) }
        dirOf = dirs
        folderRoot = commonDir(dirs.values)
        albums = out.groupBy { it.albumId }
            .map { (aid, tracks) ->
                val f = tracks.first()

                val albumArtists =
                    tracks
                        .map { it.albumArtist.trim() }
                        .filter { it.isNotBlank() }
                        .distinct()

                val trackArtists =
                    tracks
                        .map { it.artist }
                        .filter { it.isNotBlank() }
                        .distinct()

                val displayArtist =
                    when {
                        albumArtists.size == 1 ->
                            albumArtists.first()

                        trackArtists.size == 1 ->
                            trackArtists.first()

                        else ->
                            "Various artists"
                    }

                Album(
                    id = aid,
                    title = f.album,
                    artist = displayArtist,
                    artworkUrl = tracks.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl ?: "",
                    year = albumYear[aid] ?: 0,
                    songCount = tracks.size,
                )
            }
            .sortedByDescending { albumDateAdded[it.id] ?: 0L }
        val artistBuckets =
            linkedMapOf<
                String,
                Pair<
                    String,
                    MutableList<Song>,
                >,
            >()

        out.forEach { song ->
            artistNamesFor(song)
                .forEach { rawName ->
                    val name =
                        rawName.trim()

                    val key =
                        name.lowercase()

                    val bucket =
                        artistBuckets
                            .getOrPut(key) {
                                name to
                                    mutableListOf()
                            }

                    bucket.second +=
                        song
                }
        }

        artists =
            artistBuckets
                .values
                .map { (name, tracks) ->
                    Artist(
                        id =
                            localArtistId(
                                name
                            ),
                        name = name,
                        imageUrl =
                            tracks
                                .firstOrNull {
                                    it.artworkUrl
                                        .isNotBlank()
                                }
                                ?.artworkUrl
                                .orEmpty(),
                        monthlyListeners = 0,
                    )
                }
                .sortedBy {
                    it.name.lowercase()
                }
    }

    private companion object {
        val ALBUM_ART_BASE: Uri = Uri.parse("content://media/external/audio/albumart")
    }
}
