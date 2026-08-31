package com.mentality.sonethyst.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Headset
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mentality.sonethyst.R
import com.mentality.sonethyst.SonethystApplication
import com.mentality.sonethyst.data.DiscordAccount
import com.mentality.sonethyst.data.LastfmAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun IntegrationsSettingsScreen(contentPadding: PaddingValues, onBack: () -> Unit, onOpenDiscordLogin: () -> Unit) {
    val container = (LocalContext.current.applicationContext as SonethystApplication).container
    val lrclib by container.settingsStore.lrclibEnabled.collectAsStateWithLifecycle(initialValue = true)
    val artistEnrichment by container.settingsStore.artistEnrichment.collectAsStateWithLifecycle(initialValue = true)
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(stringResource(R.string.integrations_title), onBack)
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp)) {
            item { SettingsSectionTitle(stringResource(R.string.integrations_lyrics)) }
            item {
                SettingsGroup {
                    SettingsSwitchRow(
                        Icons.Filled.Lyrics,
                        stringResource(R.string.integrations_lrclib),
                        stringResource(R.string.integrations_lrclib_summary), lrclib) { v ->
                        scope.launch { container.settingsStore.setLrclibEnabled(v) }
                    }
                }
            }
            item { SettingsSectionTitle(stringResource(R.string.integrations_scrobbling_presence)) }
            item { SettingsGroup { LastfmRow(scope) } }
            item { SettingsGroup { ListenBrainzRow(scope) } }
            item { SettingsGroup { DiscordRow(scope, onOpenDiscordLogin) } }
            item { SettingsSectionTitle(stringResource(R.string.integrations_metadata)) }
            item {
                SettingsGroup {
                    SettingsSwitchRow(
                        Icons.Filled.Person,
                        stringResource(R.string.integrations_artist_info),
                        stringResource(R.string.integrations_artist_info_summary), artistEnrichment) { v ->
                        scope.launch { container.settingsStore.setArtistEnrichment(v) }
                    }
                }
            }
        }
    }
}

@Composable
private fun LastfmRow(scope: CoroutineScope) {
    val ctx = LocalContext.current
    val container = (ctx.applicationContext as SonethystApplication).container
    val acct by container.settingsStore.lastfm.collectAsStateWithLifecycle(initialValue = LastfmAccount())
    var pendingToken by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }

    when {
        acct.sessionKey.isNotBlank() -> {
            SettingsSwitchRow(
                Icons.Filled.Headset,
                "Last.fm",
                stringResource(
                    R.string.integrations_scrobbling_as,
                    acct.username,
                ), acct.enabled) { v ->
                scope.launch { container.settingsStore.setLastfmEnabled(v) }
            }
            SettingsRowDivider()
            SettingsNavRow(
                Icons.Filled.LinkOff,
                stringResource(R.string.integrations_disconnect_lastfm),
                value = stringResource(R.string.action_remove),
            ) {
                scope.launch { container.lastfm.disconnect() }
            }
        }
        pendingToken != null -> {
            SettingsNavRow(
                Icons.Filled.Headset,
                stringResource(R.string.integrations_finish_lastfm),
                subtitle =
                    status
                        ?: stringResource(
                            R.string.integrations_lastfm_finish_help
                        ),
                value =
                    if (busy) {
                        "…"
                    } else {
                        stringResource(R.string.integrations_done)
                    },
            ) {
                if (busy) return@SettingsNavRow
                scope.launch {
                    busy = true; status = null
                    val ok = container.lastfm.finishLink(pendingToken!!)
                    busy = false
                    if (ok) {
                        pendingToken = null
                    } else {
                        status =
                            ctx.getString(
                                R.string.integrations_lastfm_not_authorized
                            )
                    }
                }
            }
        }
        else -> {
            // user supplies own last.fm credentials none shipped
            val keys by container.settingsStore.lastfmKeys.collectAsStateWithLifecycle(initialValue = "" to "")
            var apiKey by remember(keys.first) { mutableStateOf(keys.first) }
            var secret by remember(keys.second) { mutableStateOf(keys.second) }
            OutlinedTextField(
                value = apiKey, onValueChange = { apiKey = it; scope.launch { container.settingsStore.setLastfmKeys(it, secret) } },
                label = {
                    Text(
                        stringResource(
                            R.string.integrations_lastfm_api_key
                        )
                    )
                }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            )
            OutlinedTextField(
                value = secret, onValueChange = { secret = it; scope.launch { container.settingsStore.setLastfmKeys(apiKey, it) } },
                label = {
                    Text(
                        stringResource(
                            R.string.integrations_lastfm_secret
                        )
                    )
                }, singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
            )
            SettingsNavRow(
                Icons.Filled.Headset,
                stringResource(R.string.integrations_connect_lastfm),
                subtitle = status ?: if (container.lastfm.configured) {
                        stringResource(
                            R.string.integrations_lastfm_scrobble
                        )
                    } else {
                        stringResource(
                            R.string.integrations_lastfm_setup
                        )
                    },
                value =
                    if (busy) {
                        "…"
                    } else {
                        stringResource(R.string.auth_connect)
                    },
            ) {
                if (busy || !container.lastfm.configured) return@SettingsNavRow
                scope.launch {
                    busy = true; status = null
                    val token = container.lastfm.beginLink()
                    busy = false
                    if (token == null) {
                        status =
                            ctx.getString(
                                R.string.integrations_lastfm_unreachable
                            )
                        return@launch
                    }
                    pendingToken = token
                    runCatching {
                        ctx.startActivity(
                            android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(container.lastfm.authorizeUrl(token)))
                                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ListenBrainzRow(scope: CoroutineScope) {
    val ctx = LocalContext.current
    val container = (ctx.applicationContext as SonethystApplication).container
    val acct by container.settingsStore.listenBrainz.collectAsStateWithLifecycle(initialValue = com.mentality.sonethyst.data.ListenBrainzAccount())
    if (acct.token.isNotBlank()) {
        SettingsSwitchRow(
            Icons.Filled.Album,
            "ListenBrainz",
            stringResource(
                R.string.integrations_scrobbling_as,
                acct.username,
            ), acct.enabled) { v ->
            scope.launch { container.settingsStore.setListenBrainzEnabled(v) }
        }
        SettingsRowDivider()
        SettingsNavRow(
            Icons.Filled.LinkOff,
            stringResource(
                R.string.integrations_disconnect_listenbrainz
            ),
            value = stringResource(R.string.action_remove),
        ) {
            scope.launch { container.listenBrainz.disconnect() }
        }
    } else {
        var token by remember { mutableStateOf("") }
        var status by remember { mutableStateOf<String?>(null) }
        var busy by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = token,
            onValueChange = { token = it; status = null },
            label = {
                Text(
                    stringResource(
                        R.string.integrations_listenbrainz_token
                    )
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )
        SettingsNavRow(
            Icons.Filled.Album,
            stringResource(R.string.integrations_connect_listenbrainz),
            subtitle =
                status
                    ?: stringResource(
                        R.string.integrations_listenbrainz_token_help
                    ),
            value = if (busy) "…" else "Connect",
        ) {
            if (busy || token.isBlank()) return@SettingsNavRow
            scope.launch {
                busy = true; status = null
                val ok = container.listenBrainz.connect(token)
                busy = false
                status =
                    if (ok) {
                        null
                    } else {
                        ctx.getString(
                            R.string.integrations_invalid_token
                        )
                    }
            }
        }
    }
}

@Composable
private fun DiscordRow(scope: CoroutineScope, onConnect: () -> Unit) {
    val ctx = LocalContext.current
    val container = (ctx.applicationContext as SonethystApplication).container
    val acct by container.settingsStore.discord.collectAsStateWithLifecycle(initialValue = DiscordAccount())

    if (acct.token.isNotBlank()) {
        SettingsSwitchRow(
            Icons.Filled.Forum,
            stringResource(R.string.integrations_discord_presence),
            if (acct.username.isNotBlank()) {
                stringResource(
                    R.string.integrations_connected_as,
                    acct.username,
                )
            } else {
                stringResource(R.string.integrations_connected)
            },
            acct.enabled,
        ) { v -> scope.launch { container.settingsStore.setDiscordEnabled(v) } }
        var appId by remember { mutableStateOf(acct.appId) }
        OutlinedTextField(
            value = appId,
            onValueChange = { appId = it; scope.launch { container.settingsStore.setDiscordAppId(it.trim()) } },
            label = {
                Text(
                    stringResource(
                        R.string.integrations_discord_app_id
                    )
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )
        var imgur by remember { mutableStateOf(acct.imgurClientId) }
        OutlinedTextField(
            value = imgur,
            onValueChange = { imgur = it; scope.launch { container.settingsStore.setDiscordImgur(it.trim()) } },
            label = {
                Text(
                    stringResource(
                        R.string.integrations_imgur_client_id
                    )
                )
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        )
        SettingsRowDivider()
        SettingsNavRow(
            Icons.Filled.LinkOff,
            stringResource(R.string.integrations_disconnect_discord),
            value = stringResource(R.string.action_remove),
        ) {
            scope.launch { container.settingsStore.clearDiscord() }
        }
    } else {
        SettingsNavRow(
            Icons.Filled.Forum,
            stringResource(R.string.integrations_connect_discord),
            stringResource(R.string.integrations_discord_summary),
            value = stringResource(R.string.auth_connect),
            onClick = onConnect,
        )
    }
}
