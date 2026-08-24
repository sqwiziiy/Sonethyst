package com.mentality.sonethyst.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.DuplicateFinder
import com.mentality.sonethyst.data.DuplicateGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    init { scan() }

    fun scan() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val songs = container.repository.librarySongs(5000)
            _state.update { it.copy(loading = false, scanned = songs.size, groups = DuplicateFinder.find(songs)) }
        }
    }
}
