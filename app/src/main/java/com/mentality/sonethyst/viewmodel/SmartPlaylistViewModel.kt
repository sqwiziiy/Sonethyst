package com.mentality.sonethyst.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.SmartPlaylist
import com.mentality.sonethyst.data.SmartRule
import com.mentality.sonethyst.model.Song
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Edits one smart playlist (a new one when [load] gets a blank id). */
class SmartPlaylistViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val container =
        (app as SonethystApplication)
            .container

    private val _state =
        MutableStateFlow(
            SmartPlaylist()
        )

    val state:
        StateFlow<SmartPlaylist> =
        _state.asStateFlow()

    private val _previewTracks =
        MutableStateFlow<List<Song>>(
            emptyList()
        )

    val previewTracks:
        StateFlow<List<Song>> =
        _previewTracks.asStateFlow()

    private var loadedId:
        String? = null

    private var sourceSongs:
        List<Song> = emptyList()

    private var previewJob:
        Job? = null

    fun load(id: String) {
        if (id == loadedId) {
            return
        }

        loadedId = id

        viewModelScope.launch {
            val playlist =
                if (id.isBlank()) {
                    SmartPlaylist(
                        id =
                            "smart-" +
                                UUID.randomUUID()
                                    .toString()
                                    .take(8),
                        name = "",
                        rules =
                            listOf(
                                SmartRule()
                            ),
                    )
                } else {
                    container.settingsStore
                        .smartPlaylists
                        .first()
                        .firstOrNull {
                            it.id == id
                        }
                        ?: SmartPlaylist(
                            id = id,
                            rules =
                                listOf(
                                    SmartRule()
                                ),
                        )
                }

            _state.value =
                playlist

            sourceSongs =
                runCatching {
                    container.repository
                        .librarySongs()
                }.getOrDefault(
                    emptyList()
                )

            refreshPreview(
                immediate = true
            )
        }
    }

    fun update(
        transform:
            (SmartPlaylist) ->
                SmartPlaylist,
    ) {
        _state.update(transform)
        refreshPreview()
    }

    private fun refreshPreview(
        immediate: Boolean = false,
    ) {
        val playlist =
            _state.value

        val source =
            sourceSongs

        previewJob?.cancel()

        previewJob =
            viewModelScope.launch {
                if (!immediate) {
                    delay(100)
                }

                val tracks =
                    withContext(
                        Dispatchers.Default
                    ) {
                        container.smartEngine
                            .evaluate(
                                playlist,
                                source,
                            )
                    }

                /*
                 * Do not publish an obsolete evaluation if the
                 * user changed another rule while it was running.
                 */
                if (_state.value == playlist) {
                    _previewTracks.value =
                        tracks
                }
            }
    }

    fun save(
        onDone: () -> Unit,
    ) {
        val sp = _state.value

        if (sp.id.isNullOrBlank()) {
            return
        }

        viewModelScope.launch {
            container.settingsStore
                .saveSmartPlaylist(
                    sp.copy(
                        name =
                            sp.name
                                ?.trim()
                                ?.takeIf {
                                    it.isNotBlank()
                                }
                    )
                )

            onDone()
        }
    }
}
