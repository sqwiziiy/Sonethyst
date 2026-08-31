package com.mentality.sonethyst.data.remote

import org.junit.Assert.assertEquals
import org.junit.Test

class MusicBrainzClientTest {
    private fun track(id: String, number: String) =
        MbTrack(number, "Track", MbTrackRecording(id))

    @Test
    fun firstTrackUsesMatchingRecording() {
        val result = MbRecording(id = "r1", title = "Track", releases = listOf(
            MbRelease(title = "Album", media = listOf(MbMedia(track = listOf(track("r1", "1"))))),
        )).toMatch()
        assertEquals("1", result?.trackNumber)
    }

    @Test
    fun laterTrackUsesItsOwnNumber() {
        val result = MbRecording(id = "r7", title = "Track", releases = listOf(
            MbRelease(media = listOf(MbMedia(track = listOf(track("r1", "1"), track("r7", "7"))))),
        )).toMatch()
        assertEquals("7", result?.trackNumber)
    }

    @Test
    fun multiDiscReleaseUsesMatchingMedium() {
        val result = MbRecording(id = "r9", title = "Track", releases = listOf(
            MbRelease(media = listOf(
                MbMedia(position = 1, track = listOf(track("r1", "1"))),
                MbMedia(position = 2, track = listOf(track("r9", "2"))),
            )),
        )).toMatch()
        assertEquals("2", result?.trackNumber)
    }

    @Test
    fun missingMatchingTrackLeavesNumberBlank() {
        val result = MbRecording(id = "missing", title = "Track", releases = listOf(
            MbRelease(media = listOf(MbMedia(track = listOf(track("r1", "1"))))),
        )).toMatch()
        assertEquals("", result?.trackNumber)
    }
}
