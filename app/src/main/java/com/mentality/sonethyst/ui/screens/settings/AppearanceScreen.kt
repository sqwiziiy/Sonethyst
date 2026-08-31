package com.mentality.sonethyst.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
import com.mentality.sonethyst.data.AccentMode
import com.mentality.sonethyst.data.CornerStyle
import com.mentality.sonethyst.data.HomeSection
import com.mentality.sonethyst.data.MiniProgress
import com.mentality.sonethyst.data.MiniStyle
import com.mentality.sonethyst.data.SeekStyle
import com.mentality.sonethyst.data.ThemeMode
import com.mentality.sonethyst.data.UiPrefs
import com.mentality.sonethyst.ui.theme.AccentPresets
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AppearanceScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val container = (LocalContext.current.applicationContext as SonethystApplication).container
    val store = container.settingsStore
    val prefs by store.uiPrefs.collectAsStateWithLifecycle(initialValue = UiPrefs())
    val scope = rememberCoroutineScope()
    val materialYouSupported =
        android.os.Build.VERSION.SDK_INT >=
            android.os.Build.VERSION_CODES.S

    val themeOptions =
        listOf(
            stringResource(R.string.appearance_system),
            stringResource(R.string.appearance_light),
            stringResource(R.string.appearance_dark),
            stringResource(R.string.appearance_amoled),
        )

    val accentOptions =
        listOf(
            stringResource(R.string.appearance_presets),
            stringResource(R.string.appearance_custom),
            stringResource(R.string.appearance_material_you),
        )

    val cornerOptions =
        listOf(
            stringResource(R.string.appearance_corner_sharp),
            stringResource(R.string.appearance_corner_default),
            stringResource(R.string.appearance_corner_rounded),
            stringResource(R.string.appearance_corner_pill),
        )

    val seekOptions =
        listOf(
            stringResource(R.string.appearance_waveform),
            stringResource(R.string.appearance_bar),
        )

    val miniStyleOptions =
        listOf(
            stringResource(R.string.appearance_standard),
            stringResource(R.string.appearance_compact),
            stringResource(R.string.appearance_prominent),
        )

    val miniProgressOptions =
        listOf(
            stringResource(R.string.appearance_line),
            stringResource(R.string.appearance_bar),
            stringResource(R.string.appearance_none),
        )

    val homeSections =
        listOf(
            HomeSection.HERO to
                stringResource(R.string.appearance_home_hero),
            HomeSection.RECENT to
                stringResource(R.string.home_jump_back_in),
            HomeSection.PLAYLISTS to
                stringResource(R.string.home_your_playlists),
            HomeSection.FAVOURITE to
                stringResource(R.string.home_from_favourites),
            HomeSection.MOST to
                stringResource(R.string.home_most_played),
            HomeSection.ARTISTS to
                stringResource(R.string.home_artists),
            HomeSection.NEW to
                stringResource(R.string.home_new_releases),
        )

    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(
            stringResource(R.string.appearance_title),
            onBack,
        )
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp)) {

            item { SettingsSectionTitle(stringResource(R.string.appearance_theme)) }
            item {
                SegmentedRow(
                    stringResource(R.string.appearance_mode),
                    themeOptions,
                    prefs.themeMode,
                ) { i ->
                    scope.launch { store.setThemeMode(i) }
                }
            }
            item {
                Text(
                    when (prefs.themeMode) {
                        ThemeMode.AMOLED ->
                            stringResource(
                                R.string.appearance_amoled_description
                            )
                        ThemeMode.SYSTEM ->
                            stringResource(
                                R.string.appearance_system_description
                            )
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
            }

            item { SettingsSectionTitle(stringResource(R.string.appearance_accent)) }
            item {
                SegmentedRow(
                    stringResource(R.string.appearance_source),
                    accentOptions,
                    prefs.accentMode,
                ) { i ->
                    scope.launch { store.setAccentMode(i) }
                }
            }

            when (prefs.accentMode) {
                AccentMode.PRESET -> item {
                    AccentPresetGrid(selected = prefs.accentPreset) { i -> scope.launch { store.setAccentPreset(i) } }
                }
                AccentMode.CUSTOM -> item {
                    CustomColorPicker(initialArgb = prefs.accentColor.toInt()) { argb ->
                        scope.launch { store.setAccentColor(argb.toLong() and 0xFFFFFFFFL) }
                    }
                }
                else -> item {
                    Text(
                        if (materialYouSupported) {
                            stringResource(
                                R.string.appearance_material_you_active
                            )
                        } else {
                            stringResource(
                                R.string.appearance_material_you_unavailable
                            )
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.appearance_display)) }
            item {
                SettingsSliderRow(
                    stringResource(R.string.appearance_font_size), "${(prefs.fontScale * 100).roundToInt()}%", prefs.fontScale, 0.85f..1.3f) { v ->
                    scope.launch { store.setFontScale(v) }
                }
            }
            item {
                SegmentedRow(
                    stringResource(R.string.appearance_corners),
                    cornerOptions,
                    prefs.cornerStyle,
                ) { i ->
                    scope.launch { store.setCornerStyle(i) }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.appearance_player)) }
            item {
                SegmentedRow(
                    stringResource(R.string.appearance_seek_bar),
                    seekOptions,
                    prefs.playerSeekStyle,
                ) { i ->
                    scope.launch { store.setPlayerSeekStyle(i) }
                }
            }
            if (prefs.playerSeekStyle == SeekStyle.WAVEFORM) {
                item {
                    SettingsSliderRow(
                        stringResource(R.string.appearance_waveform_bars), "${prefs.playerWaveBars}", prefs.playerWaveBars.toFloat(), 24f..96f) { v ->
                        scope.launch { store.setPlayerWaveBars(v.roundToInt()) }
                    }
                }
            }
            item {
                SettingsSliderRow(
                    stringResource(R.string.appearance_artwork_size), "${(prefs.playerArtSize * 100).roundToInt()}%", prefs.playerArtSize, 0.6f..1f) { v ->
                    scope.launch { store.setPlayerArtSize(v) }
                }
            }
            item {
                SettingsSliderRow(
                    stringResource(R.string.appearance_gradient_intensity), "${(prefs.playerGradient * 100).roundToInt()}%", prefs.playerGradient, 0f..1.5f) { v ->
                    scope.launch { store.setPlayerGradient(v) }
                }
            }
            item {
                SettingsSwitchRow(
                    title =
                        stringResource(
                            R.string.appearance_bottom_utilities
                        ),
                    subtitle =
                        stringResource(
                            R.string.appearance_bottom_utilities_summary
                        ), checked = prefs.playerShowUtilities) { v ->
                    scope.launch { store.setPlayerShowUtilities(v) }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.appearance_miniplayer)) }
            item {
                SegmentedRow(
                    stringResource(R.string.appearance_style),
                    miniStyleOptions,
                    prefs.miniStyle,
                ) { i ->
                    scope.launch { store.setMiniStyle(i) }
                }
            }
            item {
                SegmentedRow(
                    stringResource(R.string.appearance_progress),
                    miniProgressOptions,
                    prefs.miniProgress,
                ) { i ->
                    scope.launch { store.setMiniProgress(i) }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.appearance_library)) }
            item {
                SegmentedRow(
                    stringResource(R.string.appearance_grid_columns),
                    listOf("2", "3", "4"), (prefs.libraryColumns - 2).coerceIn(0, 2)) { i ->
                    scope.launch { store.setLibraryColumns(i + 2) }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.appearance_home_sections)) }
            items(homeSections.size) { idx ->
                val (id, label) = homeSections[idx]
                SettingsSwitchRow(title = label, checked = id !in prefs.hiddenHomeSections) { v ->
                    scope.launch { store.setHomeSectionHidden(id, !v) }
                }
            }
        }
    }
}

@Composable
private fun AccentPresetGrid(selected: Int, onSelect: (Int) -> Unit) {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        AccentPresets.chunked(5).forEachIndexed { rowIdx, row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEachIndexed { colIdx, preset ->
                    val index = rowIdx * 5 + colIdx
                    val isSel = index == selected
                    Box(
                        Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(16.dp))
                            .background(preset.seed)
                            .then(if (isSel) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, RoundedCornerShape(16.dp)) else Modifier)
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSel) {
                            val on = if (preset.seed.luminanceApprox() > 0.5f) Color.Black else Color.White
                            Icon(Icons.Filled.Check,
                                stringResource(R.string.song_selected), tint = on, modifier = Modifier.size(22.dp))
                        }
                    }
                }
                // Pad short final row so swatches keep their width.
                repeat(5 - row.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun CustomColorPicker(initialArgb: Int, onChange: (Int) -> Unit) {
    val initialHsv = remember {
        FloatArray(3).also { android.graphics.Color.colorToHSV(initialArgb, it) }
    }
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var bri by remember { mutableFloatStateOf(initialHsv[2]) }

    fun push() = onChange(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, bri)))
    val preview = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, bri)))

    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(preview).border(2.dp, MaterialTheme.colorScheme.outline, CircleShape))
            Text(
                stringResource(R.string.appearance_live_preview), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SettingsSliderRow(
            stringResource(R.string.appearance_hue), "${hue.roundToInt()}°", hue, 0f..360f) { hue = it; push() }
        SettingsSliderRow(
            stringResource(R.string.appearance_saturation), "${(sat * 100).roundToInt()}%", sat, 0f..1f) { sat = it; push() }
        SettingsSliderRow(
            stringResource(R.string.appearance_brightness), "${(bri * 100).roundToInt()}%", bri, 0f..1f) { bri = it; push() }
    }
}

private fun Color.luminanceApprox(): Float = 0.299f * red + 0.587f * green + 0.114f * blue
