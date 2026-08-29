package com.mentality.sonethyst.viewmodel

import android.app.Application
import android.content.IntentSender
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.AudioTags
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class BatchTagField {
    TITLE,
    ARTIST,
    ALBUM,
    ALBUM_ARTIST,
    GENRE,
    YEAR,
    TRACK_NUMBER,
}

data class BatchTagFieldState(
    val value: String = "",
    val mixed: Boolean = false,
    val enabled: Boolean = false,
)

data class BatchTagItem(
    val songId: String,
    val path: String,
    val localFile: Boolean,
    val tags: AudioTags,
)

data class BatchTagEditState(
    val loading: Boolean = true,
    val items: List<BatchTagItem> =
        emptyList(),
    val fields:
        Map<
            BatchTagField,
            BatchTagFieldState,
        > = emptyMap(),
    val saving: Boolean = false,
)

class BatchTagEditViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val container =
        (app as SonethystApplication)
            .container

    private val _state =
        MutableStateFlow(
            BatchTagEditState()
        )

    val state:
        StateFlow<BatchTagEditState> =
        _state.asStateFlow()

    private var loadedKey:
        String? = null

    fun load(
        songIds: List<String>,
    ) {
        val ids =
            songIds
                .filter {
                    it.isNotBlank()
                }
                .distinct()

        val key =
            ids.joinToString(
                "\u001F"
            )

        if (key == loadedKey) {
            return
        }

        loadedKey = key

        viewModelScope.launch {
            _state.value =
                BatchTagEditState(
                    loading = true
                )

            val items =
                ids.mapNotNull { id ->
                    val song =
                        container.repository
                            .songFor(id)
                            ?: return@mapNotNull null

                    val local =
                        song.streamUrl
                            .startsWith(
                                "content://"
                            )

                    val sourceTags =
                        if (local) {
                            song.path
                                .takeIf {
                                    it.isNotBlank()
                                }
                                ?.let {
                                    container
                                        .tagEditor
                                        .read(it)
                                }
                        } else {
                            container.repository
                                .readMetadata(id)
                        }

                    val tags =
                        sourceTags
                            ?: AudioTags(
                                title =
                                    song.title,
                                artist =
                                    song.artist,
                                album =
                                    song.album,
                                albumArtist =
                                    song.albumArtist,
                                genre =
                                    song.genres
                                        .joinToString(
                                            "; "
                                        ),
                            )

                    BatchTagItem(
                        songId = id,
                        path = song.path,
                        localFile = local,
                        tags = tags,
                    )
                }

            _state.value =
                BatchTagEditState(
                    loading = false,
                    items = items,
                    fields =
                        buildFields(items),
                )
        }
    }

    fun toggleField(
        field: BatchTagField,
        enabled: Boolean,
    ) {
        _state.update { current ->
            val old =
                current.fields[field]
                    ?: return@update current

            current.copy(
                fields =
                    current.fields +
                        (
                            field to
                                old.copy(
                                    enabled =
                                        enabled
                                )
                        )
            )
        }
    }

    fun setValue(
        field: BatchTagField,
        value: String,
    ) {
        val normalized =
            when (field) {
                BatchTagField.YEAR,
                BatchTagField.TRACK_NUMBER,
                ->
                    value.filter {
                        it.isDigit()
                    }

                else ->
                    value
            }

        _state.update { current ->
            val old =
                current.fields[field]
                    ?: return@update current

            current.copy(
                fields =
                    current.fields +
                        (
                            field to
                                old.copy(
                                    value =
                                        normalized,
                                    enabled =
                                        true,
                                )
                        )
            )
        }
    }

    fun writeConsentIntent():
        IntentSender? {
        val uris =
            _state.value
                .items
                .filter {
                    it.localFile
                }
                .mapNotNull {
                    container
                        .tagEditor
                        .contentUriFor(
                            it.songId
                        )
                }

        return container
            .tagEditor
            .writeConsentIntent(uris)
    }

    fun save(
        onFinished:
            (
                succeeded: Int,
                failed: Int,
            ) -> Unit,
    ) {
        val snapshot =
            _state.value

        if (
            snapshot.saving ||
            snapshot.items.isEmpty() ||
            snapshot.fields.values
                .none { it.enabled }
        ) {
            return
        }

        _state.update {
            it.copy(saving = true)
        }

        viewModelScope.launch {
            var succeeded = 0
            var failed = 0

            val changedLocalPaths =
                linkedSetOf<String>()

            snapshot.items
                .forEach { item ->
                    val updatedTags =
                        applyFields(
                            item.tags,
                            snapshot.fields,
                        )

                    val ok =
                        if (item.localFile) {
                            val uri =
                                container
                                    .tagEditor
                                    .contentUriFor(
                                        item.songId
                                    )

                            if (
                                uri == null ||
                                item.path
                                    .isBlank()
                            ) {
                                false
                            } else {
                                container
                                    .tagEditor
                                    .write(
                                        uri,
                                        item.path,
                                        updatedTags,
                                    )
                            }
                        } else {
                            container.repository
                                .updateMetadata(
                                    item.songId,
                                    updatedTags,
                                )
                        }

                    if (ok) {
                        succeeded++

                        if (
                            item.localFile &&
                            item.path
                                .isNotBlank()
                        ) {
                            changedLocalPaths +=
                                item.path
                        }
                    } else {
                        failed++
                    }
                }

            if (
                changedLocalPaths
                    .isNotEmpty()
            ) {
                changedLocalPaths
                    .forEach { path ->
                        container.localLibrary
                            .refreshTagMetadata(
                                path
                            )
                    }

                /*
                 * One library rebuild after all file writes,
                 * not one full rescan per selected song.
                 */
                container.refreshLocalLibrary()
            } else if (
                succeeded > 0
            ) {
                container
                    .notifyLibraryMetadataChanged()
            }

            _state.update {
                it.copy(saving = false)
            }

            onFinished(
                succeeded,
                failed,
            )
        }
    }

    private fun buildFields(
        items: List<BatchTagItem>,
    ): Map<
        BatchTagField,
        BatchTagFieldState,
    > =
        BatchTagField
            .values()
            .associateWith { field ->
                val values =
                    items.map {
                        valueFor(
                            it.tags,
                            field,
                        )
                    }

                val distinct =
                    values.distinct()

                BatchTagFieldState(
                    value =
                        if (
                            distinct.size == 1
                        ) {
                            distinct.first()
                        } else {
                            ""
                        },
                    mixed =
                        distinct.size > 1,
                    enabled = false,
                )
            }

    private fun valueFor(
        tags: AudioTags,
        field: BatchTagField,
    ): String =
        when (field) {
            BatchTagField.TITLE ->
                tags.title

            BatchTagField.ARTIST ->
                tags.artist

            BatchTagField.ALBUM ->
                tags.album

            BatchTagField.ALBUM_ARTIST ->
                tags.albumArtist

            BatchTagField.GENRE ->
                tags.genre

            BatchTagField.YEAR ->
                tags.year

            BatchTagField.TRACK_NUMBER ->
                tags.trackNumber
        }

    private fun applyFields(
        original: AudioTags,
        fields:
            Map<
                BatchTagField,
                BatchTagFieldState,
            >,
    ): AudioTags {
        fun edit(
            field: BatchTagField,
            fallback: String,
        ): String =
            fields[field]
                ?.takeIf {
                    it.enabled
                }
                ?.value
                ?: fallback

        return original.copy(
            title =
                edit(
                    BatchTagField.TITLE,
                    original.title,
                ),
            artist =
                edit(
                    BatchTagField.ARTIST,
                    original.artist,
                ),
            album =
                edit(
                    BatchTagField.ALBUM,
                    original.album,
                ),
            albumArtist =
                edit(
                    BatchTagField.ALBUM_ARTIST,
                    original.albumArtist,
                ),
            genre =
                edit(
                    BatchTagField.GENRE,
                    original.genre,
                ),
            year =
                edit(
                    BatchTagField.YEAR,
                    original.year,
                ),
            trackNumber =
                edit(
                    BatchTagField.TRACK_NUMBER,
                    original.trackNumber,
                ),
        )
    }
}
