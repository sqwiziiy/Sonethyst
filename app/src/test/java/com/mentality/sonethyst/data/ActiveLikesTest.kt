package com.mentality.sonethyst.data

import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.viewmodel.likedCoverFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ActiveLikesTest {
    @Test
    fun allPersistedLikesAreActiveWhenFilesAreAvailable() {
        val persisted = setOf("A", "B")

        assertEquals(setOf("A", "B"), activeLikeIds(persisted, setOf("A", "B")))
        assertEquals(setOf("A", "B"), persisted)
    }

    @Test
    fun unavailableFilesAreExcludedWithoutMutatingPersistedLikes() {
        val persisted = setOf("A", "B")

        assertEquals(setOf("B"), activeLikeIds(persisted, setOf("B")))
        assertEquals(setOf("A", "B"), persisted)
    }

    @Test
    fun noAvailableFilesProducesNoActiveLikes() {
        val persisted = setOf("A", "B")

        assertTrue(activeLikeIds(persisted, emptySet()).isEmpty())
        assertEquals(setOf("A", "B"), persisted)
    }

    @Test
    fun restoredFilesReactivatePersistedLikes() {
        val persisted = setOf("A", "B")

        assertEquals(setOf("A", "B"), activeLikeIds(persisted, setOf("A", "B")))
    }

    @Test
    fun mergedSourcesExposeNoLikesWhenLocalAndServerSourcesAreEmpty() {
        val localActive = activeLikeIds(setOf("local-A"), emptySet())
        val navidromeActive = emptySet<String>()

        assertTrue((localActive + navidromeActive).isEmpty())
    }

    @Test
    fun likedCoverComesOnlyFromVisibleLikedSongs() {
        val ordinarySong = Song("ordinary", "Ordinary", "Artist", "Album", "ordinary-cover", 1)
        val likedSong = Song("liked", "Liked", "Artist", "Album", "liked-cover", 1)

        assertEquals("", likedCoverFor(emptyList()))
        assertEquals("liked-cover", likedCoverFor(listOf(likedSong)))
        assertEquals("liked-cover", likedCoverFor(listOf(likedSong, ordinarySong)))
    }
}
