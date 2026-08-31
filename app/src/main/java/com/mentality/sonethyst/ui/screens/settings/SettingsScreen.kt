package com.mentality.sonethyst.ui.screens.settings

import com.mentality.sonethyst.BuildConfig
import com.mentality.sonethyst.R
import com.mentality.sonethyst.AppLocale

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.MergeType
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.SwitchAccount
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun SettingsScreen(
    contentPadding: PaddingValues,
    username: String,
    server: String,
    isLocal: Boolean = false,
    onBack: () -> Unit,
    onOpenPlayback: () -> Unit,
    onOpenEq: () -> Unit,
    onOpenVisualizer: () -> Unit,
    onOpenSonic: () -> Unit,
    onOpenSources: () -> Unit,
    onOpenMusicFolders: () -> Unit,
    onOpenDownloads: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenGestures: () -> Unit,
    onOpenIntegrations: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenAccounts: () -> Unit,
    onOpenBackup: () -> Unit,
    onLogout: () -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val container = (context.applicationContext as com.mentality.sonethyst.SonethystApplication).container
    val session by container.settingsStore.session.collectAsStateWithLifecycle(initialValue = null)
    val downloads by container.downloadManager.downloads.collectAsStateWithLifecycle()
    val appLanguage by container.settingsStore.appLanguage.collectAsStateWithLifecycle(initialValue = AppLocale.SYSTEM)
    var showLanguagePicker by remember { mutableStateOf(false) }
    val languageScope = rememberCoroutineScope()
    val languageSummary = when (appLanguage) {
        AppLocale.ENGLISH -> stringResource(R.string.language_english)
        AppLocale.RUSSIAN -> stringResource(R.string.language_russian)
        AppLocale.UKRAINIAN -> stringResource(R.string.language_ukrainian)
        AppLocale.FRENCH -> stringResource(R.string.language_french)
        else -> stringResource(R.string.language_system_default)
    }
    val serverBadge =
        stringResource(
            when (session?.type) {
                com.mentality.sonethyst.data.ServerType.SPOTIFY ->
                    R.string.settings_badge_spotify

                com.mentality.sonethyst.data.ServerType.JELLYFIN ->
                    R.string.settings_badge_jellyfin

                com.mentality.sonethyst.data.ServerType.LOCAL ->
                    R.string.settings_badge_local

                else ->
                    R.string.settings_badge_navidrome
            }
        )

    val avatarFallback =
        stringResource(R.string.user_avatar_fallback)

    val listenerFallback =
        stringResource(R.string.home_listener)

    val downloadsSummary =
        pluralStringResource(
            R.plurals.settings_downloads_summary,
            downloads.size,
            downloads.size,
        )

    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(
            stringResource(R.string.settings_title),
            onBack,
        )
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable(onClick = onOpenProfile).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.size(56.dp).clip(CircleShape)
                            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary))),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (!session?.imageUrl.isNullOrBlank()) {
                            com.mentality.sonethyst.ui.components.Artwork(session!!.imageUrl, MaterialTheme.colorScheme.primary, Modifier.matchParentSize(), corner = 28.dp)
                        } else {
                            Text(username.take(2).uppercase().ifBlank {
                                avatarFallback
                            }, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isLocal) stringResource(R.string.accounts_local_library)
                            else username.ifBlank { listenerFallback },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(stringResource(R.string.settings_view_profile), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Box(Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary).padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(serverBadge, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_account)) }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        Icons.Filled.SwitchAccount,
                        stringResource(R.string.settings_servers_accounts),
                        stringResource(R.string.settings_servers_accounts_summary), onClick = onOpenAccounts)
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_audio)) }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        Icons.Filled.PlayCircle,
                        stringResource(R.string.settings_playback_quality),
                        stringResource(R.string.settings_playback_quality_summary), onClick = onOpenPlayback)
                    SettingsRowDivider()
                    SettingsNavRow(
                        Icons.Filled.Tune,
                        stringResource(R.string.settings_equalizer),
                        stringResource(R.string.settings_equalizer_summary), onClick = onOpenEq)
                    SettingsRowDivider()
                    SettingsNavRow(
                        Icons.Filled.GraphicEq,
                        stringResource(R.string.settings_visualizer),
                        stringResource(R.string.settings_visualizer_summary), onClick = onOpenVisualizer)
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_discovery)) }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        Icons.Filled.AutoAwesome,
                        stringResource(R.string.settings_sonic_discovery),
                        stringResource(R.string.settings_sonic_summary), onClick = onOpenSonic)
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_library)) }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        Icons.Filled.MergeType,
                        stringResource(R.string.settings_library_sources),
                        stringResource(R.string.settings_library_sources_summary),
                        onClick = onOpenSources,
                    )
                    SettingsRowDivider()
                    SettingsNavRow(
                        Icons.Filled.Folder,
                        stringResource(R.string.settings_music_folders),
                        stringResource(R.string.settings_music_folders_summary),
                        onClick = onOpenMusicFolders,
                    )
                    SettingsRowDivider()
                    SettingsNavRow(
                        Icons.Filled.Download,
                        stringResource(R.string.settings_downloads_storage),
                        downloadsSummary,
                        onClick = onOpenDownloads,
                    )
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_interface)) }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        Icons.Filled.Language,
                        stringResource(R.string.settings_language),
                        stringResource(R.string.settings_language_summary),
                        value = languageSummary,
                        onClick = { showLanguagePicker = true },
                    )
                    SettingsRowDivider()
                    SettingsNavRow(
                        Icons.Filled.Palette,
                        stringResource(R.string.settings_appearance),
                        stringResource(R.string.settings_appearance_summary), onClick = onOpenAppearance)
                    SettingsRowDivider()
                    SettingsNavRow(
                        Icons.Filled.TouchApp,
                        stringResource(R.string.settings_gestures),
                        stringResource(R.string.settings_gestures_summary), onClick = onOpenGestures)
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_connections)) }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        Icons.Filled.Extension,
                        stringResource(R.string.settings_integrations),
                        stringResource(R.string.settings_integrations_summary), onClick = onOpenIntegrations)
                    SettingsRowDivider()
                    SettingsNavRow(
                        Icons.Filled.Lock,
                        stringResource(R.string.settings_permissions),
                        stringResource(R.string.settings_permissions_summary), onClick = onOpenPermissions)
                    SettingsRowDivider()
                    SettingsNavRow(
                        Icons.Filled.Info,
                        stringResource(R.string.settings_about),
                        value = "v${BuildConfig.VERSION_NAME}", onClick = onOpenAbout)
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.settings_section_data)) }
            item {
                SettingsGroup {
                    SettingsNavRow(
                        Icons.Filled.Backup,
                        stringResource(R.string.settings_backup_restore),
                        stringResource(R.string.settings_backup_summary), onClick = onOpenBackup)
                }
            }

            item {
                Spacer(Modifier.height(20.dp))
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh).clickable(onClick = onLogout).padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_logout), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (showLanguagePicker) {
        val options = listOf(
            AppLocale.SYSTEM to stringResource(R.string.language_system_default),
            AppLocale.ENGLISH to stringResource(R.string.language_english),
            AppLocale.RUSSIAN to stringResource(R.string.language_russian),
            AppLocale.UKRAINIAN to stringResource(R.string.language_ukrainian),
            AppLocale.FRENCH to stringResource(R.string.language_french),
        )
        ModalBottomSheet(
            onDismissRequest = { showLanguagePicker = false },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text(
                    text = stringResource(R.string.settings_language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                )
                options.forEach { (code, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable {
                                showLanguagePicker = false
                                languageScope.launch {
                                    container.settingsStore.setAppLanguage(code)
                                    AppLocale.apply(context, code)
                                    (context as? android.app.Activity)?.recreate()
                                }
                            }
                            .padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = label,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        if (appLanguage == code) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
