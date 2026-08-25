package com.mentality.sonethyst.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.SearchResults
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SearchUiState(
    val query: String = "",
    val loading: Boolean = false,
    val results: SearchResults = SearchResults(),
)

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    private val container = (app as SonethystApplication).container
    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    private var searchJob: Job? = null

    val recentSearches: StateFlow<List<String>> =
        container.settingsStore.recentSearches.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        viewModelScope.launch {
            container.offline.collect {
                refreshCurrentQuery()
            }
        }

        viewModelScope.launch {
            container.libraryReload.drop(1).collect {
                refreshCurrentQuery()
            }
        }
        viewModelScope.launch {
            container.hiddenReload.drop(1).collect {
                refreshCurrentQuery()
            }
        }
        viewModelScope.launch {
            container.ratingChanges.collect { change ->
                _state.update { current ->
                    current.copy(
                        results =
                            current.results.copy(
                                songs =
                                    current.results.songs.map {
                                        song ->
                                        if (
                                            song.id ==
                                                change.songId
                                        ) {
                                            song.copy(
                                                rating =
                                                    change.rating
                                            )
                                        } else {
                                            song
                                        }
                                    }
                            )
                    )
                }
            }
        }

    }

    private fun refreshCurrentQuery() {
        val query = _state.value.query
        if (query.isNotBlank()) {
            onQuery(query)
        }
    }

    fun commit() {
        viewModelScope.launch { container.settingsStore.addRecentSearch(_state.value.query) }
    }

    fun removeRecent(query: String) {
        viewModelScope.launch { container.settingsStore.removeRecentSearch(query) }
    }

    fun clearRecents() {
        viewModelScope.launch { container.settingsStore.clearRecentSearches() }
    }

    fun onQuery(q: String) {
        _state.update { it.copy(query = q) }
        searchJob?.cancel()
        if (q.isBlank()) {
            _state.update { it.copy(results = SearchResults(), loading = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(300)
            _state.update { it.copy(loading = true) }
            val r = container.repository.search(q)
            _state.update { it.copy(loading = false, results = r) }
        }
    }
}
