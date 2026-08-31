package com.mentality.sonethyst.data

import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import org.jaudiotagger.tag.images.AndroidArtwork
import org.jaudiotagger.tag.reference.PictureTypes
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

data class AudioTags(
    val title: String = "",
    val artist: String = "",
    val album: String = "",
    val albumArtist: String = "",
    val genre: String = "",
    val year: String = "",
    val trackNumber: String = "",
)

class TagEditor(private val context: Context) {
    private val resolver get() = context.contentResolver

    init {
        runCatching { Logger.getLogger("org.jaudiotagger").level = Level.OFF }
    }

    fun contentUriFor(songId: String): Uri? =
        songId.toLongOrNull()?.let { ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, it) }

    /**
     * Resolves the URI supplied by the local library, without relying on the
     * externally visible song ID (which may be namespaced by MergedBackend).
     */
    companion object {
        fun localContentUriStringFor(streamUrl: String): String? =
            streamUrl.trim().let { value ->
                val authority = value.substringAfter("//", "").substringBefore('/').substringBefore('?')
                val path = value.substringAfter("//", "").substringAfter('/', "").substringBefore('?')
                value.takeIf {
                    it.startsWith("content://", ignoreCase = true) && authority.isNotBlank() && path.isNotBlank()
                }
            }

        fun localContentUriFor(streamUrl: String): Uri? =
            localContentUriStringFor(streamUrl)?.let { value ->
                runCatching { Uri.parse(value) }.getOrNull()
            }
    }

    suspend fun read(path: String): AudioTags? = withContext(Dispatchers.IO) {
        runCatching {
            val f = File(path)
            if (!f.exists()) return@runCatching null
            val tag = AudioFileIO.read(f).tag ?: return@runCatching AudioTags()
            AudioTags(
                title = tag.firstOrEmpty(FieldKey.TITLE),
                artist = tag.firstOrEmpty(FieldKey.ARTIST),
                album = tag.firstOrEmpty(FieldKey.ALBUM),
                albumArtist = tag.firstOrEmpty(FieldKey.ALBUM_ARTIST),
                genre = tag.firstOrEmpty(FieldKey.GENRE),
                year = tag.firstOrEmpty(FieldKey.YEAR),
                trackNumber = tag.firstOrEmpty(FieldKey.TRACK),
            )
        }.getOrNull()
    }

    // android 11+ needs one-time user consent to write a media file the app doesnt own
    fun writeConsentIntent(
        uri: Uri,
    ): IntentSender? =
        writeConsentIntent(
            listOf(uri)
        )

    fun writeConsentIntent(
        uris: List<Uri>,
    ): IntentSender? =
        if (
            Build.VERSION.SDK_INT >=
                Build.VERSION_CODES.R &&
            uris.isNotEmpty()
        ) {
            MediaStore.createWriteRequest(
                resolver,
                uris.distinct(),
            ).intentSender
        } else {
            null
        }

    // jaudiotagger needs a real file so edit a cache copy then stream back through the resolver
    suspend fun write(uri: Uri, path: String, tags: AudioTags, artwork: ByteArray? = null): Boolean =
        withContext(Dispatchers.IO) {
            val ext = path.substringAfterLast('.', "tmp").ifBlank { "tmp" }
            val tmp = File(context.cacheDir, "tagedit_in.$ext")
            try {
                resolver.openInputStream(uri)?.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                    ?: return@withContext false
                val af = AudioFileIO.read(tmp)
                val tag = af.tagOrCreateAndSetDefault
                tag.put(FieldKey.TITLE, tags.title)
                tag.put(FieldKey.ARTIST, tags.artist)
                tag.put(FieldKey.ALBUM, tags.album)
                tag.put(FieldKey.ALBUM_ARTIST, tags.albumArtist)
                tag.put(FieldKey.GENRE, tags.genre)
                tag.put(FieldKey.YEAR, tags.year)
                tag.put(FieldKey.TRACK, tags.trackNumber)
                if (
                    artwork != null &&
                    artwork.isNotEmpty()
                ) {
                    /*
                     * Do not swallow an artwork-writing exception.
                     *
                     * Previously metadata could report "Tags saved"
                     * even when the embedded picture replacement itself
                     * had failed.
                     */
                    tag.deleteArtworkField()
                    tag.setField(
                        AndroidArtwork().apply {
                            binaryData =
                                artwork
                            mimeType =
                                artworkMimeType(
                                    artwork
                                )
                            pictureType =
                                PictureTypes.DEFAULT_ID
                        }
                    )
                }
                af.commit()
                resolver.openOutputStream(uri, "wt")?.use { out -> tmp.inputStream().use { it.copyTo(out) } }
                    ?: return@withContext false
                awaitMediaStoreRescan(path)
                true
            } catch (t: Throwable) {
                android.util.Log.e("TagEditor", "write($path) failed", t)
                false
            } finally {
                runCatching { tmp.delete() }
            }
        }

    private suspend fun awaitMediaStoreRescan(path: String) {
        if (path.isBlank()) return

        withTimeoutOrNull(5_000L) {
            suspendCancellableCoroutine { continuation ->
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(path),
                    null,
                ) { _, _ ->
                    if (continuation.isActive) {
                        continuation.resume(Unit)
                    }
                }
            }
        }
    }

    private fun artworkMimeType(
        data: ByteArray,
    ): String =
        when {
            data.size >= 4 &&
                (data[0].toInt() and 0xff) == 0x89 &&
                data[1].toInt() == 0x50 &&
                data[2].toInt() == 0x4e &&
                data[3].toInt() == 0x47 ->
                "image/png"

            data.size >= 3 &&
                (data[0].toInt() and 0xff) == 0xff &&
                (data[1].toInt() and 0xff) == 0xd8 &&
                (data[2].toInt() and 0xff) == 0xff ->
                "image/jpeg"

            data.size >= 12 &&
                data.copyOfRange(
                    8,
                    12,
                ).toString(
                    Charsets.US_ASCII
                ) == "WEBP" ->
                "image/webp"

            data.size >= 3 &&
                data.copyOfRange(
                    0,
                    3,
                ).toString(
                    Charsets.US_ASCII
                ) == "GIF" ->
                "image/gif"

            else ->
                "image/jpeg"
        }

    private fun Tag.firstOrEmpty(key: FieldKey): String = runCatching { getFirst(key) ?: "" }.getOrDefault("")

    // blank value deletes the field so clearing actually clears the tag
    private fun Tag.put(key: FieldKey, value: String) {
        runCatching {
            if (value.isBlank()) deleteField(key) else setField(key, value)
        }
    }
}
