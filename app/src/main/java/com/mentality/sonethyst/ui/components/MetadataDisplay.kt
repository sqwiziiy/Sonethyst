package com.mentality.sonethyst.ui.components

import android.content.Context
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mentality.sonethyst.R
import com.mentality.sonethyst.model.UNKNOWN_TITLE_SENTINEL

fun Context.displayArtist(raw: String): String = when (raw) {
    "Unknown artist" -> getString(R.string.common_unknown_artist)
    "Various artists" -> getString(R.string.common_various_artists)
    else -> raw
}

fun Resources.displayArtist(raw: String): String = when (raw) {
    "Unknown artist" -> getString(R.string.common_unknown_artist)
    "Various artists" -> getString(R.string.common_various_artists)
    else -> raw
}

fun Context.displayAlbum(raw: String): String =
    if (raw == "Unknown album") getString(R.string.common_unknown_album) else raw

fun Context.displayTitle(raw: String): String =
    if (raw == UNKNOWN_TITLE_SENTINEL) getString(R.string.common_unknown_title) else raw

fun Context.displayMetadata(raw: String): String = raw
    .replace("Unknown artist", getString(R.string.common_unknown_artist))
    .replace("Various artists", getString(R.string.common_various_artists))
    .replace("Unknown album", getString(R.string.common_unknown_album))

@Composable
fun displayArtist(raw: String): String = when (raw) {
    "Unknown artist" -> stringResource(R.string.common_unknown_artist)
    "Various artists" -> stringResource(R.string.common_various_artists)
    else -> raw
}

@Composable
fun displayAlbum(raw: String): String =
    if (raw == "Unknown album") stringResource(R.string.common_unknown_album) else raw

@Composable
fun displayTitle(raw: String): String =
    if (raw == UNKNOWN_TITLE_SENTINEL) stringResource(R.string.common_unknown_title) else raw

@Composable
fun displayMetadata(raw: String): String = raw
    .replace("Unknown artist", stringResource(R.string.common_unknown_artist))
    .replace("Various artists", stringResource(R.string.common_various_artists))
    .replace("Unknown album", stringResource(R.string.common_unknown_album))
