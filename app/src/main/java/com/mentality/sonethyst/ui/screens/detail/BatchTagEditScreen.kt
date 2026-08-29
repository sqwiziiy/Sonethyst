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
                    "Write permission denied"
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
            "Batch edit tags",
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
                "${state.items.size} tracks selected",
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
                "Only checked fields will be changed. Unchecked metadata stays untouched.",
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
                "Title",
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
                "Artist",
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
                "Album",
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
                "Album artist",
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
                "Genre",
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
                "Year",
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
                "Track #",
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
                        "Saving…"
                    } else {
                        "Apply to selected tracks"
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
                                "Multiple values"
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
                    "Blank will clear this field on all selected tracks.",
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
