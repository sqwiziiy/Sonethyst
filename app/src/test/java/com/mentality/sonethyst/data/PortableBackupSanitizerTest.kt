package com.mentality.sonethyst.data

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PortableBackupSanitizerTest {
    @Test
    fun removesJellyfinAndSubsonicSecretsFromBackupPayloads() {
        val jellyfin = "https://music.example/Items/a/Images/Primary?quality=90&api_key=JELLYFIN_SECRET_123"
        val subsonic = "https://music.example/rest/getCoverArt.view?id=a&u=user&t=SUBSONIC_TOKEN_456&s=SUBSONIC_SALT_789&v=1.16.1"
        val payload = """{"pins":[{"coverUrl":"$jellyfin"}],"hidden":"$subsonic"}"""

        val backupJson = Gson().toJson(
            SonethystBackup(
                prefs = PrefsBackup(strings = mapOf("library_pins" to PortableBackupSanitizer.preferenceString(payload))),
                playHistory = listOf(
                    PlayEvent("id", "title", "artist", "album", "album", "artist", subsonic, 1, 1L)
                ).map(PortableBackupSanitizer::playEvent),
            )
        )

        assertFalse(backupJson.contains("JELLYFIN_SECRET_123"))
        assertFalse(backupJson.contains("SUBSONIC_TOKEN_456"))
        assertFalse(backupJson.contains("SUBSONIC_SALT_789"))
        assertEquals(
            "https://music.example/Items/a/Images/Primary?quality=90",
            PortableBackupSanitizer.artworkUrl(jellyfin),
        )
        assertEquals(
            "https://music.example/rest/getCoverArt.view?id=a&v=1.16.1",
            PortableBackupSanitizer.artworkUrl(subsonic),
        )
    }

    @Test
    fun preservesUnrelatedPublicQueryParameters() {
        val publicUrl = "https://cdn.example/art.jpg?size=600&t=public-style&s=small"
        assertEquals(publicUrl, PortableBackupSanitizer.artworkUrl(publicUrl))
    }
}
