package com.mentality.sonethyst.data

import com.google.gson.Gson

data class PrefsBackup(
    val strings: Map<String, String> = emptyMap(),
    val ints: Map<String, Int> = emptyMap(),
    val longs: Map<String, Long> = emptyMap(),
    val booleans: Map<String, Boolean> = emptyMap(),
    val floats: Map<String, Float> = emptyMap(),
    val stringSets: Map<String, List<String>> = emptyMap(),
)

data class SonethystBackup(
    val version: Int = 1,
    val createdAt: Long = 0L,
    val prefs: PrefsBackup = PrefsBackup(),
    val localStore: String = "",
    val playHistory: List<PlayEvent> = emptyList(),
)

class BackupManager(
    private val settingsStore: SettingsStore,
    private val localStore: LocalStore,
    private val playHistory: PlayHistoryStore,
) {
    private val gson = Gson()

    suspend fun export(nowMs: Long): String {
        val backup = SonethystBackup(
            version = 1,
            createdAt = nowMs,
            prefs = settingsStore.exportPrefs(),
            localStore = PortableBackupSanitizer.preferenceString(localStore.exportJson()),
            playHistory = PortableBackupSanitizer.playEvents(playHistory.snapshot()),
        )
        return gson.toJson(backup)
    }

    suspend fun import(json: String): Boolean {
        val backup = runCatching { gson.fromJson(json, SonethystBackup::class.java) }.getOrNull() ?: return false
        settingsStore.importPrefs(backup.prefs)
        if (backup.localStore.isNotBlank()) {
            localStore.importJson(PortableBackupSanitizer.preferenceString(backup.localStore))
        }
        playHistory.restore(PortableBackupSanitizer.playEvents(backup.playHistory))
        return true
    }
}
