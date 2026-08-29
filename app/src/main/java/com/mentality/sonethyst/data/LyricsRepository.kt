package com.mentality.sonethyst.data

import com.mentality.sonethyst.model.LyricLine
import com.mentality.sonethyst.model.Song
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class Lyrics(val lines: List<LyricLine>, val synced: Boolean, val source: String)

class LyricsRepository(
    private val backendProvider: () -> MediaBackend?,
    private val lrclibEnabledProvider: () -> Boolean,
    private val overrideStore: LyricsOverrideStore? = null,
) {
    private val _revision =
        MutableStateFlow(0L)

    val revision:
        StateFlow<Long> =
        _revision.asStateFlow()
    private val gson = Gson()
    private val http = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun lyricsFor(song: Song): Lyrics? {
        /*
         * Explicit user lyrics always win over automatic
         * server / LRCLIB lookup.
         */
        overrideStore
            ?.lyricsFor(song)
            ?.let {
                return it
            }

        val server = runCatching { backendProvider()?.serverLyrics(song) }.getOrNull()
        // synced always wins regardless of source
        if (server != null && server.synced) return server
        val lrc = if (lrclibEnabledProvider()) fetchLrcLib(song) else null
        if (lrc != null && lrc.synced) return lrc
        return server ?: lrc
    }

    fun customDraft(
        song: Song,
    ): LyricsOverrideDraft? =
        overrideStore
            ?.getDraft(song)

    suspend fun saveCustomLyrics(
        song: Song,
        rawText: String,
        synced: Boolean,
        offsetMs: Int = 0,
    ): Boolean {
        val store =
            overrideStore
                ?: return false

        val ok =
            store.save(
                song,
                rawText,
                synced,
                offsetMs,
            )

        if (ok) {
            _revision.value++
        }

        return ok
    }

    suspend fun clearCustomLyrics(
        song: Song,
    ): Boolean {
        val store =
            overrideStore
                ?: return false

        val ok =
            store.remove(song)

        if (ok) {
            _revision.value++
        }

        return ok
    }

    private suspend fun fetchLrcLib(song: Song): Lyrics? = withContext(Dispatchers.IO) {
        val exact = runCatching { lrcGet(song) }.getOrNull()
        exact?.syncedLyrics?.takeIf { it.isNotBlank() }?.let { return@withContext Lyrics(parseLrc(it), true, "LRCLIB") }

        val results = runCatching { lrcSearch(song) }.getOrNull().orEmpty()
        results.firstOrNull { !it.syncedLyrics.isNullOrBlank() }
            ?.let { return@withContext Lyrics(parseLrc(it.syncedLyrics!!), true, "LRCLIB") }

        exact?.plainLyrics?.takeIf { it.isNotBlank() }
            ?.let { return@withContext Lyrics(it.lines().map { l -> LyricLine(-1, l) }, false, "LRCLIB") }
        results.firstOrNull { !it.plainLyrics.isNullOrBlank() }
            ?.let { return@withContext Lyrics(it.plainLyrics!!.lines().map { l -> LyricLine(-1, l) }, false, "LRCLIB") }
        null
    }

    private fun lrcGet(song: Song): LrcLibDto? {
        val url = buildString {
            append("https://lrclib.net/api/get")
            append("?artist_name=").append(enc(song.artist))
            append("&track_name=").append(enc(song.title))
            if (song.album.isNotBlank()) append("&album_name=").append(enc(song.album))
            if (song.durationSec > 0) append("&duration=").append(song.durationSec)
        }
        http.newCall(req(url)).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val body = resp.body?.string() ?: return null
            return gson.fromJson(body, LrcLibDto::class.java)
        }
    }

    private fun lrcSearch(song: Song): List<LrcLibDto> {
        val url = buildString {
            append("https://lrclib.net/api/search")
            append("?artist_name=").append(enc(song.artist))
            append("&track_name=").append(enc(song.title))
        }
        http.newCall(req(url)).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body?.string() ?: return emptyList()
            return runCatching { gson.fromJson(body, Array<LrcLibDto>::class.java).toList() }.getOrDefault(emptyList())
        }
    }

    private fun req(url: String): Request =
        Request.Builder().url(url).header("User-Agent", "Sonethyst Music Player").build()

    private data class LrcLibDto(val syncedLyrics: String? = null, val plainLyrics: String? = null)

    companion object {
        private fun enc(s: String): String = URLEncoder.encode(s, "UTF-8")

        private val TAG = Regex("""\[(\d+):(\d{1,2})(?:[.:](\d{1,3}))?]""")

        fun formatLrc(
            lines: List<LyricLine>,
        ): String =
            lines
                .filter {
                    it.timeMs >= 0L ||
                        it.timeSec >= 0
                }
                .joinToString("\n") { line ->
                    val totalMs =
                        (
                            if (line.timeMs >= 0L) {
                                line.timeMs
                            } else {
                                line.timeSec
                                    .toLong() *
                                    1000L
                            }
                        ).coerceAtLeast(0L)

                    val min =
                        totalMs / 60_000L

                    val sec =
                        (totalMs % 60_000L) /
                            1000L

                    val ms =
                        totalMs % 1000L

                    "[%02d:%02d.%03d]%s".format(
                        min,
                        sec,
                        ms,
                        line.text,
                    )
                }

        fun parseLrc(lrc: String): List<LyricLine> {
            val out = mutableListOf<Pair<Long, String>>()
            lrc.lineSequence().forEach { raw ->
                val tags = TAG.findAll(raw).toList()
                if (tags.isEmpty()) return@forEach
                val text = raw.substring(tags.last().range.last + 1).trim()
                tags.forEach { m ->
                    val min =
                        m.groupValues[1]
                            .toLongOrNull()
                            ?: 0L

                    val sec =
                        m.groupValues[2]
                            .toLongOrNull()
                            ?: 0L

                    val fraction =
                        m.groupValues[3]

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

                    val timeMs =
                        (
                            min * 60L +
                                sec
                        ) * 1000L +
                            millis

                    out.add(
                        timeMs to text
                    )
                }
            }
            return out
                .sortedBy { it.first }
                .map {
                    LyricLine(
                        timeSec =
                            (
                                it.first /
                                    1000L
                            ).toInt(),
                        text = it.second,
                        timeMs = it.first,
                    )
                }
        }
    }
}
