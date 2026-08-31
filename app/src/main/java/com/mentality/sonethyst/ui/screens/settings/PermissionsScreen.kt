package com.mentality.sonethyst.ui.screens.settings

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

import com.mentality.sonethyst.R

@Composable
fun PermissionsScreen(contentPadding: PaddingValues, onBack: () -> Unit) {
    val ctx = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }

    // re-read live perm state on resume from system settings
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) refresh++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }

    refresh // recompute statuses on each bump
    val audioPerm = if (Build.VERSION.SDK_INT >= 33) Manifest.permission.READ_MEDIA_AUDIO else Manifest.permission.READ_EXTERNAL_STORAGE
    val notifOk = if (Build.VERSION.SDK_INT >= 33) NotificationManagerCompat.from(ctx).areNotificationsEnabled() else true
    val audioOk = ContextCompat.checkSelfPermission(ctx, audioPerm) == PackageManager.PERMISSION_GRANTED
    val batteryOk = (ctx.getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(ctx.packageName)
    val exactOk = if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms() else true
    val fsOk = if (Build.VERSION.SDK_INT >= 34) (ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent() else true

    val usbDev = remember(refresh) { com.decent.usbaudio.UsbAudioDevice.getInstance(ctx) }
    val dac = remember(refresh) { usbDev.findUsbAudioDevice() }
    val dacOk = dac != null && usbDev.hasPermission(dac)

    fun open(action: String, withPackage: Boolean = false) {
        runCatching {
            val i = Intent(action)
            if (withPackage) i.data = Uri.parse("package:" + ctx.packageName)
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ctx.startActivity(i)
        }
    }

    Column(Modifier.fillMaxWidth()) {
        SettingsTopBar(stringResource(R.string.permissions_title), onBack)
        LazyColumn(
            Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            item {
                Text(
                    stringResource(R.string.permissions_intro),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                )
            }
            item {
                PermRow(
                    Icons.Filled.Notifications,
                    stringResource(R.string.permissions_notifications),
                    stringResource(R.string.permissions_notifications_summary), notifOk) {
                    if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    else open(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                }
            }
            item {
                PermRow(
                    Icons.Filled.LibraryMusic,
                    stringResource(R.string.permissions_device_music),
                    stringResource(R.string.permissions_device_music_summary), audioOk) {
                    audioLauncher.launch(audioPerm)
                }
            }
            item {
                PermRow(
                    Icons.Filled.BatteryStd,
                    stringResource(R.string.permissions_battery),
                    stringResource(R.string.permissions_battery_summary), batteryOk) {
                    open(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, withPackage = true)
                }
            }
            item {
                PermRow(
                    Icons.Filled.Alarm,
                    stringResource(R.string.permissions_exact_alarms),
                    stringResource(R.string.permissions_exact_alarms_summary), exactOk) {
                    if (Build.VERSION.SDK_INT >= 31) open(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                }
            }
            item {
                PermRow(
                    Icons.Filled.Fullscreen,
                    stringResource(R.string.permissions_fullscreen_alarm),
                    stringResource(R.string.permissions_fullscreen_alarm_summary), fsOk) {
                    if (Build.VERSION.SDK_INT >= 34) open(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, withPackage = true)
                }
            }
            item {
                val dacName =
                    dac?.productName
                        ?.toString()
                        ?.takeIf {
                            it.isNotBlank()
                        }
                        ?: stringResource(
                            R.string.permissions_dac_generic
                        )

                val sub =
                    when {
                        dac == null ->
                            stringResource(
                                R.string.permissions_no_usb_dac
                            )

                        dacOk ->
                            stringResource(
                                R.string.permissions_dac_granted,
                                dacName,
                            )

                        else ->
                            stringResource(
                                R.string.permissions_dac_allow,
                                dacName,
                            )
                    }
                PermRow(
                    Icons.Filled.Usb,
                    stringResource(R.string.permissions_usb_dac),
                    sub, dacOk, enabled = dac != null) {
                    dac?.let { usbDev.requestPermission(it) { refresh++ } }
                }
            }
            item {
                Text(
                    stringResource(R.string.permissions_usb_note),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun PermRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    enabled: Boolean = true,
    onGrant: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(enabled = enabled && !granted, onClick = onGrant)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.width(10.dp))
        if (granted) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.permissions_granted), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
        } else if (enabled) {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary)
                    .clickable(onClick = onGrant).padding(horizontal = 14.dp, vertical = 7.dp),
            ) {
                Text(stringResource(R.string.permissions_grant), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
    }
}
