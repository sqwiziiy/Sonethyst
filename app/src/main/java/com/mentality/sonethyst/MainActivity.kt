package com.mentality.sonethyst

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mentality.sonethyst.data.UiPrefs
import com.mentality.sonethyst.ui.SonethystApp
import com.mentality.sonethyst.ui.theme.SonethystTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as SonethystApplication).container
        handleAuthRedirect(intent)
        setContent {
            val uiPrefs by container.settingsStore.uiPrefs.collectAsStateWithLifecycle(initialValue = UiPrefs())
            SonethystTheme(uiPrefs = uiPrefs) {
                SonethystApp()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthRedirect(intent)
    }

    /** Deliver the Spotify OAuth code from `sonethyst://spotify?code=...` to the auth flow. */
    private fun handleAuthRedirect(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "sonethyst" && data.host == "spotify") {
            data.getQueryParameter("code")?.takeIf { it.isNotBlank() }?.let {
                (application as SonethystApplication).container.emitSpotifyRedirect(it)
            }
        }
    }
}
