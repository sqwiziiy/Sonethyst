package com.mentality.sonethyst.data

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagTextField
import java.io.File
import java.security.MessageDigest

data class LocalTagIdentity(
    val albumArtist: String = "",
    val artists: List<String> = emptyList(),
    val artworkUrl: String = "",
)

private data class CachedTagIdentity(
    val modified: Long = 0L,
    val size: Long = 0L,

    // Nullable so old/corrupt Gson entries cannot crash callers.
    val albumArtist: String? = "",
    val artists: List<String>? = emptyList(),
    val artworkUrl: String? = "",
)

class LocalTagMetadataCache(
    private val context: Context,
) {
    private val gson = Gson()

    /*
     * Sonethyst-owned embedded artwork cache.
     *
     * MediaStore album art is album-scoped and can stay stale after
     * editing one file. Embedded artwork is therefore extracted from
     * the actual audio file and exposed through a versioned file:// URI.
     */
    private val artworkDir =
        File(
            context.filesDir,
            "local_tag_artwork",
        ).apply {
            mkdirs()
        }

    private val file =
        File(
            context.filesDir,
            "local_tag_identity.json",
        )

    private val type =
        object :
            TypeToken<
                MutableMap<
                    String,
                    CachedTagIdentity,
                >
            >() {}.type

    private val lock = Any()

    private val cache:
        MutableMap<String, CachedTagIdentity> =
        runCatching {
            if (!file.exists()) {
                mutableMapOf()
            } else {
                gson.fromJson<
                    MutableMap<
                        String,
                        CachedTagIdentity,
                    >
                >(
                    file.readText(),
                    type,
                ) ?: mutableMapOf()
            }
        }.getOrElse {
            mutableMapOf()
        }

    fun get(
        path: String,
    ): LocalTagIdentity? {
        if (path.isBlank()) {
            return null
        }

        val source = File(path)

        if (!source.exists()) {
            return null
        }

        val entry =
            synchronized(lock) {
                cache[path]
            } ?: return null

        if (
            entry.modified !=
                source.lastModified() ||
            entry.size !=
                source.length()
        ) {
            return null
        }

        return LocalTagIdentity(
            albumArtist =
                entry.albumArtist
                    .orEmpty()
                    .trim(),
            artists =
                normalizeArtists(
                    entry.artists.orEmpty()
                ),
            artworkUrl =
                entry.artworkUrl
                    .orEmpty(),
        )
    }

    suspend fun enrichMissing(
        paths: Collection<String>,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val missing =
                paths
                    .asSequence()
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
                    .filter { get(it) == null }
                    .toList()

            if (missing.isEmpty()) {
                return@withContext false
            }

            val updates =
                linkedMapOf<
                    String,
                    CachedTagIdentity,
                >()

            missing.forEach { path ->
                readEntry(path)?.let {
                    updates[path] = it
                }
            }

            if (updates.isEmpty()) {
                return@withContext false
            }

            synchronized(lock) {
                cache.putAll(updates)
                saveLocked()
            }

            true
        }

    suspend fun refresh(
        path: String,
    ): Boolean =
        withContext(Dispatchers.IO) {
            val normalized =
                path.trim()

            if (normalized.isBlank()) {
                return@withContext false
            }

            val entry =
                readEntry(normalized)
                    ?: run {
                        synchronized(lock) {
                            val removed =
                                cache.remove(
                                    normalized
                                ) != null

                            if (removed) {
                                saveLocked()
                            }

                            return@withContext removed
                        }
                    }

            synchronized(lock) {
                cache[normalized] =
                    entry

                saveLocked()
            }

            true
        }

    fun invalidate(
        path: String,
    ) {
        if (path.isBlank()) {
            return
        }

        synchronized(lock) {
            if (
                cache.remove(path) !=
                null
            ) {
                saveLocked()
            }
        }
    }

    private fun readEntry(
        path: String,
    ): CachedTagIdentity? {
        val source =
            File(path)

        if (!source.exists()) {
            return null
        }

        val tag =
            runCatching {
                AudioFileIO
                    .read(source)
                    .tag
            }.getOrNull()

        val albumArtist =
            tag
                ?.firstOrEmpty(
                    FieldKey.ALBUM_ARTIST
                )
                .orEmpty()
                .trim()

        val artists =
            tag
                ?.artistValues()
                .orEmpty()

        val embeddedArtwork =
            runCatching {
                tag
                    ?.firstArtwork
                    ?.binaryData
            }.getOrNull()

        val artworkUrl =
            cacheArtwork(
                path = path,
                source = source,
                bytes = embeddedArtwork,
            )

        /*
         * Cache even an unreadable/empty tag.
         *
         * Its mtime + size still protect us from keeping the
         * empty result after the file changes, while avoiding
         * repeated expensive parse attempts on every startup.
         */
        return CachedTagIdentity(
            modified =
                source.lastModified(),
            size =
                source.length(),
            albumArtist =
                albumArtist,
            artists =
                artists,
            artworkUrl =
                artworkUrl,
        )
    }

    private fun cacheArtwork(
        path: String,
        source: File,
        bytes: ByteArray?,
    ): String {
        /*
         * Prefix identifies the logical source file.
         * Version changes whenever the edited audio file changes,
         * intentionally giving Coil a new URI after artwork replacement.
         */
        val prefix =
            sha256(path)
                .take(24)

        val version =
            "${source.lastModified()}_${source.length()}"

        val target =
            File(
                artworkDir,
                "${prefix}_${version}.img",
            )

        artworkDir
            .listFiles()
            ?.asSequence()
            ?.filter {
                it.name.startsWith(
                    "${prefix}_"
                ) &&
                    it != target
            }
            ?.forEach {
                runCatching {
                    it.delete()
                }
            }

        if (
            bytes == null ||
            bytes.isEmpty()
        ) {
            runCatching {
                target.delete()
            }

            return ""
        }

        if (
            !target.exists() ||
            target.length() !=
                bytes.size.toLong()
        ) {
            val temporary =
                File(
                    artworkDir,
                    "${target.name}.tmp",
                )

            runCatching {
                temporary.writeBytes(
                    bytes
                )

                if (
                    !temporary.renameTo(
                        target
                    )
                ) {
                    target.writeBytes(
                        bytes
                    )

                    temporary.delete()
                }
            }.getOrElse {
                runCatching {
                    temporary.delete()
                }

                return ""
            }
        }

        return Uri
            .fromFile(target)
            .toString()
    }

    private fun sha256(
        value: String,
    ): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(
                value.toByteArray(
                    Charsets.UTF_8
                )
            )
            .joinToString("") {
                "%02x".format(
                    it.toInt() and 0xff
                )
            }

    private fun Tag.artistValues():
        List<String> {
        val all =
            runCatching {
                getFields(
                    FieldKey.ARTIST
                )
                    .mapNotNull { field ->
                        val raw =
                            when (field) {
                                is TagTextField ->
                                    field.content

                                else ->
                                    field.toString()
                            }

                        raw
                            .trim()
                            .takeIf {
                                it.isNotBlank()
                            }
                    }
                    .flatMap {
                        splitArtistField(it)
                    }
            }.getOrDefault(
                emptyList()
            )

        if (all.isNotEmpty()) {
            return normalizeArtists(all)
        }

        val first =
            firstOrEmpty(
                FieldKey.ARTIST
            )

        return normalizeArtists(
            splitArtistField(first)
        )
    }

    private fun splitArtistField(
        raw: String,
    ): List<String> =
        raw
            .split(
                ';',
                '\u0000',
            )
            .map { it.trim() }
            .filter { it.isNotBlank() }

    private fun normalizeArtists(
        artists: List<String>,
    ): List<String> =
        artists
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinctBy {
                it.lowercase()
            }

    private fun Tag.firstOrEmpty(
        key: FieldKey,
    ): String =
        runCatching {
            getFirst(key)
                ?: ""
        }.getOrDefault("")

    private fun saveLocked() {
        runCatching {
            file.parentFile
                ?.mkdirs()

            val json =
                gson.toJson(
                    cache,
                    type,
                )

            val tmp =
                File(
                    file.parentFile,
                    "${file.name}.tmp",
                )

            tmp.writeText(json)

            if (!tmp.renameTo(file)) {
                file.writeText(json)
                tmp.delete()
            }
        }
    }
}
