package com.mentality.sonethyst.util

import java.text.Normalizer
import java.util.Locale

object TrackMatch {
    /*
     * Same recording copies can differ slightly because containers /
     * servers round duration differently.
     *
     * Keep this deliberately small: remixes, live cuts, extended edits,
     * slowed versions, etc. belong to the next "versions/edits" feature,
     * not duplicate collapsing.
     */
    const val DURATION_TOLERANCE_SEC = 4

    private val featureSuffix =
        Regex(
            """\((?:feat|ft)\.?\s*[^)]*\)""",
            RegexOption.IGNORE_CASE,
        )

    private val nonAlphaNumeric =
        Regex("""[^\p{L}\p{N}]+""")

    fun norm(value: String): String =
        Normalizer.normalize(
            value,
            Normalizer.Form.NFKC,
        )
            .lowercase(Locale.ROOT)
            .replace(featureSuffix, " ")
            .replace(nonAlphaNumeric, " ")
            .trim()

    fun key(
        artist: String,
        title: String,
    ): String =
        "${norm(artist)}|${norm(title)}"
}
