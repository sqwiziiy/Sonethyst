package com.mentality.sonethyst.viewmodel

import com.mentality.sonethyst.data.activeLikeIds
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LikeRefreshTest {
    @Test
    fun successfulSongStarEmitsTargetedRefresh() {
        assertTrue(shouldEmitLikesChanged("song", true))
    }

    @Test
    fun successfulSongUnstarEmitsTargetedRefresh() {
        assertTrue(shouldEmitLikesChanged("song", true))
    }

    @Test
    fun failedMutationDoesNotEmitTargetedRefresh() {
        assertFalse(shouldEmitLikesChanged("song", false))
        assertFalse(shouldEmitLikesChanged("album", false))
    }

    @Test
    fun playlistMutationKeepsExistingLocalBehavior() {
        assertFalse(shouldEmitLikesChanged("playlist", true))
    }

    @Test
    fun staleLocalLikesRemainPersistedButDoNotEnterSummary() {
        val persisted = setOf("A", "B")

        assertTrue(activeLikeIds(persisted, setOf("B")) == setOf("B"))
        assertTrue(activeLikeIds(persisted, emptySet()).isEmpty())
    }
}
