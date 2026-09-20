package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonColors
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A reusable segmented button row that follows KeyMapper's design patterns.
 *
 * @param modifier The modifier to apply to the segmented button row
 * @param buttonStates List of pairs containing the data and display text for each button
 * @param selectedState The currently selected state
 * @param onStateSelected Callback when a button is selected
 * @param isCompact Whether to use compact styling (smaller shapes, auto-sizing text)
 * @param isEnabled Whether the buttons are enabled
 * @param isStateEnabled Whether an individual button is enabled. Defaults to always true; use
 * this to disable specific options while leaving the rest of the row enabled.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> KeyMapperSegmentedButtonRow(
    modifier: Modifier = Modifier,
    buttonStates: List<Pair<T, String>>,
    selectedState: T?,
    onStateSelected: (T) -> Unit,
    isCompact: Boolean = false,
    isEnabled: Boolean = true,
    isStateEnabled: (T) -> Boolean = { true },
    colors: SegmentedButtonColors = SegmentedButtonDefaults.colors(),
) {
    val hasDisabledButton = !isEnabled || buttonStates.any { !isStateEnabled(it.first) }

    val colors = if (!hasDisabledButton) {
        colors
    } else {
        // The disabled border color of the inactive button is by default not greyed out enough
        colors.copy(
            disabledInactiveBorderColor =
            SegmentedButtonDefaults.colors().inactiveBorderColor.copy(alpha = 0.5f),
        )
    }

    SingleChoiceSegmentedButtonRow(
        modifier = modifier,
    ) {
        for (content in buttonStates) {
            val (state, label) = content
            val isSelected = state == selectedState
            val isButtonEnabled = isEnabled && isStateEnabled(state)
            val isUnselectedDisabled = !isButtonEnabled && !isSelected

            if (isCompact) {
                SegmentedButton(
                    modifier = Modifier.height(36.dp),
                    selected = isSelected,
                    onClick = { onStateSelected(state) },
                    enabled = isButtonEnabled,
                    icon = { },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = buttonStates.indexOf(content),
                        count = buttonStates.size,
                        baseShape = MaterialTheme.shapes.extraSmall,
                    ),
                    colors = colors,
                ) {
                    val contentColor = LocalContentColor.current

                    BasicText(
                        modifier = if (isUnselectedDisabled) Modifier.alpha(0.5f) else Modifier,
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        autoSize = TextAutoSize.StepBased(
                            maxFontSize = LocalTextStyle.current.fontSize,
                            minFontSize = 10.sp,
                        ),
                        color = { contentColor },
                    )
                }
            } else {
                SegmentedButton(
                    selected = isSelected,
                    onClick = { onStateSelected(state) },
                    enabled = isButtonEnabled,
                    shape = SegmentedButtonDefaults.itemShape(
                        index = buttonStates.indexOf(content),
                        count = buttonStates.size,
                    ),
                    colors = colors,
                ) {
                    Text(
                        modifier = if (isUnselectedDisabled) Modifier.alpha(0.5f) else Modifier,
                        text = label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
