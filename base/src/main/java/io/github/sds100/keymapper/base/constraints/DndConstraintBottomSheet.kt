package io.github.sds100.keymapper.base.constraints

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.DndModeStrings
import io.github.sds100.keymapper.base.utils.ui.compose.KMBottomSheet
import io.github.sds100.keymapper.base.utils.ui.compose.RadioButtonText
import io.github.sds100.keymapper.system.volume.DndMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DndConstraintBottomSheet(delegate: CreateConstraintDelegate) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val state = delegate.dndConstraintState ?: return

    DndConstraintBottomSheet(
        sheetState = sheetState,
        selectedMode = state.dndMode,
        onSelectMode = { delegate.dndConstraintState = ConstraintData.DoNotDisturb(it) },
        onDismissRequest = { delegate.dndConstraintState = null },
        onDoneClick = delegate::onDoneConfigDndConstraintClick,
    )
}

/**
 * @param selectedMode the selected Do Not Disturb mode, or null if any mode is selected.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DndConstraintBottomSheet(
    sheetState: SheetState,
    selectedMode: DndMode?,
    onSelectMode: (DndMode?) -> Unit = {},
    onDismissRequest: () -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()

    KMBottomSheet(
        title = stringResource(R.string.constraint_dnd_mode_bottom_sheet_title),
        negButtonText = stringResource(R.string.neg_cancel),
        posButtonText = stringResource(R.string.pos_done),
        onPosButtonClick = {
            scope.launch {
                sheetState.hide()
                onDoneClick()
            }
        },
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
    ) {
        RadioButtonText(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.constraint_dnd_mode_any),
            isSelected = selectedMode == null,
            onSelected = { onSelectMode(null) },
        )

        for (mode in DndMode.entries) {
            RadioButtonText(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(DndModeStrings.getLabel(mode)),
                isSelected = selectedMode == mode,
                onSelected = { onSelectMode(mode) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun Preview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        DndConstraintBottomSheet(sheetState = sheetState, selectedMode = DndMode.PRIORITY)
    }
}
