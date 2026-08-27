package com.mentality.sonethyst.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.TagTextField
import java.io.File

data class LocalTagIdentity(
    val albumArtist: String = "",
    val artists: List<String> = emptyList(),
)

private data class CachedTagIdentity(
    val modified: Long = 0L,
    val size: Long = 0L,

    // Nullable so old/corrupt Gson entries cannot crash callers.
    val albumArtist: String? = "",
    val artists: List<String>? = emptyList(),
)

class LocalTagMetadataCache(
    context: Context,
) {
    private val gson = Gson()

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
