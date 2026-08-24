package com.mentality.sonethyst.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mentality.sonethyst.SonethystApplication
import kotlinx.coroutines.launch

@Composable
fun MusicFoldersSettingsScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val container =
        (context.applicationContext as SonethystApplication).container

    val excludedFolders by
        container.settingsStore.localExcludedFolders
            .collectAsStateWithLifecycle(initialValue = emptySet())

    val libraryReload by
        container.libraryReload.collectAsStateWithLifecycle()

    val scope = rememberCoroutineScope()

    var folders by remember {
        mutableStateOf<List<String>>(emptyList())
    }

    var rescanning by remember {
        mutableStateOf(false)
    }

    fun normalize(path: String): String =
        path.replace('\\', '/')
            .trim()
            .trimEnd('/')

    val normalizedExcluded = remember(excludedFolders) {
        excludedFolders
            .map(::normalize)
            .filter { it.isNotBlank() }
            .toSet()
    }

    LaunchedEffect(libraryReload) {
        runCatching {
            container.localLibrary.ensureLoaded()
        }

        folders =
            container.localLibrary
                .detectedMusicFolders()
                .distinct()
                .sortedBy { it.lowercase() }
    }

    Column(
        Modifier.fillMaxWidth(),
    ) {
        SettingsTopBar(
            title = "Music folders",
            onBack = onBack,
        )

        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(
                bottom =
                    contentPadding.calculateBottomPadding() +
                        24.dp,
            ),
        ) {
            item(
                key = "description",
                contentType = "description",
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                ) {
                    Text(
                        "Choose which folders appear in Sonethyst.",
                        style = MaterialTheme.typography.bodyMedium,
                    )

                    Text(
                        "Excluded folders and their subfolders are hidden only from Sonethyst. Your actual music files are never deleted or modified.",
                        style = MaterialTheme.typography.bodySmall,
                        color =
                            MaterialTheme.colorScheme
                                .onSurfaceVariant,
                        modifier =
                            Modifier.padding(top = 4.dp),
                    )
                }
            }

            item(
                key = "actions",
                contentType = "actions",
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    horizontalArrangement =
                        Arrangement.spacedBy(10.dp),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    Button(
                        enabled = !rescanning,
                        onClick = {
                            scope.launch {
                                rescanning = true

                                runCatching {
                                    container.refreshLocalLibrary()
                                }

                                folders =
                                    container.localLibrary
                                        .detectedMusicFolders()
                                        .distinct()
                                        .sortedBy {
                                            it.lowercase()
                                        }

                                rescanning = false
                            }
                        },
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            null,
                            modifier = Modifier.size(18.dp),
                        )

                        Text(
                            if (rescanning) {
                                "Scanning…"
                            } else {
                                "Rescan"
                            },
                            modifier =
                                Modifier.padding(start = 8.dp),
                        )
                    }

                    if (normalizedExcluded.isNotEmpty()) {
                        OutlinedButton(
                            enabled = !rescanning,
                            onClick = {
                                scope.launch {
                                    container.settingsStore
                                        .clearLocalFolderExclusions()
                                }
                            },
                        ) {
                            Text("Include all")
                        }
                    }
                }
            }

            item(
                key = "section-title",
                contentType = "section-title",
            ) {
                SettingsSectionTitle(
                    "Detected folders"
                )
            }

            if (folders.isEmpty()) {
                item(
                    key = "empty",
                    contentType = "empty",
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment =
                            Alignment.Center,
                    ) {
                        Text(
                            "No local music folders detected",
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant,
                        )
                    }
                }
            } else {
                items(
                    items = folders,
                    key = { path -> path },
                    contentType = {
                        "music-folder"
                    },
                ) { rawPath ->
                    val path = normalize(rawPath)

                    val directlyExcluded =
                        path in normalizedExcluded

                    val excludedByParent =
                        normalizedExcluded
                            .firstOrNull { parent ->
                                parent != path &&
                                    path.startsWith(
                                        "$parent/"
                                    )
                            }

                    val effectivelyExcluded =
                        directlyExcluded ||
                            excludedByParent != null

                    val folderName =
                        path.substringAfterLast('/')
                            .ifBlank { path }

                    ListItem(
                        headlineContent = {
                            Text(
                                folderName,
                                fontWeight =
                                    FontWeight.Medium,
                                maxLines = 1,
                                overflow =
                                    TextOverflow.Ellipsis,
                            )
                        },
                        supportingContent = {
                            Column {
                                Text(
                                    path,
                                    maxLines = 2,
                                    overflow =
                                        TextOverflow.Ellipsis,
                                )

                                Text(
                                    when {
                                        excludedByParent != null ->
                                            "Excluded by parent folder"

                                        directlyExcluded ->
                                            "Excluded"

                                        else ->
                                            "Included"
                                    },
                                    color =
                                        if (
                                            effectivelyExcluded
                                        ) {
                                            MaterialTheme
                                                .colorScheme
                                                .error
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        },
                                    style =
                                        MaterialTheme
                                            .typography
                                            .labelMedium,
                                    modifier =
                                        Modifier.padding(
                                            top = 3.dp
                                        ),
                                )
                            }
                        },
                        leadingContent = {
                            Box(
                                Modifier
                                    .size(42.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            12.dp
                                        )
                                    )
                                    .background(
                                        MaterialTheme
                                            .colorScheme
                                            .surfaceContainerHigh
                                    ),
                                contentAlignment =
                                    Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.Folder,
                                    null,
                                    tint =
                                        if (
                                            effectivelyExcluded
                                        ) {
                                            MaterialTheme
                                                .colorScheme
                                                .onSurfaceVariant
                                        } else {
                                            MaterialTheme
                                                .colorScheme
                                                .primary
                                        },
                                )
                            }
                        },
                        trailingContent = {
                            Switch(
                                checked =
                                    !effectivelyExcluded,
                                enabled =
                                    excludedByParent == null &&
                                        !rescanning,
                                onCheckedChange = {
                                        included ->
                                    scope.launch {
                                        container.settingsStore
                                            .setLocalFolderExcluded(
                                                path = path,
                                                excluded =
                                                    !included,
                                            )
                                    }
                                },
                                colors =
                                    SwitchDefaults.colors(
                                        checkedTrackColor =
                                            MaterialTheme
                                                .colorScheme
                                                .primary,
                                    ),
                            )
                        },
                        colors =
                            ListItemDefaults.colors(
                                containerColor =
                                    Color.Transparent,
                            ),
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 12.dp,
                                    vertical = 2.dp,
                                )
                                .clip(
                                    RoundedCornerShape(18.dp)
                                )
                                .background(
                                    MaterialTheme
                                        .colorScheme
                                        .surfaceContainerHigh
                                        .copy(alpha = 0.5f)
                                ),
                    )
                }
            }
        }
    }
}
