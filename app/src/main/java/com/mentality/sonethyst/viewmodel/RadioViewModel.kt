package com.mentality.sonethyst.viewmodel

import android.app.Application
import android.icu.text.Transliterator
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.BUILTIN_RADIO_FALLBACKS
import com.mentality.sonethyst.data.RadioStation
import com.mentality.sonethyst.data.dedupeRadioStations
import com.mentality.sonethyst.data.normalizedRadioStreamUrl
import java.text.Normalizer
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RadioUiState(
    val loading: Boolean = false,
    val popular: List<RadioStation> = emptyList(),
    val results: List<RadioStation> = emptyList(),
    val query: String = "",
    val activeTag: String = "",
    val failed: Boolean = false,
)

class RadioViewModel(
    app: Application,
) : AndroidViewModel(app) {
    private val container =
        (app as SonethystApplication)
            .container

    private val _state =
        MutableStateFlow(
            RadioUiState()
        )

    val state:
        StateFlow<RadioUiState> =
        _state.asStateFlow()

    private var searchJob:
        Job? = null

    val favorites:
        StateFlow<List<RadioStation>> =
        container.settingsStore
            .radioFavorites
            .stateIn(
                viewModelScope,
                SharingStarted.Eagerly,
                emptyList(),
            )

    init {
        loadPopular()
    }

    fun loadPopular() {
        _state.update {
            it.copy(
                loading = true,
                failed = false,
                query = "",
                activeTag = "",
            )
        }

        viewModelScope.launch {
            val list =
                container.radioBrowser
                    .topStations()

            _state.update {
                it.copy(
                    popular =
                        dedupeRadioStations(
                            list.orEmpty()
                        ),
                    results =
                        emptyList(),
                    loading = false,
                    failed =
                        list == null,
                )
            }
        }
    }

    fun search(query: String) {
        val q =
            query.trim()

        searchJob?.cancel()

        if (q.isBlank()) {
            clearSearch()
            return
        }

        _state.update {
            it.copy(
                loading = true,
                failed = false,
                query = q,
                activeTag = "",
            )
        }

        searchJob =
            viewModelScope.launch {
                delay(350)

                val variants =
                    searchVariants(q)

                val remote =
                    mutableListOf<RadioStation>()

                var transportWorked =
                    false

                /*
                 * Strict first. Try alternate normalized/transliterated
                 * spellings only when they are actually different.
                 */
                for (variant in variants) {
                    val result =
                        container.radioBrowser
                            .search(
                                variant,
                                hideBroken = true,
                            )

                    if (result != null) {
                        transportWorked = true
                        remote += result
                    }

                    if (remote.size >= 80) {
                        break
                    }
                }

                /*
                 * Radio Browser can mark a perfectly usable station
                 * as broken. Only broaden the search if strict mode
                 * returned no station at all.
                 */
                if (
                    remote.isEmpty() &&
                    transportWorked
                ) {
                    for (variant in variants) {
                        val result =
                            container.radioBrowser
                                .search(
                                    variant,
                                    hideBroken = false,
                                )

                        if (result != null) {
                            transportWorked = true
                            remote += result
                        }

                        if (remote.size >= 80) {
                            break
                        }
                    }
                }

                val local =
                    favorites.value
                        .filter {
                            it.custom == true &&
                                stationMatches(
                                    it,
                                    variants,
                                )
                        }

                val builtin =
                    BUILTIN_RADIO_FALLBACKS
                        .filter {
                            stationMatches(
                                it,
                                variants,
                            )
                        }

                /*
                 * Priority:
                 * user's custom station
                 * -> trusted builtin fallback
                 * -> Radio Browser directory.
                 *
                 * URL dedupe removes duplicates automatically.
                 */
                val combined =
                    dedupeRadioStations(
                        local +
                            builtin +
                            remote
                    )

                _state.update {
                    it.copy(
                        results =
                            combined,
                        loading = false,
                        failed =
                            !transportWorked &&
                                local.isEmpty(),
                    )
                }
            }
    }

    fun byTag(tag: String) {
        _state.update {
            it.copy(
                loading = true,
                failed = false,
                query = "",
                activeTag = tag,
            )
        }

        viewModelScope.launch {
            val list =
                container.radioBrowser
                    .byTag(tag)

            _state.update {
                it.copy(
                    results =
                        dedupeRadioStations(
                            list.orEmpty()
                        ),
                    loading = false,
                    failed =
                        list == null,
                )
            }
        }
    }

    fun clearSearch() {
        _state.update {
            it.copy(
                query = "",
                activeTag = "",
                results = emptyList(),
                failed = false,
            )
        }
    }

    fun isFavorite(
        station: RadioStation,
    ): Boolean {
        val target =
            normalizedRadioStreamUrl(
                station.streamUrl
            )

        return target.isNotBlank() &&
            favorites.value.any {
                normalizedRadioStreamUrl(
                    it.streamUrl
                ) == target
            }
    }

    fun toggleFavorite(
        station: RadioStation,
    ) {
        viewModelScope.launch {
            val target =
                normalizedRadioStreamUrl(
                    station.streamUrl
                )

            val existing =
                favorites.value
                    .firstOrNull {
                        normalizedRadioStreamUrl(
                            it.streamUrl
                        ) == target
                    }

            if (existing != null) {
                container.settingsStore
                    .deleteRadioStation(
                        existing.uuid
                    )
            } else {
                container.settingsStore
                    .saveRadioStation(
                        station
                    )
            }
        }
    }

    fun addCustom(
        name: String,
        url: String,
    ): String? {
        val error =
            validateCustomStream(url)

        if (error != null) {
            return error
        }

        val clean =
            url.trim()

        val normalized =
            normalizedRadioStreamUrl(
                clean
            )

        val station =
            RadioStation(
                uuid =
                    "custom:" +
                        normalized.hashCode(),
                name =
                    name.trim()
                        .ifBlank {
                            clean.toHttpHostOrUrl()
                        },
                streamUrl = clean,
                tags = "Custom",
                custom = true,
            )

        viewModelScope.launch {
            container.settingsStore
                .saveRadioStation(
                    station
                )
        }

        return null
    }

    fun editCustom(
        station: RadioStation,
        name: String,
        url: String,
    ): String? {
        if (station.custom != true) {
            return "Only custom stations can be edited"
        }

        val error =
            validateCustomStream(
                url = url,
                ignoreUuid =
                    station.uuid,
            )

        if (error != null) {
            return error
        }

        val clean =
            url.trim()

        val updated =
            station.copy(
                name =
                    name.trim()
                        .ifBlank {
                            clean.toHttpHostOrUrl()
                        },
                streamUrl = clean,
                tags = "Custom",
                custom = true,
            )

        viewModelScope.launch {
            /*
             * UUID stays stable across edits. SettingsStore
             * replaces this record and URL-dedup prevents a
             * second copy of the same stream.
             */
            container.settingsStore
                .saveRadioStation(
                    updated
                )
        }

        return null
    }

    fun deleteCustom(
        station: RadioStation,
    ) {
        if (station.custom != true) {
            return
        }

        viewModelScope.launch {
            container.settingsStore
                .deleteRadioStation(
                    station.uuid
                )
        }
    }

    private fun validateCustomStream(
        url: String,
        ignoreUuid: String = "",
    ): String? {
        val clean =
            url.trim()

        if (clean.isBlank()) {
            return "Enter a stream URL"
        }

        if (
            clean.any {
                it.isWhitespace()
            }
        ) {
            return "The URL can't contain spaces"
        }

        val uri =
            runCatching {
                Uri.parse(clean)
            }.getOrNull()
                ?: return "Enter a valid stream URL"

        val scheme =
            uri.scheme
                ?.lowercase()

        if (
            scheme != "http" &&
            scheme != "https"
        ) {
            return "Use an http:// or https:// stream URL"
        }

        if (
            uri.host
                .isNullOrBlank()
        ) {
            return "Enter a valid stream URL"
        }

        val normalized =
            normalizedRadioStreamUrl(
                clean
            )

        if (normalized.isBlank()) {
            return "Enter a valid stream URL"
        }

        val duplicate =
            favorites.value
                .firstOrNull {
                    it.uuid !=
                        ignoreUuid &&
                        normalizedRadioStreamUrl(
                            it.streamUrl
                        ) == normalized
                }

        if (duplicate != null) {
            return "This stream is already saved as “${duplicate.displayName}”"
        }

        return null
    }

    fun registerPlay(
        station: RadioStation,
    ) {
        viewModelScope.launch {
            runCatching {
                container.radioBrowser
                    .registerClick(
                        station.uuid
                    )
            }
        }
    }

    private fun searchVariants(
        query: String,
    ): List<String> {
        val original =
            query.trim()

        val normalized =
            normalizeSearchText(
                original
            )

        val latin =
            runCatching {
                Transliterator
                    .getInstance(
                        "Any-Latin; " +
                            "NFD; " +
                            "[:Nonspacing Mark:] Remove; " +
                            "NFC"
                    )
                    .transliterate(
                        original
                    )
            }.getOrDefault(
                original
            )
                .trim()

        return listOf(
            original,
            normalized,
            latin,
            normalizeSearchText(latin),
        )
            .map {
                it.trim()
            }
            .filter {
                it.isNotBlank()
            }
            .distinctBy {
                it.lowercase(
                    Locale.ROOT
                )
            }
    }

    private fun normalizeSearchText(
        value: String,
    ): String =
        Normalizer.normalize(
            value,
            Normalizer.Form.NFKD,
        )
            .replace(
                Regex("\\p{M}+"),
                "",
            )
            .replace(
                Regex("[^\\p{L}\\p{N}]+"),
                " ",
            )
            .trim()
            .lowercase(
                Locale.ROOT
            )

    private fun stationMatches(
        station: RadioStation,
        variants: List<String>,
    ): Boolean {
        val haystack =
            normalizeSearchText(
                listOf(
                    station.displayName,
                    station.tags.orEmpty(),
                    station.country.orEmpty(),
                    station.streamUrl.orEmpty(),
                ).joinToString(" ")
            )

        return variants.any {
            val needle =
                normalizeSearchText(it)

            needle.isNotBlank() &&
                haystack.contains(
                    needle
                )
        }
    }

    private fun String.toHttpHostOrUrl():
        String =
        runCatching {
            android.net.Uri
                .parse(this)
                .host
                ?: this
        }.getOrDefault(this)
}
