package com.mentality.sonethyst.ui.screens.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.R
import com.mentality.sonethyst.ui.screens.settings.SettingsTopBar
import com.mentality.sonethyst.viewmodel.LyricsEditState
import com.mentality.sonethyst.ui.components.displayArtist
import com.mentality.sonethyst.ui.components.displayTitle

@Composable
fun LyricsEditScreen(
    contentPadding: PaddingValues,
    state: LyricsEditState,
    onText: (String) -> Unit,
    onSynced: (Boolean) -> Unit,
    onAdjustOffset: (Int) -> Unit,
    onResetOffset: () -> Unit,
    currentPositionMs: Long,
    onAdjustLine:
        (
            rawLineIndex: Int,
            deltaMs: Int,
        ) -> Unit,
    onSetLineNow:
        (
            rawLineIndex: Int,
            timeMs: Long,
        ) -> Unit,
    onSave: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(
        onBack = onBack
    )

    Column(
        Modifier.fillMaxSize()
    ) {
        SettingsTopBar(
            stringResource(
                R.string.player_edit_lyrics
            ),
            onBack,
        )

        if (state.loading) {
            Column(
                Modifier.fillMaxSize(),
                verticalArrangement =
                    Arrangement.Center,
            ) {
                CircularProgressIndicator(
                    Modifier.padding(24.dp)
                )
            }

            return
        }

        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 16.dp
                )
                .padding(
                    bottom =
                        contentPadding
                            .calculateBottomPadding() +
                            16.dp
                )
        ) {
            Text(
                displayTitle(state.title),
                style =
                    MaterialTheme
                        .typography
                        .titleLarge,
                fontWeight =
                    FontWeight.Bold,
            )

            Text(
                displayArtist(state.artist),
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )

            val displayedSource =
                when {
                    state.hasCustom ->
                        stringResource(
                            R.string.lyrics_edit_source_custom
                        )

                    state.source.isBlank() ->
                        stringResource(
                            R.string.lyrics_edit_source_none
                        )

                    else ->
                        state.source
                }

            Text(
                stringResource(
                    R.string.lyrics_edit_source,
                    displayedSource,
                ),
                style =
                    MaterialTheme
                        .typography
                        .labelMedium,
                color =
                    MaterialTheme
                        .colorScheme
                        .onSurfaceVariant,
            )

            Spacer(
                Modifier.height(12.dp)
            )

            Row(
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    )
            ) {
                FilterChip(
                    selected =
                        !state.synced,
                    onClick = {
                        onSynced(false)
                    },
                    label = {
                        Text(stringResource(R.string.lyrics_edit_plain_text))
                    },
                )

                FilterChip(
                    selected =
                        state.synced,
                    onClick = {
                        onSynced(true)
                    },
                    label = {
                        Text(stringResource(R.string.lyrics_edit_synced_lrc))
                    },
                )
            }

            if (state.synced) {
                Text(
                    stringResource(R.string.lyrics_edit_lrc_hint),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                )

                Spacer(
                    Modifier.height(8.dp)
                )

                val offsetValue =
                    if (state.offsetMs > 0) {
                        "+${state.offsetMs}"
                    } else {
                        state.offsetMs.toString()
                    }

                Text(
                    stringResource(
                        R.string.lyrics_edit_timing_offset,
                        offsetValue,
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .labelLarge,
                    fontWeight =
                        FontWeight.Bold,
                )

                Spacer(
                    Modifier.height(6.dp)
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(
                            6.dp
                        ),
                ) {
                    OutlinedButton(
                        onClick = {
                            onAdjustOffset(-500)
                        },
                        modifier =
                            Modifier.weight(1f),
                    ) {
                        Text("−500")
                    }

                    OutlinedButton(
                        onClick = {
                            onAdjustOffset(-100)
                        },
                        modifier =
                            Modifier.weight(1f),
                    ) {
                        Text("−100")
                    }

                    OutlinedButton(
                        onClick = {
                            onAdjustOffset(100)
                        },
                        modifier =
                            Modifier.weight(1f),
                    ) {
                        Text("+100")
                    }

                    OutlinedButton(
                        onClick = {
                            onAdjustOffset(500)
                        },
                        modifier =
                            Modifier.weight(1f),
                    ) {
                        Text("+500")
                    }
                }

                if (state.offsetMs != 0) {
                    OutlinedButton(
                        onClick =
                            onResetOffset,
                        modifier =
                            Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.lyrics_edit_reset_offset))
                    }
                }
            }

            Spacer(
                Modifier.height(8.dp)
            )

            OutlinedTextField(
                value = state.rawText,
                onValueChange = onText,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .weight(
                            if (
                                state.synced &&
                                state.timingLines
                                    .isNotEmpty()
                            ) {
                                0.45f
                            } else {
                                1f
                            }
                        ),
                label = {
                    Text(
                        if (state.synced) {
                            stringResource(R.string.lyrics_edit_lrc_lyrics)
                        } else {
                            stringResource(R.string.player_lyrics)
                        }
                    )
                },
            )

            if (
                state.synced &&
                state.timingLines.isNotEmpty()
            ) {
                Spacer(
                    Modifier.height(10.dp)
                )

                Text(
                    stringResource(R.string.lyrics_edit_line_timing),
                    style =
                        MaterialTheme
                            .typography
                            .titleMedium,
                    fontWeight =
                        FontWeight.Bold,
                )

                Text(
                    stringResource(R.string.lyrics_edit_now_help),
                    style =
                        MaterialTheme
                            .typography
                            .bodySmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                )

                Spacer(
                    Modifier.height(6.dp)
                )

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(0.55f),
                    verticalArrangement =
                        Arrangement.spacedBy(
                            8.dp
                        ),
                ) {
                    items(
                        count =
                            state.timingLines.size,
                        key = { index ->
                            val line =
                                state.timingLines[
                                    index
                                ]

                            "timing:${line.rawLineIndex}"
                        },
                    ) { index ->
                        val line =
                            state.timingLines[
                                index
                            ]

                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(
                                    vertical = 4.dp
                                )
                        ) {
                            Text(
                                formatLyricsTimestamp(
                                    line.timeMs
                                ),
                                style =
                                    MaterialTheme
                                        .typography
                                        .labelLarge,
                                fontWeight =
                                    FontWeight.Bold,
                            )

                            Text(
                                line.text.ifBlank {
                                    "♪"
                                },
                                maxLines = 2,
                                style =
                                    MaterialTheme
                                        .typography
                                        .bodyMedium,
                            )

                            Spacer(
                                Modifier.height(4.dp)
                            )

                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement =
                                    Arrangement
                                        .spacedBy(
                                            6.dp
                                        ),
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        onAdjustLine(
                                            line.rawLineIndex,
                                            -100,
                                        )
                                    },
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        ),
                                ) {
                                    Text("−100")
                                }

                                Button(
                                    onClick = {
                                        onSetLineNow(
                                            line.rawLineIndex,
                                            currentPositionMs,
                                        )
                                    },
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        ),
                                ) {
                                    Text(stringResource(R.string.lyrics_edit_now))
                                }

                                OutlinedButton(
                                    onClick = {
                                        onAdjustLine(
                                            line.rawLineIndex,
                                            100,
                                        )
                                    },
                                    modifier =
                                        Modifier.weight(
                                            1f
                                        ),
                                ) {
                                    Text("+100")
                                }
                            }
                        }
                    }
                }
            }

            Spacer(
                Modifier.height(12.dp)
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.spacedBy(
                        8.dp
                    ),
            ) {
                Button(
                    onClick = onSave,
                    enabled =
                        !state.saving,
                    modifier =
                        Modifier.weight(1f),
                ) {
                    Text(
                        if (state.saving) {
                            stringResource(R.string.lyrics_edit_saving)
                        } else {
                            stringResource(R.string.action_save)
                        }
                    )
                }

                if (state.hasCustom) {
                    OutlinedButton(
                        onClick = onClear,
                    ) {
                        Text(stringResource(R.string.action_reset))
                    }
                }
            }
        }
    }
}


private fun formatLyricsTimestamp(
    timeMs: Long,
): String {
    val safe =
        timeMs.coerceAtLeast(0L)

    val min =
        safe / 60_000L

    val sec =
        (safe % 60_000L) /
            1000L

    val ms =
        safe % 1000L

    return "%02d:%02d.%03d".format(
        min,
        sec,
        ms,
    )
}
