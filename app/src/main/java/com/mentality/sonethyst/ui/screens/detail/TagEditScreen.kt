package com.mentality.sonethyst.ui.screens.detail

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mentality.sonethyst.R
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.AudioTags
import com.mentality.sonethyst.data.TagEditor
import com.mentality.sonethyst.data.remote.MetadataMatch
import com.mentality.sonethyst.ui.components.Artwork
import com.mentality.sonethyst.ui.screens.settings.SettingsTopBar
import com.mentality.sonethyst.viewmodel.TagEditState
import kotlinx.coroutines.launch

// saving a local file goes through the mediastore write-consent dialog on android 11+
@Composable
fun TagEditScreen(
    contentPadding: PaddingValues,
    state: TagEditState,
    onEdit: ((AudioTags) -> AudioTags) -> Unit,
    onMatch: () -> Unit,
    onFindArtwork: () -> Unit,
    onApplyMatch: (MetadataMatch) -> Unit,
    onPickCover: (MetadataMatch) -> Unit,
    onPickLocalCover: (String) -> Unit,
    onIdentify: (() -> Unit)?,        // null when acoustid identify unavailable
    identifying: Boolean = false,
    onBack: () -> Unit,
    confirm: (String) -> Unit,
) {
    val context = LocalContext.current
    val container =
        (context.applicationContext as SonethystApplication)
            .container
    val scope = rememberCoroutineScope()
    var saving by remember { mutableStateOf(false) }

    val coverPicker =
        rememberLauncherForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                /*
                 * Keep access to the selected image stable across
                 * recomposition and while Save tags is running.
                 */
                runCatching {
                    context.contentResolver
                        .takePersistableUriPermission(
                            it,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION,
                        )
                }

                onPickLocalCover(
                    it.toString()
                )
            }
        }

    val doWrite: () -> Unit = {
        scope.launch {
            val uri = TagEditor.localContentUriFor(state.localContentUri)
            if (uri == null) { android.util.Log.w("TagEdit", "No valid local content URI for write"); confirm(
                    context.getString(
                        R.string.tag_cant_write_file
                    )
                ); saving = false } else {
                val pickedArtwork =
                    state.pickedCoverUrl

                val art =
                    when {
                        pickedArtwork.isBlank() ->
                            null

                        pickedArtwork.startsWith(
                            "content://"
                        ) ->
                            runCatching {
                                context.contentResolver
                                    .openInputStream(
                                        android.net.Uri.parse(
                                            pickedArtwork
                                        )
                                    )
                                    ?.use {
                                        it.readBytes()
                                    }
                            }.getOrNull()

                        else ->
                            container.musicBrainz
                                .fetchImage(
                                    pickedArtwork
                                )
                    }

                if (
                    pickedArtwork.isNotBlank() &&
                    art == null
                ) {
                    android.util.Log.w("TagEdit", "Artwork read failed")
                    saving = false
                    confirm(
                        context.getString(
                            R.string.tag_couldnt_read_artwork
                        )
                    )
                    return@launch
                }

                val ok =
                    container.tagEditor.write(
                        uri,
                        state.path,
                        state.tags,
                        art,
                    )
                saving = false
                if (ok) {
                    confirm(
                        context.getString(
                            R.string.tag_saved
                        )
                    )
                    runCatching {
                        container.refreshLocalLibrary(
                            state.path
                        )
                    }
                    onBack()
                } else {
                    confirm(
                        context.getString(
                            R.string.tag_save_failed
                        )
                    )
                }
            }
        }
    }
    val consentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) doWrite() else { android.util.Log.i("TagEdit", "MediaStore write consent denied"); saving = false; confirm(
                context.getString(
                    R.string.tag_write_permission_denied
                )
            ) }
    }
    val onSave: () -> Unit = {
        saving = true
        if (!state.localFile) {
            // server item updates via backend metadata api no file write or consent
            scope.launch {
                val ok = container.repository.updateMetadata(state.songId, state.tags)
                saving = false
                if (ok) {
                    container.notifyLibraryMetadataChanged()
                    confirm(
                        context.getString(
                            R.string.tag_metadata_updated
                        )
                    )
                    onBack()
                } else {
                    confirm(
                        context.getString(
                            R.string.tag_update_permission_failed
                        )
                    )
                }
            }
        } else {
            val uri = TagEditor.localContentUriFor(state.localContentUri)
            if (uri == null) { android.util.Log.w("TagEdit", "No valid local content URI for consent"); confirm(
                    context.getString(
                        R.string.tag_cant_write_file
                    )
                ); saving = false } else {
                val consent = container.tagEditor.writeConsentIntent(uri)
                if (consent != null) consentLauncher.launch(IntentSenderRequest.Builder(consent).build()) else doWrite()
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        SettingsTopBar(
            stringResource(R.string.tag_edit_title),
            onBack,
        )
        if (state.loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return
        }
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Artwork(state.pickedCoverUrl.ifBlank { state.artUrl }, MaterialTheme.colorScheme.primary, Modifier.size(72.dp), corner = 12.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.tags.title.ifBlank {
                        stringResource(R.string.tag_untitled)
                    }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(state.path.substringAfterLast('/'), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (state.pickedCoverUrl.isNotBlank()) Text(stringResource(R.string.tag_new_cover_staged), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.height(12.dp))

            Column(
                verticalArrangement =
                    Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = onMatch,
                        enabled = !state.matching,
                        modifier = Modifier.weight(1f),
                    ) {
                        if (state.matching) {
                            CircularProgressIndicator(
                                Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Filled.AutoFixHigh,
                                null,
                                Modifier.size(18.dp),
                            )
                        }

                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.tag_match_metadata))
                    }

                    if (state.localFile) {
                        OutlinedButton(
                            onClick = onFindArtwork,
                            enabled = !state.matching,
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(
                                Icons.Filled.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(
                                Modifier.width(6.dp)
                            )
                            Text(stringResource(R.string.tag_find_artwork))
                        }
                    }
                }

                if (
                    state.localFile ||
                    onIdentify != null
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement =
                            Arrangement.spacedBy(8.dp),
                    ) {
                        if (state.localFile) {
                            OutlinedButton(
                                onClick = {
                                    coverPicker.launch(
                                        arrayOf(
                                            "image/*"
                                        )
                                    )
                                },
                                modifier =
                                    Modifier.weight(1f),
                            ) {
                                Icon(
                                    Icons.Filled.AddPhotoAlternate,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(
                                    Modifier.width(6.dp)
                                )
                                Text(stringResource(R.string.tag_choose_image))
                            }
                        }

                        if (onIdentify != null) {
                            OutlinedButton(
                                onClick = onIdentify,
                                enabled = !identifying,
                                modifier =
                                    Modifier.weight(1f),
                            ) {
                                if (identifying) {
                                    CircularProgressIndicator(
                                        Modifier.size(16.dp),
                                        strokeWidth = 2.dp,
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Fingerprint,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }

                                Spacer(
                                    Modifier.width(6.dp)
                                )

                                Text(
                                    if (identifying) {
                                        stringResource(R.string.tag_identifying)
                                    } else {
                                        stringResource(R.string.tag_auto_identify)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            state.matchError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 4.dp)) }
            state.matches.forEach { m ->
                MatchRow(
                    m = m,
                    onApply = {
                        onApplyMatch(m)
                    },
                    onPickCover =
                        if (
                            state.localFile &&
                            m.coverUrl.isNotBlank()
                        ) {
                            { onPickCover(m) }
                        } else {
                            null
                        },
                )
            }

            Spacer(Modifier.height(8.dp))
            TagField(stringResource(R.string.smart_field_title), state.tags.title) { v -> onEdit { it.copy(title = v) } }
            TagField(stringResource(R.string.smart_field_artist), state.tags.artist) { v -> onEdit { it.copy(artist = v) } }
            TagField(stringResource(R.string.smart_field_album), state.tags.album) { v -> onEdit { it.copy(album = v) } }
            TagField(stringResource(R.string.tag_album_artist), state.tags.albumArtist) { v -> onEdit { it.copy(albumArtist = v) } }
            TagField(stringResource(R.string.tag_genre), state.tags.genre) { v -> onEdit { it.copy(genre = v) } }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.weight(1f)) { TagField(stringResource(R.string.tag_year), state.tags.year) { v -> onEdit { it.copy(year = v.filter { c -> c.isDigit() }) } } }
                Box(Modifier.weight(1f)) { TagField(stringResource(R.string.tag_track_number), state.tags.trackNumber) { v -> onEdit { it.copy(trackNumber = v.filter { c -> c.isDigit() }) } } }
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSave, enabled = !saving, modifier = Modifier.fillMaxWidth()) {
                if (saving) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)) }
                Text(
                    if (saving) {
                        stringResource(R.string.lyrics_edit_saving)
                    } else {
                        stringResource(R.string.tag_save)
                    }, fontWeight = FontWeight.Bold)
            }
            Text(
                if (state.localFile) {
                    stringResource(
                        R.string.tag_local_write_description
                    )
                } else {
                    stringResource(
                        R.string.tag_server_write_description
                    )
                },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun TagField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    )
}

@Composable
private fun MatchRow(
    m: MetadataMatch,
    onApply: () -> Unit,
    onPickCover: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh.copy(
                    alpha = 0.5f
                )
            )
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (m.coverUrl.isNotBlank()) {
            Artwork(m.coverUrl, MaterialTheme.colorScheme.primary, Modifier.size(40.dp), corner = 8.dp)
            Spacer(Modifier.width(10.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(m.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(m.artist.ifBlank { null }, m.album.ifBlank { null }, m.year.ifBlank { null }).joinToString(" • "),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        if (onPickCover != null) {
            Text(
                stringResource(R.string.tag_use_cover),
                style =
                    MaterialTheme.typography.labelLarge,
                color =
                    MaterialTheme.colorScheme.primary,
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onPickCover)
                        .padding(
                            horizontal = 8.dp,
                            vertical = 6.dp,
                        ),
            )
        }

        Text(
            stringResource(R.string.action_apply),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier =
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onApply)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 6.dp,
                    ),
        )
    }
}
