package io.github.sds100.keymapper.base.home

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.utils.ui.compose.CollapsableFloatingActionButton

@Composable
fun AnimatedFloatingActionButton(
    modifier: Modifier = Modifier,
    pulse: Boolean,
    showFabText: Boolean,
    text: String,
    onClick: () -> Unit,
) {
    val defaultColor = MaterialTheme.colorScheme.primaryContainer
    val pulseColor = LocalCustomColorsPalette.current.primaryContainerDarker

    val animatedPulseColor = remember { Animatable(defaultColor) }

    LaunchedEffect(pulse) {
        repeat(10) {
            animatedPulseColor.animateTo(pulseColor, tween(700))
            animatedPulseColor.animateTo(defaultColor, tween(700))
        }
    }

    CollapsableFloatingActionButton(
        modifier = modifier,
        onClick = onClick,
        showText = showFabText,
        text = text,
        containerColor = animatedPulseColor.value,
    )
}

@Preview
@Composable
private fun PreviewAnimatedFloatingActionButton() {
    KeyMapperTheme {
        AnimatedFloatingActionButton(
            pulse = false,
            showFabText = true,
            text = "New Key Map",
            onClick = {}
        )
    }
}

@Preview
@Composable
private fun PreviewAnimatedFloatingActionButtonPulsing() {
    KeyMapperTheme {
        AnimatedFloatingActionButton(
            pulse = true,
            showFabText = true,
            text = "New Key Map",
            onClick = {}
        )
    }
}