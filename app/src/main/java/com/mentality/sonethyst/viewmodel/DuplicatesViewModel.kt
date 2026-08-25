package com.mentality.sonethyst.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.DuplicateFinder
import com.mentality.sonethyst.data.DuplicateGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DuplicatesUiState(
    val loading: Boolean = true,
    val scanned: Int = 0,
    val groups: List<DuplicateGroup> = emptyList(),
)

/** Scans the reachable library for likely duplicate tracks. */
class DuplicatesViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as SonethystApplication).container
    private val _state = MutableStateFlow(DuplicatesUiState())
    val state: StateFlow<DuplicatesUiState> = _state.asStateFlow()

    init {
        scan(
            refreshLocal = true
        )
    }

    fun scan(
        refreshLocal: Boolean = false,
    ) {
        viewModelScope.launch {
            _state.update {
                it.copy(loading = true)
            }

            /*
             * LocalLibrary is an in-memory MediaStore snapshot.
             * A manual duplicate rescan must refresh that snapshot first,
             * otherwise newly added/copied audio files are invisible until
             * the app is restarted.
             *
             * Initial screen load keeps refreshLocal=false to avoid doing
             * two local-library scans when the app already has fresh data.
             */
            if (refreshLocal) {
                runCatching {
                    container.refreshLocalLibrary()
                }
            }

            val songs =
                container.repository
                    .duplicateCandidates()

            val groups =
                withContext(Dispatchers.Default) {
                    DuplicateFinder.find(songs)
                }

            _state.update {
                it.copy(
                    loading = false,
                    scanned = songs.size,
                    groups = groups,
                )
            }
        }
    }
}
