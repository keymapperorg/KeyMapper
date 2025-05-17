package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

@Composable
fun KeyMapperSliderThumb(interactionSource: MutableInteractionSource, enabled: Boolean = true) {
    SliderDefaults.Thumb(
        interactionSource = interactionSource,
        thumbSize = DpSize(4.dp, 28.dp),
        enabled = enabled,
    )
}
