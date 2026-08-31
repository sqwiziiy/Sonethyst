package com.mentality.sonethyst.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.DataSaverOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import com.mentality.sonethyst.data.AlarmPrefs
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mentality.sonethyst.R
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.PlaybackPrefs
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val BITRATES = listOf(0, 128, 192, 256, 320)

@Composable
fun PlaybackSettingsScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = remember { (context.applicationContext as SonethystApplication).container }
    val store = container.settingsStore
    val isLocal = container.isLocal
    val prefs by store.playbackPrefs.collectAsStateWithLifecycle(initialValue = PlaybackPrefs())
    val dataSaver by store.dataSaver.collectAsStateWithLifecycle(initialValue = false)
    val alarm by store.alarmPrefs.collectAsStateWithLifecycle(initialValue = AlarmPrefs())
    val scope = rememberCoroutineScope()

    val bitrateLabels =
        listOf(
            stringResource(R.string.playback_lossless),
            "128",
            "192",
            "256",
            "320",
        )

    val alarmTime =
        formatTime(
            context,
            alarm.hour,
            alarm.minute,
        )

    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(
            stringResource(R.string.settings_playback_quality),
            onBack,
        )
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp)) {

            // streaming quality only applies to server backends
            if (!isLocal) {
                item { SettingsSectionTitle(stringResource(R.string.playback_streaming_quality)) }
                item {
                    val sel = BITRATES.indexOf(prefs.streamWifi).coerceAtLeast(0)
                    SegmentedRow(
                        stringResource(R.string.playback_wifi),
                        bitrateLabels,
                        sel,
                    ) { i -> scope.launch { store.setStreamWifi(BITRATES[i]) } }
                }
                item {
                    val sel = BITRATES.indexOf(prefs.streamCellular).coerceAtLeast(0)
                    SegmentedRow(
                        stringResource(R.string.playback_cellular),
                        bitrateLabels,
                        sel,
                    ) { i -> scope.launch { store.setStreamCellular(BITRATES[i]) } }
                }
                item {
                    SettingsGroup {
                        SettingsSwitchRow(
                            Icons.Filled.DataSaverOn,
                            stringResource(R.string.playback_data_saver),
                            stringResource(R.string.playback_data_saver_summary), dataSaver) { v -> scope.launch { store.setDataSaver(v) } }
                    }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.eq_output)) }
            item {
                SettingsGroup {
                    SettingsSwitchRow(
                        Icons.Filled.HighQuality,
                        stringResource(R.string.playback_hires),
                        stringResource(R.string.playback_hires_summary), prefs.preferHighRes) { v ->
                        scope.launch { store.setPreferHighRes(v) }
                    }
                    SettingsRowDivider()
                    SettingsSwitchRow(
                        Icons.Filled.Usb,
                        stringResource(R.string.playback_usb_bitperfect),
                        stringResource(R.string.playback_usb_bitperfect_summary),
                        prefs.bitPerfectUsb,
                    ) { v ->
                        scope.launch { store.setBitPerfectUsb(v) }
                        // claim usb up front so driver avoids the per-plug attach prompt
                        if (v) runCatching {
                            val dev = com.decent.usbaudio.UsbAudioDevice.getInstance(context)
                            dev.findUsbAudioDevice()?.let { if (!dev.hasPermission(it)) dev.requestPermission(it) {} }
                        }
                    }
                }
            }
            item { SignalPathCard() }

            item { SettingsSectionTitle(stringResource(R.string.playback_section)) }
            item {
                SettingsSliderRow(
                    stringResource(R.string.playback_crossfade),
                    if (prefs.crossfadeSec == 0) {
                        stringResource(R.string.eq_off)
                    } else {
                        stringResource(
                            R.string.playback_seconds,
                            prefs.crossfadeSec,
                        )
                    },
                    prefs.crossfadeSec.toFloat(), 0f..12f, steps = 11,
                ) { v -> scope.launch { store.setCrossfade(v.roundToInt()) } }
            }
            item {
                SettingsGroup {
                    SettingsSwitchRow(
                        Icons.Filled.Audiotrack,
                        stringResource(R.string.playback_gapless),
                        stringResource(R.string.playback_gapless_summary), prefs.gapless) { v -> scope.launch { store.setGapless(v) } }
                    SettingsRowDivider()
                    SettingsSwitchRow(
                        Icons.Filled.GraphicEq,
                        stringResource(R.string.playback_skip_silence),
                        stringResource(R.string.playback_skip_silence_summary), prefs.skipSilence) { v -> scope.launch { store.setSkipSilence(v) } }
                    SettingsRowDivider()
                    SettingsSwitchRow(
                        Icons.Filled.Headphones,
                        stringResource(R.string.playback_mono),
                        stringResource(R.string.playback_mono_summary), prefs.monoAudio) { v -> scope.launch { store.setMono(v) } }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.playback_default_speed)) }
            item {
                SettingsSliderRow(
                    stringResource(R.string.playback_speed), "${"%.2f".format(prefs.defaultSpeed)}x", prefs.defaultSpeed, 0.5f..2.0f, steps = 5) { v ->
                    scope.launch { store.setDefaultSpeed(v) }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.playback_wakeup_alarm)) }
            item {
                SettingsGroup {
                    SettingsSwitchRow(
                        Icons.Filled.Alarm,
                        stringResource(R.string.playback_wake_music),
                        stringResource(
                            R.string.playback_wake_music_summary,
                            alarmTime,
                        ),
                        alarm.enabled,
                    ) { v -> scope.launch { store.setAlarm(v, alarm.hour, alarm.minute) } }
                    SettingsRowDivider()
                    SettingsNavRow(
                        Icons.Filled.Alarm,
                        stringResource(R.string.playback_alarm_time),
                        value = alarmTime,
                    ) {
                        android.app.TimePickerDialog(
                            context, { _, h, m -> scope.launch { store.setAlarm(alarm.enabled, h, m) } },
                            alarm.hour, alarm.minute, false,
                        ).show()
                    }
                }
            }
        }
    }
}

private fun formatTime(
    context: android.content.Context,
    hour: Int,
    minute: Int,
): String {
    val calendar =
        java.util.Calendar.getInstance().apply {
            set(
                java.util.Calendar.HOUR_OF_DAY,
                hour,
            )
            set(
                java.util.Calendar.MINUTE,
                minute,
            )
            set(
                java.util.Calendar.SECOND,
                0,
            )
        }

    return android.text.format.DateFormat
        .getTimeFormat(context)
        .format(calendar.time)
}

@Composable
private fun SignalPathCard() {
    val ctx = LocalContext.current
    val container = remember { (ctx.applicationContext as SonethystApplication).container }
    val sp by container.signalPath.collectAsStateWithLifecycle()
    if (!sp.active) return
    val good = sp.bitPerfect
    val accent =
        if (good) {
            Color(0xFF28D572)
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }

    val stereoLabel =
        stringResource(R.string.playback_stereo)

    val monoLabel =
        stringResource(R.string.playback_mono_channel)

    val parts = buildList {
        if (sp.codec.isNotBlank()) add(sp.codec)
        if (sp.sampleRateHz > 0) add("%.1f kHz".format(sp.sampleRateHz / 1000f))
        if (sp.bitDepth > 0) add("${sp.bitDepth}-bit")
        if (sp.channels == 2) {
            add(stereoLabel)
        } else if (sp.channels == 1) {
            add(monoLabel)
        }
    }.joinToString(" · ")
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(if (good) Icons.Filled.Verified else Icons.Filled.GraphicEq, null, tint = accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(
                    if (good) {
                        R.string.playback_bitperfect
                    } else {
                        R.string.playback_signal_path
                    }
                ), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
        }
        Spacer(Modifier.height(6.dp))
        Text("$parts  →  ${sp.output}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
        if (sp.note.isNotBlank()) {
            Spacer(Modifier.height(2.dp))
            Text(sp.note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
