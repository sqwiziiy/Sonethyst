package com.mentality.sonethyst.data.remote

import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// nullable per gson null trap

data class MbRecordingResult(val recordings: List<MbRecording>? = emptyList())

data class MbRecording(
    val id: String? = "",
    val title: String? = "",
    val length: Long? = null,
    val score: Int? = 0,
    @SerializedName("artist-credit") val artistCredit: List<MbArtistCredit>? = emptyList(),
    val releases: List<MbRelease>? = emptyList(),
)

data class MbArtistCredit(val name: String? = "", val joinphrase: String? = "", val artist: MbArtist? = null)
data class MbArtist(val id: String? = "", val name: String? = "")

data class MbRelease(
    val id: String? = "",
    val title: String? = "",
    val date: String? = "",
    @SerializedName("release-group") val releaseGroup: MbReleaseGroup? = null,
    val media: List<MbMedia>? = emptyList(),
)

data class MbReleaseGroup(val id: String? = "", @SerializedName("primary-type") val primaryType: String? = "")
data class MbMedia(val position: Int? = null, val track: List<MbTrack>? = emptyList())
data class MbTrack(val number: String? = "", val title: String? = "")

interface MusicBrainzApi {
    @GET("ws/2/recording")
    suspend fun searchRecording(
        @Query("query") query: String,
        @Query("fmt") fmt: String = "json",
        @Query("limit") limit: Int = 8,
    ): MbRecordingResult
}

data class MetadataMatch(
    val title: String,
    val artist: String,
    val album: String,
    val year: String,
    val trackNumber: String,
    val coverUrl: String,
    val score: Int,
)

// musicbrainz requires a descriptive user-agent and rate-limits ~1 req/s

class MusicBrainzClient {
    private val http = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .build()
            )
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: MusicBrainzApi = Retrofit.Builder()
        .baseUrl("https://musicbrainz.org/")
        .client(http)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(MusicBrainzApi::class.java)

    private fun esc(s: String) = s.replace("\\", "\\\\").replace("\"", "\\\"")

    suspend fun search(
        title: String,
        artist: String,
        album: String = "",
    ): List<MetadataMatch> {
        val cleanTitle =
            title.trim()

        val cleanArtist =
            artist.trim()

        if (
            cleanTitle.isBlank() &&
            cleanArtist.isBlank()
        ) {
            return emptyList()
        }

        /*
         * Album is deliberately NOT a mandatory MusicBrainz term.
         *
         * Local files commonly contain folder names, custom albums,
         * "Unknown album", etc. Requiring release:"..." made a valid
         * title + artist return zero results.
         */
        val first =
            queryRecordings(
                title = cleanTitle,
                artist = cleanArtist,
            )

        val candidates =
            if (first.isNotEmpty()) {
                first
            } else {
                val simplified =
                    simplifiedTitle(
                        cleanTitle
                    )

                if (
                    simplified.isNotBlank() &&
                    !simplified.equals(
                        cleanTitle,
                        ignoreCase = true,
                    )
                ) {
                    /*
                     * Respect MusicBrainz's public rate limit before
                     * issuing a fallback query.
                     */
                    delay(1100L)

                    queryRecordings(
                        title = simplified,
                        artist = cleanArtist,
                    )
                } else {
                    emptyList()
                }
            }

        return candidates
            .distinctBy {
                listOf(
                    it.title,
                    it.artist,
                    it.album,
                    it.year,
                    it.trackNumber,
                )
                    .joinToString("|")
                    .lowercase()
            }
            .sortedByDescending {
                rankMatch(
                    match = it,
                    title = cleanTitle,
                    artist = cleanArtist,
                    album = album,
                )
            }
            .take(8)
    }

    suspend fun searchArtwork(
        title: String,
        artist: String,
        album: String = "",
    ): List<MetadataMatch> =
        search(
            title = title,
            artist = artist,
            album = album,
        )
            .filter {
                it.coverUrl.isNotBlank()
            }

    private suspend fun queryRecordings(
        title: String,
        artist: String,
    ): List<MetadataMatch> {
        val terms =
            buildList {
                if (title.isNotBlank()) {
                    add(
                        "recording:\"${esc(title)}\""
                    )
                }

                if (artist.isNotBlank()) {
                    add(
                        "artist:\"${esc(artist)}\""
                    )
                }
            }

        if (terms.isEmpty()) {
            return emptyList()
        }

        val query =
            terms.joinToString(
                " AND "
            )

        val result =
            runCatching {
                api.searchRecording(
                    query
                )
            }.getOrNull()
                ?: return emptyList()

        return result
            .recordings
            .orEmpty()
            .mapNotNull {
                it.toMatch()
            }
    }

    private fun simplifiedTitle(
        input: String,
    ): String {
        var value =
            input
                .trim()

        val suffix =
            Regex(
                """\s*[\[(][^\])]*(?:slowed|reverb|remix|sped\s*up|nightcore|edit|version)[^\])]*[\])]\s*$""",
                RegexOption.IGNORE_CASE,
            )

        while (true) {
            val next =
                value.replace(
                    suffix,
                    "",
                ).trim()

            if (next == value) {
                break
            }

            value = next
        }

        return value
            .replace(
                Regex("""\s+"""),
                " ",
            )
            .trim()
    }

    private fun rankMatch(
        match: MetadataMatch,
        title: String,
        artist: String,
        album: String,
    ): Int {
        fun key(value: String) =
            value
                .lowercase()
                .replace(
                    Regex("""[^\p{L}\p{N}]+"""),
                    " ",
                )
                .trim()

        val wantedTitle =
            key(
                simplifiedTitle(
                    title
                )
            )

        val resultTitle =
            key(
                simplifiedTitle(
                    match.title
                )
            )

        val wantedArtist =
            key(artist)

        val resultArtist =
            key(match.artist)

        val wantedAlbum =
            key(album)

        val resultAlbum =
            key(match.album)

        var rank =
            match.score

        if (
            wantedTitle.isNotBlank() &&
            wantedTitle ==
                resultTitle
        ) {
            rank += 250
        }

        if (
            wantedArtist.isNotBlank() &&
            wantedArtist ==
                resultArtist
        ) {
            rank += 150
        }

        if (
            wantedAlbum.isNotBlank() &&
            wantedAlbum ==
                resultAlbum
        ) {
            rank += 100
        } else if (
            wantedAlbum.isNotBlank() &&
            resultAlbum.contains(
                wantedAlbum
            )
        ) {
            rank += 35
        }

        return rank
    }

    suspend fun fetchImage(url: String): ByteArray? = withContext(Dispatchers.IO) {
        if (url.isBlank()) return@withContext null
        runCatching {
            http.newCall(Request.Builder().url(url).header("User-Agent", USER_AGENT).build()).execute().use {
                if (it.isSuccessful) it.body?.bytes() else null
            }
        }.getOrNull()
    }

    private fun MbRecording.toMatch(): MetadataMatch? {
        val t = title?.takeIf { it.isNotBlank() } ?: return null
        val artistName = artistCredit.orEmpty().joinToString("") { (it.name ?: it.artist?.name ?: "") + (it.joinphrase ?: "") }
            .ifBlank { artistCredit.orEmpty().firstOrNull()?.artist?.name ?: "" }
        // prefer an official album release over singles/compilations for album + track number
        val release = releases.orEmpty().firstOrNull { it.releaseGroup?.primaryType.equals("Album", true) }
            ?: releases.orEmpty().firstOrNull()
        val trackNo = release?.media.orEmpty().firstNotNullOfOrNull { m -> m.track.orEmpty().firstOrNull()?.number }
        val cover = release?.id?.takeIf { it.isNotBlank() }?.let { "https://coverartarchive.org/release/$it/front-500" } ?: ""
        return MetadataMatch(
            title = t,
            artist = artistName.trim(),
            album = release?.title ?: "",
            year = release?.date?.take(4) ?: "",
            trackNumber = trackNo ?: "",
            coverUrl = cover,
            score = score ?: 0,
        )
    }

    private companion object {
        const val USER_AGENT = "Sonethyst/0.1 (https://github.com/sqwiziiy/Sonethyst)"
    }
}
