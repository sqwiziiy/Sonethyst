package com.mentality.sonethyst.playback

import android.content.Context

// failures swallowed so a missing widget/tile can never affect playback
object WidgetBridge {
    fun refresh(context: Context) {
        runCatching { com.mentality.sonethyst.ui.widget.AuroraWidgetController.refresh(context) }
    }
}
