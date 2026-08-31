package com.mentality.sonethyst.ui.screens.detail

import android.app.Activity
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.ui.screens.settings.SettingsTopBar
import com.mentality.sonethyst.viewmodel.BatchTagEditState
import com.mentality.sonethyst.viewmodel.BatchTagField
import com.mentality.sonethyst.viewmodel.BatchTagFieldState

@Composable
fun BatchTagEditScreen(
    contentPadding: PaddingValues,
    state: BatchTagEditState,
    onToggleField:
        (
            BatchTagField,
            Boolean,
        ) -> Unit,
    onValue:
        (
            BatchTagField,
            String,
        ) -> Unit,
    requestWriteConsent:
        () -> IntentSender?,
    onSave: () -> Unit,
    onBack: () -> Unit,
    confirm: (String) -> Unit,
) {
    val context =
        androidx.compose.ui.platform.LocalContext.current

    val consentLauncher =
        rememberLauncherForActivityResult(
            ActivityResultContracts
                .StartIntentSenderForResult()
        ) { result ->
            if (
                result.resultCode ==
                    Activity.RESULT_OK
            ) {
                onSave()
            } else {
                confirm(
                    context.getString(
                        com.mentality.sonethyst.R.string.batch_tags_write_denied
                    )
                )
            }
        }

    val beginSave = {
        val consent =
            requestWriteConsent()

        if (consent != null) {
            consentLauncher.launch(
                IntentSenderRequest
                    .Builder(consent)
                    .build()
            )
        } else {
            onSave()
        }
    }

    Column(
        Modifier.fillMaxSize()
    ) {
        SettingsTopBar(
            androidx.compose.ui.res.stringResource(
                com.mentality.sonethyst.R.string.batch_tags_title
            ),
            onBack,
        )

        if (state.loading) {
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment =
                    Alignment.CenterHorizontally,
                verticalArrangement =
                    Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }

            return
        }

        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(
                    rememberScrollState()
                )
                .padding(
                    horizontal = 16.dp
                )
                .padding(
                    bottom =
                        contentPadding
                            .calculateBottomPadding() +
                            24.dp
                )
        ) {
            Text(
                androidx.compose.ui.res.pluralStringResource(
                    com.mentality.sonethyst.R.plurals.batch_tags_selected,
                    state.items.size,
                    state.items.size,
                ),
                style =
                    MaterialTheme
                        .typography
                        .titleMedium,
                fontWeight =
                    FontWeight.Bold,
            )

            Spacer(
                Modifier.height(4.dp)
            )

            Text(
                androidx.compose.ui.res.stringResource(
                    com.mentality.sonethyst.R.string.batch_tags_explanation
                ),
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
                Modifier.height(12.dp)
            )

            BatchField(
                androidx.compose.ui.res.stringResource(
                    com.mentality.sonethyst.R.string.batch_tags_field_title
                ),
                state.fields[
                    BatchTagField.TITLE
                ],
                {
                    onToggleField(
                        BatchTagField.TITLE,
                        it,
                    )
                },
                {
                    onValue(
                        BatchTagField.TITLE,
                        it,
                    )
                },
            )

            BatchField(
                androidx.compose.ui.res.stringResource(
                    com.mentality.sonethyst.R.string.batch_tags_field_artist
                ),
                state.fields[
                    BatchTagField.ARTIST
                ],
                {
                    onToggleField(
                        BatchTagField.ARTIST,
                        it,
                    )
                },
                {
                    onValue(
                        BatchTagField.ARTIST,
                        it,
                    )
                },
            )

            BatchField(
                androidx.compose.ui.res.stringResource(
                    com.mentality.sonethyst.R.string.batch_tags_field_album
                ),
                state.fields[
                    BatchTagField.ALBUM
                ],
                {
                    onToggleField(
                        BatchTagField.ALBUM,
                        it,
                    )
                },
                {
                    onValue(
                        BatchTagField.ALBUM,
                        it,
                    )
                },
            )

            BatchField(
                androidx.compose.ui.res.stringResource(
                    com.mentality.sonethyst.R.string.batch_tags_field_album_artist
                ),
                state.fields[
                    BatchTagField.ALBUM_ARTIST
                ],
                {
                    onToggleField(
                        BatchTagField.ALBUM_ARTIST,
                        it,
                    )
                },
                {
                    onValue(
                        BatchTagField.ALBUM_ARTIST,
                        it,
                    )
                },
            )

            BatchField(
                androidx.compose.ui.res.stringResource(
                    com.mentality.sonethyst.R.string.batch_tags_field_genre
                ),
                state.fields[
                    BatchTagField.GENRE
                ],
                {
                    onToggleField(
                        BatchTagField.GENRE,
                        it,
                    )
                },
                {
                    onValue(
                        BatchTagField.GENRE,
                        it,
                    )
                },
            )

            BatchField(
                androidx.compose.ui.res.stringResource(
                    com.mentality.sonethyst.R.string.batch_tags_field_year
                ),
                state.fields[
                    BatchTagField.YEAR
                ],
                {
                    onToggleField(
                        BatchTagField.YEAR,
                        it,
                    )
                },
                {
                    onValue(
                        BatchTagField.YEAR,
                        it,
                    )
                },
            )

            BatchField(
                androidx.compose.ui.res.stringResource(
                    com.mentality.sonethyst.R.string.batch_tags_field_track_number
                ),
                state.fields[
                    BatchTagField.TRACK_NUMBER
                ],
                {
                    onToggleField(
                        BatchTagField.TRACK_NUMBER,
                        it,
                    )
                },
                {
                    onValue(
                        BatchTagField.TRACK_NUMBER,
                        it,
                    )
                },
            )

            Spacer(
                Modifier.height(16.dp)
            )

            val anythingEnabled =
                state.fields.values
                    .any {
                        it.enabled
                    }

            Button(
                onClick = beginSave,
                enabled =
                    !state.saving &&
                        state.items.isNotEmpty() &&
                        anythingEnabled,
                modifier =
                    Modifier.fillMaxWidth(),
            ) {
                if (state.saving) {
                    CircularProgressIndicator(
                        Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                    )

                    Spacer(
                        Modifier.size(8.dp)
                    )
                }

                Text(
                    if (state.saving) {
                        androidx.compose.ui.res.stringResource(
                            com.mentality.sonethyst.R.string.batch_tags_saving
                        )
                    } else {
                        androidx.compose.ui.res.stringResource(
                            com.mentality.sonethyst.R.string.batch_tags_apply
                        )
                    },
                    fontWeight =
                        FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
private fun BatchField(
    label: String,
    state: BatchTagFieldState?,
    onEnabled: (Boolean) -> Unit,
    onValue: (String) -> Unit,
) {
    if (state == null) {
        return
    }

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment =
            Alignment.Top,
    ) {
        Checkbox(
            checked = state.enabled,
            onCheckedChange = onEnabled,
            modifier =
                Modifier.padding(
                    top = 8.dp
                ),
        )

        Column(
            Modifier.weight(1f)
        ) {
            OutlinedTextField(
                value = state.value,
                onValueChange = onValue,
                enabled = state.enabled,
                label = {
                    Text(label)
                },
                placeholder =
                    if (state.mixed) {
                        {
                            Text(
                                androidx.compose.ui.res.stringResource(
                                    com.mentality.sonethyst.R.string.batch_tags_multiple_values
                                )
                            )
                        }
                    } else {
                        null
                    },
                singleLine = true,
                modifier =
                    Modifier.fillMaxWidth(),
            )

            if (
                state.mixed &&
                state.enabled &&
                state.value.isBlank()
            ) {
                Text(
                    androidx.compose.ui.res.stringResource(
                        com.mentality.sonethyst.R.string.batch_tags_blank_clears
                    ),
                    style =
                        MaterialTheme
                            .typography
                            .labelSmall,
                    color =
                        MaterialTheme
                            .colorScheme
                            .onSurfaceVariant,
                )
            }
        }
    }

    Spacer(
        Modifier.height(4.dp)
    )
}
