package com.mentality.sonethyst.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mentality.sonethyst.R
import com.mentality.sonethyst.SonethystApplication
import kotlinx.coroutines.launch

@Composable
fun SonicSettingsScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val app = remember { (ctx.applicationContext as SonethystApplication).container }
    val progress by app.sonicEngine.progress.collectAsStateWithLifecycle()
    val analyzed by app.sonicEngine.analyzedCount.collectAsStateWithLifecycle()
    val auto by app.settingsStore.sonicAutoAnalyze.collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(
            stringResource(R.string.sonic_title),
            onBack,
        )
        Column(Modifier.fillMaxWidth().padding(bottom = contentPadding.calculateBottomPadding() + 24.dp)) {

            Row(
                Modifier.fillMaxWidth().padding(16.dp).clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        pluralStringResource(
                            R.plurals.sonic_tracks_analyzed,
                            analyzed,
                            analyzed,
                        ), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        stringResource(R.string.sonic_powers_radio), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            SettingsSectionTitle(
                stringResource(R.string.sonic_analyze)
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(enabled = !progress.running) { app.sonicEngine.scan() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (progress.running) {
                    CircularProgressIndicator(modifier = Modifier.width(22.dp).height(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Filled.Radio, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (progress.running) {
                            stringResource(
                                R.string.sonic_analyzing_library
                            )
                        } else {
                            stringResource(
                                R.string.sonic_analyze_library
                            )
                        },
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium,
                    )
                    Text(
                        when {
                            progress.running -> "${progress.done} / ${progress.total} • ${progress.current}"
                            analyzed > 0 ->
                                pluralStringResource(
                                    R.plurals.sonic_analyzed_rescan,
                                    analyzed,
                                    analyzed,
                                )
                            else ->
                                stringResource(
                                    R.string.sonic_extract_features
                                )
                        },
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
                if (progress.running) {
                    Text(
                        stringResource(R.string.action_cancel), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clip(RoundedCornerShape(50)).clickable { app.sonicEngine.cancel() }.padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }

            SettingsSectionTitle(
                stringResource(R.string.sonic_automation)
            )
            SettingsGroup {
                SettingsSwitchRow(
                    Icons.Filled.AutoAwesome,
                    stringResource(R.string.sonic_auto_analyze),
                    stringResource(
                        R.string.sonic_auto_analyze_summary
                    ), auto,
                ) { v -> scope.launch { app.settingsStore.setSonicAutoAnalyze(v) } }
            }

            Text(
                stringResource(R.string.sonic_description),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            )
        }
    }
}
