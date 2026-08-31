package com.mentality.sonethyst.ui.screens.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Swipe
import androidx.compose.material.icons.filled.SwipeDown
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
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
import com.mentality.sonethyst.data.GesturePrefs
import com.mentality.sonethyst.data.PlaybackPrefs
import kotlinx.coroutines.launch

@Composable
fun GesturesSettingsScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val container = (LocalContext.current.applicationContext as SonethystApplication).container
    val store = container.settingsStore
    val gestures by store.gesturePrefs.collectAsStateWithLifecycle(initialValue = GesturePrefs())
    val haptics by store.haptics.collectAsStateWithLifecycle(initialValue = false)
    val privateSession by store.privateSession.collectAsStateWithLifecycle(initialValue = false)
    val playback by store.playbackPrefs.collectAsStateWithLifecycle(initialValue = PlaybackPrefs())
    var notifications by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(
            stringResource(R.string.gestures_title),
            onBack,
        )
        LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp)) {
            item { SettingsSectionTitle(stringResource(R.string.gestures_player)) }
            item {
                SettingsGroup {
                    SettingsSwitchRow(
                        Icons.Filled.Swipe,
                        stringResource(R.string.gestures_swipe_artwork),
                        stringResource(R.string.gestures_swipe_artwork_summary), gestures.swipeArtwork) { v -> scope.launch { store.setGestureSwipeArtwork(v) } }
                    SettingsRowDivider()
                    SettingsSwitchRow(
                        Icons.Filled.SwipeDown,
                        stringResource(R.string.gestures_swipe_down),
                        stringResource(R.string.gestures_swipe_down_summary), gestures.swipeDownDismiss) { v -> scope.launch { store.setGestureSwipeDismiss(v) } }
                    SettingsRowDivider()
                    SettingsSwitchRow(
                        Icons.Filled.TouchApp,
                        stringResource(R.string.gestures_double_tap),
                        stringResource(R.string.gestures_double_tap_summary), gestures.doubleTapPause) { v -> scope.launch { store.setGestureDoubleTap(v) } }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.gestures_feedback)) }
            item {
                SettingsGroup {
                    SettingsSwitchRow(
                        Icons.Filled.Vibration,
                        stringResource(R.string.gestures_haptics),
                        stringResource(R.string.gestures_haptics_summary), haptics) { v -> scope.launch { store.setHaptics(v) } }
                }
            }

            item { SettingsSectionTitle(stringResource(R.string.gestures_behaviour)) }
            item {
                SettingsGroup {
                    SettingsSwitchRow(
                        Icons.Filled.Notifications,
                        stringResource(R.string.gestures_notifications),
                        stringResource(R.string.gestures_notifications_summary), notifications) { notifications = it }
                    SettingsRowDivider()
                    SettingsSwitchRow(
                        Icons.Filled.Radio,
                        stringResource(R.string.gestures_autoplay_radio),
                        stringResource(R.string.gestures_autoplay_radio_summary), playback.autoplayRadio) { v -> scope.launch { store.setAutoplayRadio(v) } }
                    SettingsRowDivider()
                    SettingsSwitchRow(
                        Icons.Filled.History,
                        stringResource(R.string.gestures_scrobble),
                        stringResource(R.string.gestures_scrobble_summary), playback.scrobble) { v -> scope.launch { store.setScrobble(v) } }
                    SettingsRowDivider()
                    SettingsSwitchRow(
                        Icons.Filled.Lock,
                        stringResource(R.string.gestures_private_session),
                        stringResource(R.string.gestures_private_session_summary), privateSession) { v -> scope.launch { store.setPrivateSession(v) } }
                }
            }
        }
    }
}
