package com.mentality.sonethyst.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.Locale
import java.util.UUID

const val HIDDEN_ITEM_KEY_SEP = '\u0003'

data class HiddenLibraryItem(
    val key: String = "",
    val scope: String = "",
    val kind: String = "",
    val title: String = "",
    val subtitle: String = "",
    val artworkUrl: String = "",
)

// fields nullable-safe for gson forward compat
data class PlaylistFolder(
    val id: String = "",
    val name: String = "",
    val parentId: String = "",
)

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
    val ratings: Map<String, Int>? = emptyMap(),
    val customTags: Map<String, List<String>>? = emptyMap(),
    val hiddenItems: Map<String, HiddenLibraryItem>? = emptyMap(),
    val playlistFolders: List<PlaylistFolder>? = emptyList(),
    val playlistFolderAssignments: Map<String, String>? = emptyMap(),
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

    fun playlistFolders(): List<PlaylistFolder> =
        state.playlistFolders
            .orEmpty()
            .sortedWith(
                compareBy<PlaylistFolder> {
                    it.parentId
                }.thenBy {
                    it.name.lowercase(Locale.ROOT)
                }
            )

    fun playlistFolderId(
        playlistKey: String,
    ): String {
        val folderId =
            state.playlistFolderAssignments
                .orEmpty()[playlistKey]
                .orEmpty()

        if (folderId.isBlank()) {
            return ""
        }

        return if (
            state.playlistFolders
                .orEmpty()
                .any { it.id == folderId }
        ) {
            folderId
        } else {
            ""
        }
    }

    fun createPlaylistFolder(
        name: String,
        parentId: String = "",
    ): String? = synchronized(lock) {
        val safeName =
            name.trim()
                .take(80)

        if (safeName.isBlank()) {
            return@synchronized null
        }

        val folders =
            state.playlistFolders.orEmpty()

        if (
            parentId.isNotBlank() &&
            folders.none { it.id == parentId }
        ) {
            return@synchronized null
        }

        val duplicate =
            folders.any {
                it.parentId == parentId &&
                    it.name.equals(
                        safeName,
                        ignoreCase = true,
                    )
            }

        if (duplicate) {
            return@synchronized null
        }

        val id =
            "playlist-folder-" +
                UUID.randomUUID()
                    .toString()
                    .take(8)

        state =
            state.copy(
                playlistFolders =
                    folders +
                        PlaylistFolder(
                            id = id,
                            name = safeName,
                            parentId = parentId,
                        )
            )

        persist()
        id
    }

    fun renamePlaylistFolder(
        id: String,
        name: String,
    ): Boolean = synchronized(lock) {
        val safeName =
            name.trim()
                .take(80)

        val folders =
            state.playlistFolders.orEmpty()

        val target =
            folders.firstOrNull {
                it.id == id
            } ?: return@synchronized false

        if (safeName.isBlank()) {
            return@synchronized false
        }

        val duplicate =
            folders.any {
                it.id != id &&
                    it.parentId ==
                        target.parentId &&
                    it.name.equals(
                        safeName,
                        ignoreCase = true,
                    )
            }

        if (duplicate) {
            return@synchronized false
        }

        state =
            state.copy(
                playlistFolders =
                    folders.map {
                        if (it.id == id) {
                            it.copy(
                                name = safeName
                            )
                        } else {
                            it
                        }
                    }
            )

        persist()
        true
    }

    fun movePlaylistFolder(
        id: String,
        parentId: String,
    ): Boolean = synchronized(lock) {
        val folders =
            state.playlistFolders.orEmpty()

        if (
            id.isBlank() ||
            id == parentId ||
            folders.none { it.id == id } ||
            (
                parentId.isNotBlank() &&
                    folders.none {
                        it.id == parentId
                    }
            )
        ) {
            return@synchronized false
        }

        /*
         * Reject moving a folder into one of its descendants.
         */
        var cursor =
            parentId

        while (cursor.isNotBlank()) {
            if (cursor == id) {
                return@synchronized false
            }

            cursor =
                folders.firstOrNull {
                    it.id == cursor
                }?.parentId.orEmpty()
        }

        state =
            state.copy(
                playlistFolders =
                    folders.map {
                        if (it.id == id) {
                            it.copy(
                                parentId = parentId
                            )
                        } else {
                            it
                        }
                    }
            )

        persist()
        true
    }

    fun deletePlaylistFolder(
        id: String,
    ): Boolean = synchronized(lock) {
        val folders =
            state.playlistFolders.orEmpty()

        val target =
            folders.firstOrNull {
                it.id == id
            } ?: return@synchronized false

        /*
         * Deleting a folder never deletes playlists.
         *
         * Child folders and contained playlists move one level up.
         */
        val parentId =
            target.parentId

        val nextFolders =
            folders
                .filterNot {
                    it.id == id
                }
                .map {
                    if (it.parentId == id) {
                        it.copy(
                            parentId = parentId
                        )
                    } else {
                        it
                    }
                }

        val assignments =
            state.playlistFolderAssignments
                .orEmpty()
                .toMutableMap()

        assignments
            .filterValues {
                it == id
            }
            .keys
            .toList()
            .forEach { key ->
                if (parentId.isBlank()) {
                    assignments.remove(key)
                } else {
                    assignments[key] =
                        parentId
                }
            }

        state =
            state.copy(
                playlistFolders =
                    nextFolders,
                playlistFolderAssignments =
                    assignments,
            )

        persist()
        true
    }

    fun setPlaylistFolder(
        playlistKey: String,
        folderId: String,
    ): Boolean = synchronized(lock) {
        if (playlistKey.isBlank()) {
            return@synchronized false
        }

        if (
            folderId.isNotBlank() &&
            state.playlistFolders
                .orEmpty()
                .none {
                    it.id == folderId
                }
        ) {
            return@synchronized false
        }

        val assignments =
            state.playlistFolderAssignments
                .orEmpty()
                .toMutableMap()

        if (folderId.isBlank()) {
            assignments.remove(
                playlistKey
            )
        } else {
            assignments[playlistKey] =
                folderId
        }

        state =
            state.copy(
                playlistFolderAssignments =
                    assignments
            )

        persist()
        true
    }

    fun removePlaylistFolderAssignment(
        playlistKey: String,
    ) = synchronized(lock) {
        val assignments =
            state.playlistFolderAssignments
                .orEmpty()
                .toMutableMap()

        assignments.remove(
            playlistKey
        )

        state =
            state.copy(
                playlistFolderAssignments =
                    assignments
            )

        persist()
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

    fun hiddenItems(
        scopes: Set<String>,
    ): List<HiddenLibraryItem> {
        if (scopes.isEmpty()) {
            return emptyList()
        }

        return state.hiddenItems
            .orEmpty()
            .values
            .filter {
                it.scope in scopes
            }
            .sortedWith(
                compareBy<HiddenLibraryItem> {
                    it.kind
                }.thenBy {
                    it.title.lowercase(Locale.ROOT)
                }
            )
    }

    fun isHidden(
        key: String,
    ): Boolean =
        key in state.hiddenItems.orEmpty()

    fun setHidden(
        item: HiddenLibraryItem,
        hidden: Boolean,
    ): Boolean = synchronized(lock) {
        val items =
            state.hiddenItems
                .orEmpty()
                .toMutableMap()

        if (hidden) {
            items[item.key] = item
        } else {
            items.remove(item.key)
        }

        state = state.copy(
            hiddenItems = items
        )

        persist()
        true
    }

    fun removeHidden(
        key: String,
    ): Boolean = synchronized(lock) {
        val items =
            state.hiddenItems
                .orEmpty()
                .toMutableMap()

        items.remove(key)

        state = state.copy(
            hiddenItems = items
        )

        persist()
        true
    }

    fun customTags(key: String): List<String> =
        state.customTags
            .orEmpty()[key]
            .orEmpty()

    fun customTagAssignments(
        scopes: Set<String>,
    ): Map<String, List<String>> {
        if (scopes.isEmpty()) {
            return emptyMap()
        }

        return state.customTags
            .orEmpty()
            .filterKeys { key ->
                val separator =
                    key.indexOf(CUSTOM_TAG_KEY_SEP)

                separator > 0 &&
                    key.substring(
                        0,
                        separator,
                    ) in scopes
            }
    }

    fun setCustomTags(
        key: String,
        tags: Collection<String>,
    ): Boolean = synchronized(lock) {
        val normalized =
            tags
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .filter { it.length <= 64 }
                .distinctBy {
                    it.lowercase(Locale.ROOT)
                }
                .sortedBy {
                    it.lowercase(Locale.ROOT)
                }

        val all =
            state.customTags
                .orEmpty()
                .toMutableMap()

        if (normalized.isEmpty()) {
            all.remove(key)
        } else {
            all[key] = normalized
        }

        state = state.copy(
            customTags = all
        )
        persist()
        true
    }

    fun rating(id: String): Int =
        state.ratings
            .orEmpty()[id]
            ?.coerceIn(0, 5)
            ?: 0

    fun setRating(
        id: String,
        rating: Int,
    ): Boolean = synchronized(lock) {
        val safe = rating.coerceIn(0, 5)
        val ratings =
            state.ratings.orEmpty()
                .toMutableMap()

        if (safe == 0) {
            ratings.remove(id)
        } else {
            ratings[id] = safe
        }

        state = state.copy(ratings = ratings)
        persist()
        true
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
