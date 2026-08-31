package com.mentality.sonethyst.ui.widget

import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.mentality.sonethyst.playback.NowPlaying
import com.mentality.sonethyst.playback.NowPlayingStore
import com.mentality.sonethyst.playback.PlaybackService
import com.mentality.sonethyst.ui.components.displayTitle

class SonethystTileService : TileService() {

    override fun onStartListening() = render()

    override fun onClick() {
        val np = NowPlayingStore.read(this)
        runCatching {
            val intent = Intent(this, PlaybackService::class.java).setAction(PlaybackService.ACTION_PLAY_PAUSE)
            ContextCompat.startForegroundService(this, intent)
        }
        // optimistic flip next render reconciles
        render(playingOverride = !np.isPlaying)
    }

    private fun render(playingOverride: Boolean? = null) {
        val tile = qsTile ?: return
        val np = NowPlayingStore.read(this)
        val playing = playingOverride ?: np.isPlaying
        tile.state = if (playing) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label =
            if (
                np.hasTrack &&
                np.title.isNotBlank()
            ) {
                this.displayTitle(np.title)
            } else {
                getString(
                    com.mentality.sonethyst.R.string.app_name
                )
            }
        if (Build.VERSION.SDK_INT >= 29) {
            tile.subtitle =
                if (np.hasTrack) {
                    getString(
                        if (playing) {
                            com.mentality.sonethyst.R.string.quick_tile_playing
                        } else {
                            com.mentality.sonethyst.R.string.quick_tile_paused
                        }
                    )
                } else {
                    getString(
                        com.mentality.sonethyst.R.string.app_name
                    )
                }
        }
        runCatching {
            tile.icon = Icon.createWithResource(
                this, if (playing) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
            )
        }
        tile.updateTile()
    }
}
