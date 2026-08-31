package com.mentality.sonethyst.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Podcasts
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Workspaces
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SidebarContent(
    username: String,
    server: String,
    isLocal: Boolean = false,
    avatarUrl: String = "",
    onProfile: () -> Unit,
    onSettings: () -> Unit,
    onLibrary: () -> Unit,
    onHistory: () -> Unit,
    onStats: () -> Unit,
    onDuplicates: () -> Unit,
    onRadio: () -> Unit = {},
    onPodcasts: () -> Unit = {},
    onClose: () -> Unit,
    onLogout: () -> Unit,
) {
    val initials =
        username.take(2).uppercase().ifBlank {
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.sidebar_initials_fallback
            )
        }
    val host =
        server
            .removePrefix("http://")
            .removePrefix("https://")
            .ifBlank {
                androidx.compose.ui.res.stringResource(
                    com.mentality.sonethyst.R.string.sidebar_view_profile
                )
            }
    Column(
        Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(16.dp))
        // Profile header
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).clickable { onClose(); onProfile() }.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(56.dp).clip(CircleShape)
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
                contentAlignment = Alignment.Center,
            ) {
                if (avatarUrl.isNotBlank()) {
                    Artwork(avatarUrl, MaterialTheme.colorScheme.primary, Modifier.matchParentSize(), corner = 28.dp)
                } else {
                    Text(initials, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    if (isLocal) {
                        androidx.compose.ui.res.stringResource(
                            com.mentality.sonethyst.R.string.accounts_local_library
                        )
                    } else username.ifBlank {
                        androidx.compose.ui.res.stringResource(
                            com.mentality.sonethyst.R.string.sidebar_listener
                        )
                    }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (isLocal) {
                        androidx.compose.ui.res.stringResource(
                            com.mentality.sonethyst.R.string.server_local
                        )
                    } else host,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.outline))
        Spacer(Modifier.height(8.dp))

        DrawerItem(
            Icons.Filled.Person,
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.sidebar_profile
            ),
        ) { onClose(); onProfile() }
        DrawerItem(
            Icons.AutoMirrored.Filled.QueueMusic,
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.sidebar_library
            ),
        ) { onClose(); onLibrary() }
        DrawerItem(
            Icons.Filled.Radio,
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.sidebar_radio
            ),
        ) { onClose(); onRadio() }
        DrawerItem(
            Icons.Filled.Podcasts,
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.sidebar_podcasts
            ),
        ) { onClose(); onPodcasts() }
        DrawerItem(
            Icons.Filled.History,
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.sidebar_history
            ),
        ) { onClose(); onHistory() }
        DrawerItem(
            Icons.Filled.Workspaces,
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.sidebar_stats
            ),
        ) { onClose(); onStats() }
        DrawerItem(
            Icons.Filled.ContentCopy,
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.sidebar_duplicates
            ),
        ) { onClose(); onDuplicates() }
        DrawerItem(
            Icons.Filled.Settings,
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.sidebar_settings
            ),
        ) { onClose(); onSettings() }

        Spacer(Modifier.weight(1f))

        DrawerItem(
            Icons.AutoMirrored.Filled.Logout,
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.sidebar_logout
            ), tint = MaterialTheme.colorScheme.error) { onClose(); onLogout() }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun DrawerItem(icon: ImageVector, label: String, tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(16.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = tint)
    }
}
