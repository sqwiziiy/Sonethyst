package com.mentality.sonethyst.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagEditorUriTest {
    @Test
    fun numericLocalIdUsesTheAuthoritativeContentUri() {
        val uri = TagEditor.localContentUriStringFor("content://media/external/audio/media/42")

        assertEquals("content://media/external/audio/media/42", uri?.toString())
    }

    @Test
    fun wrappedMergedIdStillUsesTheAuthoritativeContentUri() {
        val wrappedSongId = "0|42"
        val uri = TagEditor.localContentUriStringFor("content://media/external/audio/media/42")

        // The wrapped ID is deliberately not passed to URI resolution.
        assertEquals("0|42", wrappedSongId)
        assertEquals("content://media/external/audio/media/42", uri?.toString())
    }

    @Test
    fun remoteStreamHasNoLocalMediaStorePath() {
        assertNull(TagEditor.localContentUriStringFor("https://server.example/rest/stream?id=42"))
    }

    @Test
    fun blankOrMalformedContentUriFailsSafely() {
        assertNull(TagEditor.localContentUriStringFor(""))
        assertNull(TagEditor.localContentUriStringFor("content://"))
        assertNull(TagEditor.localContentUriStringFor("content://media"))
        assertNull(TagEditor.localContentUriStringFor("not a uri"))
    }
}
