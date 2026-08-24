package com.mentality.sonethyst.ui.components

import android.util.Base64

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun Artwork(
    url: String,
    accent: Color,
    modifier: Modifier = Modifier,
    corner: Dp = 12.dp,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current

    val placeholder = Brush.linearGradient(
        listOf(
            accent.copy(alpha = 0.55f),
            accent.copy(alpha = 0.12f),
        )
    )

    val collageUrls = remember(url) {
        if (url.startsWith("sonethyst-collage:")) {
            url
                .removePrefix("sonethyst-collage:")
                .split(".")
                .filter { it.isNotBlank() }
                .mapNotNull { encoded ->
                    runCatching {
                        String(
                            Base64.decode(
                                encoded,
                                Base64.URL_SAFE or Base64.NO_WRAP,
                            ),
                            Charsets.UTF_8,
                        )
                    }.getOrNull()
                }
                .take(4)
        } else {
            emptyList()
        }
    }

    val request = remember(url, context) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(placeholder),
        contentAlignment = Alignment.Center,
    ) {
        if (collageUrls.size == 4) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    CollageArtworkCell(
                        url = collageUrls[0],
                        placeholder = placeholder,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )

                    CollageArtworkCell(
                        url = collageUrls[1],
                        placeholder = placeholder,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    CollageArtworkCell(
                        url = collageUrls[2],
                        placeholder = placeholder,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )

                    CollageArtworkCell(
                        url = collageUrls[3],
                        placeholder = placeholder,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
            }
        } else {
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun CollageArtworkCell(
    url: String,
    placeholder: Brush,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val request = remember(url, context) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier.background(placeholder),
    ) {
        AsyncImage(
            model = request,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(4.dp)
                .height(22.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.verticalGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                ),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        if (action != null && onAction != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onAction)
                    .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
            ) {
                Text(
                    text = action,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(3.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(11.dp),
                )
            }
        }
    }
}

@Composable
fun Eyebrow(text: String, color: Color, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Black,
        color = color,
        letterSpacing = 1.5.sp,
        modifier = modifier,
    )
}

@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    val base = MaterialTheme.colorScheme.background
    val top = MaterialTheme.colorScheme.primary
    val bottom = MaterialTheme.colorScheme.tertiary
    Box(modifier.fillMaxSize().background(base)) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.radialGradient(
                    colors = listOf(top.copy(alpha = 0.22f), Color.Transparent),
                    center = Offset(120f, -80f),
                    radius = 1100f,
                )
            )
        )
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0.55f to Color.Transparent,
                        1f to bottom.copy(alpha = 0.12f),
                    )
                )
            )
        )
    }
}

fun formatTime(totalSec: Int): String {
    val safe = totalSec.coerceAtLeast(0)
    val m = safe / 60
    val s = safe % 60
    return "%d:%02d".format(m, s)
}

fun formatTime(totalSec: Float): String = formatTime(totalSec.toInt())
