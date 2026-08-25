package com.mentality.sonethyst.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.model.Song
import java.util.Locale

@Composable
fun SongTagsDialog(
    song: Song,
    currentTags: List<String>,
    existingTags: List<String>,
    onSave: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var selected by remember(song.id, currentTags) {
        mutableStateOf(
            currentTags.toSet()
        )
    }

    var newTag by remember(song.id) {
        mutableStateOf("")
    }

    val available =
        remember(existingTags, currentTags) {
            (existingTags + currentTags)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy {
                    it.lowercase(Locale.ROOT)
                }
                .sortedBy {
                    it.lowercase(Locale.ROOT)
                }
        }

    fun addNewTag() {
        val value = newTag.trim()

        if (
            value.isNotBlank() &&
            value.length <= 64
        ) {
            selected = selected + value
            newTag = ""
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Tags")
        },
        text = {
            Column {
                Text(
                    song.title,
                    style =
                        MaterialTheme.typography.titleSmall,
                )

                Spacer(Modifier.height(12.dp))

                if (available.isNotEmpty()) {
                    LazyColumn(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                    ) {
                        items(
                            items = available,
                            key = { it.lowercase(Locale.ROOT) },
                        ) { tag ->
                            val checked =
                                selected.any {
                                    it.equals(
                                        tag,
                                        ignoreCase = true,
                                    )
                                }

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selected =
                                                if (checked) {
                                                    selected.filterNot {
                                                        it.equals(
                                                            tag,
                                                            ignoreCase = true,
                                                        )
                                                    }.toSet()
                                                } else {
                                                    selected + tag
                                                }
                                        }
                                        .padding(vertical = 4.dp),
                                verticalAlignment =
                                    Alignment.CenterVertically,
                            ) {
                                Checkbox(
                                    checked = checked,
                                    onCheckedChange = {
                                        selected =
                                            if (checked) {
                                                selected.filterNot {
                                                    value ->
                                                    value.equals(
                                                        tag,
                                                        ignoreCase = true,
                                                    )
                                                }.toSet()
                                            } else {
                                                selected + tag
                                            }
                                    },
                                )

                                Text(tag)
                            }
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                    verticalAlignment =
                        Alignment.CenterVertically,
                ) {
                    OutlinedTextField(
                        value = newTag,
                        onValueChange = {
                            if (it.length <= 64) {
                                newTag = it
                            }
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("New tag") },
                        singleLine = true,
                    )

                    TextButton(
                        enabled =
                            newTag.trim().isNotBlank(),
                        onClick = {
                            addNewTag()
                        },
                    ) {
                        Text("Add")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    addNewTag()

                    onSave(
                        selected
                            .map { it.trim() }
                            .filter { it.isNotBlank() }
                            .distinctBy {
                                it.lowercase(Locale.ROOT)
                            }
                            .sortedBy {
                                it.lowercase(Locale.ROOT)
                            }
                    )
                },
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Cancel")
            }
        },
    )
}
