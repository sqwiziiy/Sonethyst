package com.mentality.sonethyst.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.LyricsRepository
import com.mentality.sonethyst.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LyricsTimingLine(
    val rawLineIndex: Int,
    val timeMs: Long,
    val text: String,
)

data class LyricsEditState(
    val loading: Boolean = true,
    val songId: String = "",
    val title: String = "",
    val artist: String = "",
    val rawText: String = "",
    val synced: Boolean = false,
    val offsetMs: Int = 0,
    val timingLines: List<LyricsTimingLine> =
        emptyList(),
    val source: String = "",
    val hasCustom: Boolean = false,
    val saving: Boolean = false,
    val error: String = "",
)

class LyricsEditViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val container =
        (app as SonethystApplication)
            .container

    private val _state =
        MutableStateFlow(
            LyricsEditState()
        )

    val state:
        StateFlow<LyricsEditState> =
        _state.asStateFlow()

    private var song:
        Song? = null

    private var loadedId:
        String? = null

    fun load(
        songId: String,
    ) {
        if (
            songId.isBlank() ||
            loadedId == songId
        ) {
            return
        }

        loadedId = songId

        viewModelScope.launch {
            _state.value =
                LyricsEditState(
                    loading = true,
                    songId = songId,
                )

            val found =
                container.repository
                    .songFor(songId)

            if (found == null) {
                _state.value =
                    LyricsEditState(
                        loading = false,
                        songId = songId,
                        error =
                            "Track not found",
                    )

                return@launch
            }

            song = found

            val custom =
                container
                    .lyricsRepository
                    .customDraft(found)

            val current =
                if (custom == null) {
                    container
                        .lyricsRepository
                        .lyricsFor(found)
                } else {
                    null
                }

            val raw =
                when {
                    custom != null ->
                        custom.rawText

                    current?.synced == true ->
                        LyricsRepository
                            .formatLrc(
                                current.lines
                            )

                    current != null ->
                        current.lines
                            .joinToString(
                                "\\n"
                            ) {
                                it.text
                            }

                    else ->
                        ""
                }

            _state.value =
                LyricsEditState(
                    loading = false,
                    songId = songId,
                    title = found.title,
                    artist = found.artist,
                    rawText = raw,
                    synced =
                        custom?.synced
                            ?: current?.synced
                            ?: false,
                    offsetMs =
                        custom?.offsetMs
                            ?: 0,
                    timingLines =
                        if (
                            custom?.synced ==
                                true ||
                            current?.synced ==
                                true
                        ) {
                            parseTimingLines(
                                raw
                            )
                        } else {
                            emptyList()
                        },
                    source =
                        if (custom != null) {
                            "Custom"
                        } else {
                            current?.source
                                ?: "None"
                        },
                    hasCustom =
                        custom != null,
                )
        }
    }

    fun setText(
        value: String,
    ) {
        _state.update {
            it.copy(
                rawText = value,
                timingLines =
                    if (it.synced) {
                        parseTimingLines(
                            value
                        )
                    } else {
                        emptyList()
                    },
            )
        }
    }

    fun setSynced(
        value: Boolean,
    ) {
        _state.update {
            it.copy(
                synced = value,
                timingLines =
                    if (value) {
                        parseTimingLines(
                            it.rawText
                        )
                    } else {
                        emptyList()
                    },
            )
        }
    }

    fun setOffset(
        valueMs: Int,
    ) {
        _state.update {
            it.copy(
                offsetMs =
                    valueMs.coerceIn(
                        -30_000,
                        30_000,
                    )
            )
        }
    }

    fun adjustOffset(
        deltaMs: Int,
    ) {
        _state.update {
            it.copy(
                offsetMs =
                    (
                        it.offsetMs +
                            deltaMs
                    ).coerceIn(
                        -30_000,
                        30_000,
                    )
            )
        }
    }

    fun adjustLineTime(
        rawLineIndex: Int,
        deltaMs: Int,
    ) {
        val line =
            _state.value
                .timingLines
                .firstOrNull {
                    it.rawLineIndex ==
                        rawLineIndex
                }
                ?: return

        setLineTime(
            rawLineIndex,
            line.timeMs +
                deltaMs,
        )
    }

    fun setLineTime(
        rawLineIndex: Int,
        timeMs: Long,
    ) {
        _state.update { current ->
            val updated =
                rewriteTimestamp(
                    rawText =
                        current.rawText,
                    rawLineIndex =
                        rawLineIndex,
                    newTimeMs =
                        timeMs.coerceAtLeast(
                            0L
                        ),
                )

            current.copy(
                rawText = updated,
                synced = true,
                timingLines =
                    parseTimingLines(
                        updated
                    ),
            )
        }
    }

    private fun parseTimingLines(
        rawText: String,
    ): List<LyricsTimingLine> =
        rawText
            .split("\n")
            .mapIndexedNotNull {
                index,
                raw ->

                val match =
                    TIMESTAMP.find(raw)
                        ?: return@mapIndexedNotNull null

                LyricsTimingLine(
                    rawLineIndex = index,
                    timeMs =
                        timestampMs(match),
                    text =
                        raw.substring(
                            match.range.last + 1
                        ).trimStart(),
                )
            }

    private fun rewriteTimestamp(
        rawText: String,
        rawLineIndex: Int,
        newTimeMs: Long,
    ): String {
        val lines =
            rawText
                .split("\n")
                .toMutableList()

        if (
            rawLineIndex !in
                lines.indices
        ) {
            return rawText
        }

        val raw =
            lines[rawLineIndex]

        val match =
            TIMESTAMP.find(raw)
                ?: return rawText

        lines[rawLineIndex] =
            raw.replaceRange(
                match.range,
                formatTimestamp(
                    newTimeMs
                ),
            )

        return lines.joinToString(
            "\n"
        )
    }

    private fun timestampMs(
        match: MatchResult,
    ): Long {
        val min =
            match.groupValues[1]
                .toLongOrNull()
                ?: 0L

        val sec =
            match.groupValues[2]
                .toLongOrNull()
                ?: 0L

        val fraction =
            match.groupValues[3]

        val millis =
            when (fraction.length) {
                0 -> 0L

                1 ->
                    (
                        fraction
                            .toLongOrNull()
                            ?: 0L
                    ) * 100L

                2 ->
                    (
                        fraction
                            .toLongOrNull()
                            ?: 0L
                    ) * 10L

                else ->
                    fraction
                        .take(3)
                        .toLongOrNull()
                        ?: 0L
            }

        return (
            min * 60L +
                sec
        ) * 1000L +
            millis
    }

    private fun formatTimestamp(
        timeMs: Long,
    ): String {
        val safe =
            timeMs.coerceAtLeast(
                0L
            )

        val min =
            safe / 60_000L

        val sec =
            (
                safe % 60_000L
            ) / 1000L

        val millis =
            safe % 1000L

        return "[%02d:%02d.%03d]".format(
            min,
            sec,
            millis,
        )
    }

    fun save(
        onResult: (Boolean) -> Unit,
    ) {
        val currentSong =
            song ?: return

        val snapshot =
            _state.value

        if (snapshot.saving) {
            return
        }

        _state.update {
            it.copy(
                saving = true
            )
        }

        viewModelScope.launch {
            /*
             * If the user pasted valid LRC timestamps,
             * treat it as synced lyrics automatically.
             *
             * This prevents:
             * [00:08.00]Line
             *
             * from being rendered literally as plain text
             * just because the mode toggle was not changed.
             */
            val detectedLrc =
                LyricsRepository
                    .parseLrc(
                        snapshot.rawText
                    )
                    .isNotEmpty()

            val effectiveSynced =
                snapshot.synced ||
                    detectedLrc

            val ok =
                container
                    .lyricsRepository
                    .saveCustomLyrics(
                        currentSong,
                        snapshot.rawText,
                        effectiveSynced,
                        snapshot.offsetMs,
                    )

            _state.update {
                it.copy(
                    saving = false,
                    synced =
                        if (ok) {
                            effectiveSynced
                        } else {
                            it.synced
                        },
                    hasCustom =
                        ok ||
                            it.hasCustom,
                    source =
                        if (ok) {
                            "Custom"
                        } else {
                            it.source
                        },
                )
            }

            onResult(ok)
        }
    }

    fun clear(
        onResult: (Boolean) -> Unit,
    ) {
        val currentSong =
            song ?: return

        viewModelScope.launch {
            val ok =
                container
                    .lyricsRepository
                    .clearCustomLyrics(
                        currentSong
                    )

            if (ok) {
                loadedId = null
                load(currentSong.id)
            }

            onResult(ok)
        }
    }
    companion object {
        private val TIMESTAMP =
            Regex(
                """^\s*\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]"""
            )
    }

}
