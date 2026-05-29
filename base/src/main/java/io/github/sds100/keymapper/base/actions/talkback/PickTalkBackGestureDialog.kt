package io.github.sds100.keymapper.base.actions.talkback

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.CreateActionDelegate
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.CustomDialog

data class TalkBackGestureDialogState(val selectedGesture: TalkBackGestureType)

@Composable
fun PickTalkBackGestureDialog(delegate: CreateActionDelegate) {
    val state = delegate.talkBackGestureDialogState ?: return

    var selected by remember(state) { mutableStateOf(state.selectedGesture) }

    PickTalkBackGestureDialog(
        selected = selected,
        onSelectGesture = { selected = it },
        onDismissRequest = { delegate.talkBackGestureDialogState = null },
        onConfirm = {
            delegate.talkBackGestureDialogState =
                delegate.talkBackGestureDialogState?.copy(selectedGesture = selected)
            delegate.onDoneConfigTalkBackGestureClick()
        },
    )
}

@Composable
private fun PickTalkBackGestureDialog(
    selected: TalkBackGestureType,
    onSelectGesture: (TalkBackGestureType) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
) {
    val groups = remember {
        listOf(
            R.string.talkback_gesture_section_1_finger to listOf(
                TalkBackGestureType.SWIPE_UP,
                TalkBackGestureType.SWIPE_DOWN,
                TalkBackGestureType.SWIPE_LEFT,
                TalkBackGestureType.SWIPE_RIGHT,
                TalkBackGestureType.SWIPE_UP_THEN_DOWN,
                TalkBackGestureType.SWIPE_DOWN_THEN_UP,
                TalkBackGestureType.SWIPE_LEFT_THEN_RIGHT,
                TalkBackGestureType.SWIPE_RIGHT_THEN_LEFT,
                TalkBackGestureType.SWIPE_RIGHT_THEN_UP,
            ),
            R.string.talkback_gesture_section_2_finger to listOf(
                TalkBackGestureType.TWO_FINGER_TAP,
                TalkBackGestureType.TWO_FINGER_DOUBLE_TAP_HOLD,
                TalkBackGestureType.TWO_FINGER_TRIPLE_TAP,
                TalkBackGestureType.TWO_FINGER_TRIPLE_TAP_HOLD,
            ),
            R.string.talkback_gesture_section_3_finger to listOf(
                TalkBackGestureType.THREE_FINGER_TAP,
                TalkBackGestureType.THREE_FINGER_TAP_HOLD,
                TalkBackGestureType.THREE_FINGER_TRIPLE_TAP_HOLD,
                TalkBackGestureType.THREE_FINGER_SWIPE_UP,
                TalkBackGestureType.THREE_FINGER_SWIPE_DOWN,
            ),
            R.string.talkback_gesture_section_4_finger to listOf(
                TalkBackGestureType.FOUR_FINGER_TAP,
                TalkBackGestureType.FOUR_FINGER_DOUBLE_TAP,
                TalkBackGestureType.FOUR_FINGER_SWIPE_UP,
                TalkBackGestureType.FOUR_FINGER_SWIPE_DOWN,
                TalkBackGestureType.FOUR_FINGER_SWIPE_LEFT,
                TalkBackGestureType.FOUR_FINGER_SWIPE_RIGHT,
            ),
        )
    }

    CustomDialog(
        title = stringResource(R.string.action_talkback_gesture),
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.pos_done))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn {
            for ((headerResId, gestures) in groups) {
                stickyHeader(key = headerResId) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Text(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            text = stringResource(headerResId),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                items(gestures, key = { it.name }) { gesture ->
                    TalkBackGestureItem(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        gesture = gesture,
                        isSelected = selected == gesture,
                        onSelected = { onSelectGesture(gesture) },
                    )
                }
            }
        }
    }
}

@Composable
private fun TalkBackGestureItem(
    modifier: Modifier = Modifier,
    gesture: TalkBackGestureType,
    isSelected: Boolean,
    onSelected: () -> Unit,
) {
    Surface(modifier = modifier, shape = MaterialTheme.shapes.medium, color = Color.Transparent) {
        Row(
            modifier = Modifier
                .clickable(onClick = onSelected)
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            RadioButton(selected = isSelected, onClick = null)

            Column(modifier = Modifier.padding(start = 8.dp)) {
                Text(
                    text = stringResource(TalkBackGestureStrings.getActionLabel(gesture)),
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(TalkBackGestureStrings.getGestureLabel(gesture)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewPickTalkBackGestureDialog() {
    KeyMapperTheme {
        var selected by remember { mutableStateOf(TalkBackGestureType.SWIPE_UP) }

        PickTalkBackGestureDialog(
            selected = selected,
            onSelectGesture = { selected = it },
            onDismissRequest = {},
            onConfirm = {},
        )
    }
}
