package com.mentality.sonethyst.ui.screens.settings

import android.content.Intent
import android.net.Uri

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mentality.sonethyst.AuroraApplication
import com.mentality.sonethyst.BuildConfig
import com.mentality.sonethyst.data.ServerType

@Composable
fun AboutSettingsScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val container =
        (context.applicationContext as AuroraApplication).container
    val session by container.settingsStore.session.collectAsStateWithLifecycle(initialValue = null)

    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar("About Sonethyst", onBack)
        Column(
            Modifier.fillMaxWidth().padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(24.dp))
            Box(
                Modifier.size(88.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.GraphicEq, null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp)) }
            Spacer(Modifier.height(14.dp))
            val clientType = when (session?.type) {
                ServerType.JELLYFIN -> "Jellyfin"
                ServerType.SUBSONIC -> "Navidrome"
                ServerType.SPOTIFY -> "Spotify"
                ServerType.LOCAL -> "Local"
                null -> "Local"
            }
            val protocol = when (session?.type) {
                ServerType.JELLYFIN -> "Jellyfin"
                ServerType.SUBSONIC -> "Subsonic / OpenSubsonic"
                ServerType.SPOTIFY -> "Spotify Web API"
                ServerType.LOCAL -> "Local files"
                null -> "—"
            }

            Text("Sonethyst", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("Version ${BuildConfig.VERSION_NAME}  •  $clientType client", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))

            InfoRow("Connected server", session?.server?.removePrefix("http://")?.removePrefix("https://") ?: "—")
            InfoRow("Signed in as", session?.username ?: "—")
            InfoRow("Protocol", protocol)
            InfoRow("Client name", "Sonethyst")
            InfoRow("Playback engine", "AndroidX Media3 (ExoPlayer)")

            Spacer(Modifier.height(20.dp))

            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Filled.Code,
                    title = "GitHub",
                    subtitle = "sqwiziiy/Sonethyst",
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse(
                                        "https://github.com/sqwiziiy/Sonethyst"
                                    ),
                                )
                            )
                        }
                    },
                )
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Built with Jetpack Compose & Material 3.\nBased in part on Aurora by nessli420 (Apache 2.0).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
