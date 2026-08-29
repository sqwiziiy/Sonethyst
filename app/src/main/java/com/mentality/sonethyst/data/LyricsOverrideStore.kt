package com.mentality.sonethyst.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mentality.sonethyst.model.LyricLine
import com.mentality.sonethyst.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

data class LyricsOverrideDraft(
    val rawText: String = "",
    val synced: Boolean = false,
    val offsetMs: Int = 0,
)

private data class SavedLyricsOverride(
    val rawText: String = "",
    val synced: Boolean = false,
    val offsetMs: Int = 0,
)

class LyricsOverrideStore(
    context: Context,
    private val scopeKeyProvider: () -> String,
) {
    private val gson = Gson()
    private val lock = Any()

    private val file =
        File(
            context.filesDir,
            "lyrics_overrides.json",
        )

    private val type =
        object :
            TypeToken<
                MutableMap<String, SavedLyricsOverride>
            >() {}.type

    private val entries:
        MutableMap<String, SavedLyricsOverride> =
        runCatching {
            if (!file.exists()) {
                mutableMapOf()
            } else {
                gson.fromJson<
                    MutableMap<
                        String,
                        SavedLyricsOverride,
                    >
                >(
                    file.readText(),
                    type,
                ) ?: mutableMapOf()
            }
        }.getOrElse {
            mutableMapOf()
        }

    fun getDraft(
        song: Song,
    ): LyricsOverrideDraft? =
        synchronized(lock) {
            entries[key(song)]
                ?.let {
                    LyricsOverrideDraft(
                        rawText = it.rawText,
                        synced = it.synced,
                        offsetMs = it.offsetMs,
                    )
                }
        }

    fun lyricsFor(
        song: Song,
    ): Lyrics? {
        val draft =
            getDraft(song)
                ?: return null

        val lines =
            if (draft.synced) {
                LyricsRepository
                    .parseLrc(
                        draft.rawText
                    )
            } else {
                draft.rawText
                    .lines()
                    .map {
                        LyricLine(
                            -1,
                            it,
                        )
                    }
            }

        if (lines.isEmpty()) {
            return null
        }

        val effectiveLines =
            if (
                draft.synced &&
                draft.offsetMs != 0
            ) {
                lines.map { line ->
                    if (line.timeMs < 0L) {
                        line
                    } else {
                        val shifted =
                            (
                                line.timeMs +
                                    draft.offsetMs
                            ).coerceAtLeast(0L)

                        line.copy(
                            timeSec =
                                (
                                    shifted /
                                        1000L
                                ).toInt(),
                            timeMs = shifted,
                        )
                    }
                }
            } else {
                lines
            }

        return Lyrics(
            lines = effectiveLines,
            synced =
                draft.synced &&
                    lines.any {
                        it.timeSec >= 0
                    },
            source = "Custom",
        )
    }

    suspend fun save(
        song: Song,
        rawText: String,
        synced: Boolean,
        offsetMs: Int = 0,
    ): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                synchronized(lock) {
                    entries[key(song)] =
                        SavedLyricsOverride(
                            rawText = rawText,
                            synced = synced,
                            offsetMs = offsetMs,
                        )

                    persistLocked()
                }

                true
            }.getOrDefault(false)
        }

    suspend fun remove(
        song: Song,
    ): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                synchronized(lock) {
                    val removed =
                        entries.remove(
                            key(song)
                        ) != null

                    if (removed) {
                        persistLocked()
                    }

                    removed
                }
            }.getOrDefault(false)
        }

    private fun key(
        song: Song,
    ): String {
        val scope =
            scopeKeyProvider()
                .ifBlank {
                    "default"
                }

        val identity =
            if (song.path.isNotBlank()) {
                "path:${song.path}"
            } else {
                "id:${song.id}"
            }

        return "$scope|$identity"
    }

    private fun persistLocked() {
        file.parentFile?.mkdirs()

        val tmp =
            File(
                file.parentFile,
                "${file.name}.tmp",
            )

        tmp.writeText(
            gson.toJson(
                entries,
                type,
            )
        )

        if (!tmp.renameTo(file)) {
            file.writeText(
                tmp.readText()
            )
            tmp.delete()
        }
    }
}
