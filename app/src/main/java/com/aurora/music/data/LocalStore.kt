package com.aurora.music.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.UUID

// fields nullable-safe for gson forward compat
data class LocalPlaylist(
    val id: String = "",
    val title: String? = "",
    val subtitle: String? = "",
    val trackIds: List<String>? = emptyList(),
    val coverMode: String? = "automatic",
    val coverValue: String? = "",
)

private data class LocalState(
    val playlists: List<LocalPlaylist>? = emptyList(),
    val likedIds: List<String>? = emptyList(),
)

class LocalStore(context: Context) {
    private val file = File(context.filesDir, "local_store.json")
    private val gson = Gson()
    private val lock = Any()

    @Volatile private var state: LocalState = load()

    private fun load(): LocalState = runCatching {
        if (!file.exists()) return LocalState()
        gson.fromJson(file.readText(), object : TypeToken<LocalState>() {}.type) ?: LocalState()
    }.getOrDefault(LocalState())

    private fun persist() {
        runCatching { file.writeText(gson.toJson(state)) }
    }

    fun playlists(): List<LocalPlaylist> = state.playlists.orEmpty()
    fun playlist(id: String): LocalPlaylist? = state.playlists.orEmpty().firstOrNull { it.id == id }

    fun createPlaylist(name: String): String = synchronized(lock) {
        val id = "local-pl-" + UUID.randomUUID().toString().take(8)
        state = state.copy(playlists = state.playlists.orEmpty() + LocalPlaylist(id, name, "", emptyList()))
        persist()
        id
    }

    fun updatePlaylist(id: String, name: String?, subtitle: String?) = synchronized(lock) {
        state = state.copy(playlists = state.playlists.orEmpty().map {
            if (it.id == id) it.copy(title = name ?: it.title, subtitle = subtitle ?: it.subtitle) else it
        })
        persist()
    }

    fun deletePlaylist(id: String) = synchronized(lock) {
        state = state.copy(playlists = state.playlists.orEmpty().filterNot { it.id == id })
        persist()
    }

    fun addTracks(id: String, trackIds: List<String>) = synchronized(lock) {
        state = state.copy(playlists = state.playlists.orEmpty().map {
            if (it.id == id) it.copy(trackIds = (it.trackIds.orEmpty() + trackIds).distinct()) else it
        })
        persist()
    }

    fun removeTracks(id: String, trackIds: List<String>) = synchronized(lock) {
        val drop = trackIds.toSet()
        state = state.copy(playlists = state.playlists.orEmpty().map {
            if (it.id == id) it.copy(trackIds = it.trackIds.orEmpty().filterNot { t -> t in drop }) else it
        })
        persist()
    }

    fun setPlaylistCover(
        id: String,
        mode: String,
        value: String?,
    ) = synchronized(lock) {
        state = state.copy(playlists = state.playlists.orEmpty().map {
            if (it.id == id) {
                it.copy(
                    coverMode = mode,
                    coverValue = value.orEmpty(),
                )
            } else {
                it
            }
        })
        persist()
    }

    fun reorderTracks(id: String, orderedTrackIds: List<String>) = synchronized(lock) {
        state = state.copy(playlists = state.playlists.orEmpty().map {
            if (it.id == id) {
                val existing = it.trackIds.orEmpty().toSet()
                val ordered = orderedTrackIds.filter { trackId -> trackId in existing }
                val missing = it.trackIds.orEmpty().filterNot { trackId -> trackId in ordered.toSet() }
                it.copy(trackIds = ordered + missing)
            } else {
                it
            }
        })
        persist()
    }

    fun exportJson(): String = gson.toJson(state)
    fun importJson(json: String) = synchronized(lock) {
        runCatching { gson.fromJson(json, object : TypeToken<LocalState>() {}.type) as? LocalState }.getOrNull()?.let {
            state = it; persist()
        }
    }

    fun likedIds(): Set<String> = state.likedIds.orEmpty().toSet()

    fun setLiked(id: String, liked: Boolean): Boolean = synchronized(lock) {
        val cur = state.likedIds.orEmpty()
        val next = if (liked) (cur + id).distinct() else cur.filterNot { it == id }
        state = state.copy(likedIds = next)
        persist()
        true
    }
}
