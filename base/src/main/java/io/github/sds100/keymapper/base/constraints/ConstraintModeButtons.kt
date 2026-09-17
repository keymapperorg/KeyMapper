package io.github.sds100.keymapper.base.constraints

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow

/**
 * The AND/OR switcher that is used for the constraints in a group and for combining the groups.
 */
@Composable
fun ConstraintModeButtons(
    mode: ConstraintMode,
    onSelectMode: (ConstraintMode) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
) {
    KeyMapperSegmentedButtonRow(
        modifier = modifier,
        buttonStates = listOf(
            ConstraintMode.AND to stringResource(R.string.constraint_mode_and),
            ConstraintMode.OR to stringResource(R.string.constraint_mode_or),
        ),
        selectedState = mode,
        onStateSelected = onSelectMode,
        isCompact = true,
        isEnabled = isEnabled,
    )
}

@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        Surface {
            ConstraintModeButtons(
                modifier = Modifier
                    .padding(16.dp)
                    .width(160.dp),
                mode = ConstraintMode.AND,
                onSelectMode = {},
            )
        }
    }
}

@Preview
@Composable
private fun DisabledPreview() {
    KeyMapperTheme {
        Surface {
            ConstraintModeButtons(
                modifier = Modifier
                    .padding(16.dp)
                    .width(160.dp),
                mode = ConstraintMode.OR,
                onSelectMode = {},
                isEnabled = false,
            )
        }
    }
}
