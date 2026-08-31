package com.mentality.sonethyst.ui.screens.library

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.R
import com.mentality.sonethyst.data.SmartPlaylist
import com.mentality.sonethyst.data.SmartRule
import com.mentality.sonethyst.data.resolveSmartPlaylistCover
import com.mentality.sonethyst.model.Song
import com.mentality.sonethyst.ui.components.Artwork
import com.mentality.sonethyst.ui.components.PlaylistCoverSheet
import com.mentality.sonethyst.util.accentFor
import com.mentality.sonethyst.ui.screens.settings.SegmentedRow
import com.mentality.sonethyst.ui.screens.settings.SettingsGroup
import com.mentality.sonethyst.ui.screens.settings.SettingsTopBar

// keys must match SmartPlaylistEngine
private const val TYPE_TEXT = 0
private const val TYPE_NUMBER = 1
private const val TYPE_BOOL = 2

private data class FieldSpec(
    val key: String,
    val labelRes: Int,
    val type: Int,
)

private val FIELDS =
    listOf(
        FieldSpec("title", R.string.smart_field_title, TYPE_TEXT),
        FieldSpec("artist", R.string.smart_field_artist, TYPE_TEXT),
        FieldSpec("album", R.string.smart_field_album, TYPE_TEXT),
        FieldSpec("format", R.string.smart_field_format, TYPE_TEXT),
        FieldSpec("duration", R.string.smart_field_duration, TYPE_NUMBER),
        FieldSpec("bitrate", R.string.smart_field_bitrate, TYPE_NUMBER),
        FieldSpec("playCount", R.string.smart_field_play_count, TYPE_NUMBER),
        FieldSpec(
            "lastPlayedDays",
            R.string.smart_field_last_played_days,
            TYPE_NUMBER,
        ),
        FieldSpec("liked", R.string.smart_field_liked, TYPE_BOOL),
        FieldSpec(
            "downloaded",
            R.string.smart_field_downloaded,
            TYPE_BOOL,
        ),
    )

private val TEXT_OPS =
    listOf(
        "contains" to R.string.smart_op_contains,
        "notContains" to R.string.smart_op_not_contains,
        "is" to R.string.smart_op_is,
        "isNot" to R.string.smart_op_is_not,
        "startsWith" to R.string.smart_op_starts_with,
    )

private val NUM_OPS =
    listOf(
        "gt" to R.string.smart_op_more_than,
        "lt" to R.string.smart_op_less_than,
        "eq" to R.string.smart_op_exactly,
    )

private val BOOL_OPS =
    listOf(
        "isTrue" to R.string.smart_op_yes,
        "isFalse" to R.string.smart_op_no,
    )

private val SORTS =
    listOf(
        "title" to R.string.smart_sort_title,
        "artist" to R.string.smart_sort_artist,
        "album" to R.string.smart_sort_album,
        "duration" to R.string.smart_sort_duration,
        "playCount" to R.string.smart_sort_play_count,
        "lastPlayed" to R.string.smart_sort_last_played,
        "random" to R.string.smart_sort_random,
    )

private fun fieldSpec(key: String?): FieldSpec = FIELDS.firstOrNull { it.key == key } ?: FIELDS.first()
private fun opsFor(type: Int) = when (type) { TYPE_NUMBER -> NUM_OPS; TYPE_BOOL -> BOOL_OPS; else -> TEXT_OPS }

@Composable
fun SmartPlaylistEditScreen(
    contentPadding: PaddingValues,
    playlist: SmartPlaylist,
    previewTracks: List<Song>,
    isNew: Boolean,
    onUpdate: ((SmartPlaylist) -> SmartPlaylist) -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
) {
    var showCoverSheet by
        remember {
            mutableStateOf(false)
        }

    val context =
        LocalContext.current

    val customCoverLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            if (uri != null) {
                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                }

                onUpdate {
                    it.copy(
                        coverMode = "custom",
                        coverValue =
                            uri.toString(),
                    )
                }

                showCoverSheet = false
            }
        }

    val coverMode =
        playlist.coverMode
            .orEmpty()
            .ifBlank {
                "automatic"
            }
            .lowercase()

    val coverUrl =
        resolveSmartPlaylistCover(
            playlist,
            previewTracks,
        )

    val coverLabel =
        stringResource(
            when (coverMode) {
                "first" ->
                    R.string.cover_first_track

                "collage" ->
                    R.string.cover_collage

                "track" ->
                    R.string.cover_track_artwork

                "custom" ->
                    R.string.cover_device_image

                else ->
                    R.string.cover_automatic
            }
        )

    Column(Modifier.fillMaxSize()) {
        SettingsTopBar(
            title =
                stringResource(
                    if (isNew) {
                        R.string.smart_new
                    } else {
                        R.string.smart_edit
                    }
                ),
            onBack = onBack,
        )
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            OutlinedTextField(
                value = playlist.name.orEmpty(),
                onValueChange = { v -> onUpdate { it.copy(name = v) } },
                label = { Text(stringResource(R.string.detail_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Spacer(Modifier.height(14.dp))

            SettingsGroup {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable {
                            showCoverSheet = true
                        }
                        .padding(
                            horizontal = 20.dp,
                            vertical = 12.dp,
                        ),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    Artwork(
                        url = coverUrl,
                        accent =
                            accentFor(
                                playlist.id
                                    ?: "smart"
                            ),
                        modifier =
                            Modifier.size(58.dp),
                        corner = 12.dp,
                    )

                    Spacer(
                        Modifier.width(14.dp)
                    )

                    Column(
                        Modifier.weight(1f)
                    ) {
                        Text(
                            stringResource(R.string.smart_playlist_cover),
                            style =
                                MaterialTheme
                                    .typography
                                    .titleSmall,
                            fontWeight =
                                FontWeight.Medium,
                        )

                        Text(
                            coverLabel,
                            style =
                                MaterialTheme
                                    .typography
                                    .bodySmall,
                            color =
                                MaterialTheme
                                    .colorScheme
                                    .onSurfaceVariant,
                        )
                    }

                    Icon(
                        Icons.Filled.Image,
                        stringResource(R.string.smart_change_cover),
                        tint =
                            MaterialTheme
                                .colorScheme
                                .primary,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            SettingsGroup {
                SegmentedRow(
                    title = stringResource(R.string.smart_match),
                    options =
                        listOf(
                            stringResource(R.string.smart_all_rules),
                            stringResource(R.string.smart_any_rule),
                        ),
                    selected = if (playlist.matchAll != false) 0 else 1,
                    onSelect = { i -> onUpdate { it.copy(matchAll = i == 0) } },
                )
            }
            Spacer(Modifier.height(14.dp))

            Text(stringResource(R.string.smart_rules), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(Modifier.height(6.dp))
            val rules = playlist.rules.orEmpty()
            rules.forEachIndexed { i, rule ->
                RuleRow(
                    rule = rule,
                    onChange = { r -> onUpdate { it.copy(rules = rules.toMutableList().apply { set(i, r) }) } },
                    onRemove = { onUpdate { it.copy(rules = rules.toMutableList().apply { removeAt(i) }) } },
                )
            }
            TextButton(onClick = { onUpdate { it.copy(rules = rules + SmartRule()) } }, modifier = Modifier.padding(horizontal = 12.dp)) {
                Icon(Icons.Filled.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.smart_add_rule))
            }
            Spacer(Modifier.height(14.dp))

            SettingsGroup {
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.smart_sort_by), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    Dropdown(
                        options =
                            SORTS.map {
                                stringResource(it.second)
                            },
                        selected = SORTS.indexOfFirst { it.first == (playlist.sortBy ?: "title") }.coerceAtLeast(0),
                        onSelect = { i -> onUpdate { it.copy(sortBy = SORTS[i].first) } },
                    )
                    Spacer(Modifier.width(8.dp))
                    val desc = playlist.descending == true
                    Icon(
                        if (desc) Icons.Filled.ArrowDownward else Icons.Filled.ArrowUpward,
                        if (desc) {
                            stringResource(R.string.smart_descending)
                        } else {
                            stringResource(R.string.smart_ascending)
                        },
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .clickable { onUpdate { it.copy(descending = !desc) } }.padding(7.dp),
                    )
                }
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.smart_limit), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                    OutlinedTextField(
                        value = (playlist.limit ?: 0).takeIf { it > 0 }?.toString() ?: "",
                        onValueChange = { v -> onUpdate { it.copy(limit = v.filter { c -> c.isDigit() }.toIntOrNull() ?: 0) } },
                        placeholder = { Text("0") },
                        singleLine = true,
                        modifier = Modifier.width(90.dp),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            Button(
                onClick = onSave,
                enabled = !playlist.name.isNullOrBlank(),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) { Text(
                stringResource(
                    if (isNew) {
                        R.string.smart_create
                    } else {
                        R.string.smart_save_changes
                    }
                ), fontWeight = FontWeight.Bold) }
        }
    }

    if (showCoverSheet) {
        PlaylistCoverSheet(
            tracks = previewTracks,
            currentMode = coverMode,
            onAutomatic = {
                onUpdate {
                    it.copy(
                        coverMode =
                            "automatic",
                        coverValue = "",
                    )
                }

                showCoverSheet = false
            },
            onFirstTrack = {
                onUpdate {
                    it.copy(
                        coverMode = "first",
                        coverValue = "",
                    )
                }

                showCoverSheet = false
            },
            onCollage = {
                onUpdate {
                    it.copy(
                        coverMode = "collage",
                        coverValue = "",
                    )
                }

                showCoverSheet = false
            },
            onTrack = { song ->
                onUpdate {
                    it.copy(
                        coverMode = "track",
                        coverValue =
                            song.id,
                    )
                }

                showCoverSheet = false
            },
            onChooseImage = {
                customCoverLauncher.launch(
                    arrayOf("image/*")
                )
            },
            onDismiss = {
                showCoverSheet = false
            },
        )
    }
}

@Composable
private fun RuleRow(rule: SmartRule, onChange: (SmartRule) -> Unit, onRemove: () -> Unit) {
    val spec = fieldSpec(rule.field)
    val ops = opsFor(spec.type)
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.45f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Dropdown(
                options =
                    FIELDS.map {
                        stringResource(it.labelRes)
                    },
                selected = FIELDS.indexOfFirst { it.key == spec.key }.coerceAtLeast(0),
                onSelect = { i ->
                    val f = FIELDS[i]
                    // reset op and bool value when field type changes
                    val op = if (opsFor(f.type).any { it.first == rule.op }) rule.op else opsFor(f.type).first().first
                    onChange(rule.copy(field = f.key, op = op, value = if (f.type == TYPE_BOOL) "" else rule.value))
                },
                modifier = Modifier.weight(1f),
            )
            Dropdown(
                options =
                    ops.map {
                        stringResource(it.second)
                    },
                selected = ops.indexOfFirst { it.first == rule.op }.coerceAtLeast(0),
                onSelect = { i -> onChange(rule.copy(op = ops[i].first)) },
            )
            Icon(
                Icons.Filled.Close, stringResource(R.string.smart_remove_rule),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp).clip(CircleShape).clickable(onClick = onRemove).padding(6.dp),
            )
        }
        if (spec.type != TYPE_BOOL) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = rule.value.orEmpty(),
                onValueChange = { v -> onChange(rule.copy(value = if (spec.type == TYPE_NUMBER) v.filter { it.isDigit() } else v)) },
                placeholder = {
                    Text(
                        stringResource(
                            if (spec.type == TYPE_NUMBER) {
                                R.string.smart_number_placeholder
                            } else {
                                R.string.smart_text_placeholder
                            }
                        )
                    )
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Dropdown(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    Box(modifier) {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                options.getOrElse(selected) { options.first() },
                style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Medium,
                maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false),
            )
            Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(20.dp))
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            options.forEachIndexed { i, opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onSelect(i); open = false })
            }
        }
    }
}
