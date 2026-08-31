package com.mentality.sonethyst.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class ServerNormalizationTest {
    @Test
    fun bareJellyfinHostDefaultsToHttps() {
        assertEquals("https://music.example", JellyfinClient.normalizeServer("music.example/"))
    }

    @Test
    fun jellyfinPreservesExplicitSchemes() {
        assertEquals("http://music.example", JellyfinClient.normalizeServer("http://music.example"))
        assertEquals("https://music.example", JellyfinClient.normalizeServer("https://music.example"))
    }

    @Test
    fun bareSubsonicHostDefaultsToHttps() {
        assertEquals("https://music.example", SubsonicClient.normalizeServer("music.example/"))
    }

    @Test
    fun subsonicPreservesExplicitSchemes() {
        assertEquals("http://music.example", SubsonicClient.normalizeServer("http://music.example"))
        assertEquals("https://music.example", SubsonicClient.normalizeServer("https://music.example"))
    }
}
