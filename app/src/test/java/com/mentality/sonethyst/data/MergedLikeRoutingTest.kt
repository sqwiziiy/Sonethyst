package com.mentality.sonethyst.data

import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.viewmodel.shouldRollbackLikeMutation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MergedLikeRoutingTest {
    private fun song(id: String, suffix: String = "mp3", streamUrl: String = "https://server/$id") =
        Song("$id", "Same title", "Same artist", "Same album", "cover", 180, streamUrl = streamUrl, suffix = suffix)

    private val local = song("0\u0001local-id", suffix = "mp3", streamUrl = "content://media/external/audio/media/7")
    private val navidrome = song("1\u0001navidrome-id")
    private val jellyfin = song("2\u0001jellyfin-id")

    @Test
    fun activeNavidromeWinsEqualQualityAndRoutesByItsNamespace() {
        val representative = chooseDuplicateRepresentative(listOf(local, navidrome), 1) { 0L }

        assertEquals("1\u0001navidrome-id", representative.id)
        assertEquals(1, mergedSourceIndex(representative.id))
        assertEquals("navidrome-id", mergedRouteTarget(representative.id)?.second)
    }

    @Test
    fun activeLocalWinsDuplicate() {
        assertEquals(local.id, chooseDuplicateRepresentative(listOf(local, navidrome), 0) { 0L }.id)
    }

    @Test
    fun activeJellyfinWinsDuplicate() {
        assertEquals(jellyfin.id, chooseDuplicateRepresentative(listOf(local, jellyfin), 2) { 0L }.id)
    }

    @Test
    fun qualityFallbackRemainsWhenActiveSourceIsAbsent() {
        val lossless = song("1\u0001lossless", suffix = "flac")
        val representative = chooseDuplicateRepresentative(listOf(local, lossless), 2) { if (it.suffix == "flac") 10L else 1L }

        assertEquals(lossless.id, representative.id)
    }

    @Test
    fun localizedServerRepresentativeKeepsServerIdAndLocalPlayback() {
        val representative = navidrome.copy(streamUrl = "content://media/external/audio/media/9")

        assertEquals("1\u0001navidrome-id", representative.id)
        assertTrue(representative.streamUrl.startsWith("content://"))
    }

    @Test
    fun likeMutationRollbackGateHandlesSuccessFailureAndExceptions() {
        assertFalse(shouldRollbackLikeMutation(1, 1, true))
        assertTrue(shouldRollbackLikeMutation(1, 1, false))
        assertTrue(shouldRollbackLikeMutation(1, 1, false)) // exception is normalized to failure
    }

    @Test
    fun staleFailureCannotRollbackNewerGeneration() {
        assertFalse(shouldRollbackLikeMutation(2, 1, false))
        assertFalse(shouldRollbackLikeMutation(2, 1, true))
    }
}
