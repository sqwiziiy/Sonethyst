package com.aurora.music.ui.components

import androidx.compose.runtime.Composable
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.LottieProperty
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.aurora.music.R

@Composable
fun LottieEqualizer(
    modifier: Modifier = Modifier,
    isPlaying: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.equalizer)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isPlaying,
    )

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = color.toArgb(),
            keyPath = arrayOf("**"),
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = color.toArgb(),
            keyPath = arrayOf("**"),
        ),
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
        dynamicProperties = dynamicProperties,
    )
}

@Composable
fun LottieLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.loader)
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
    )

    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR,
            value = color.toArgb(),
            keyPath = arrayOf("**"),
        ),
        rememberLottieDynamicProperty(
            property = LottieProperty.STROKE_COLOR,
            value = color.toArgb(),
            keyPath = arrayOf("**"),
        ),
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier,
        dynamicProperties = dynamicProperties,
    )
}
