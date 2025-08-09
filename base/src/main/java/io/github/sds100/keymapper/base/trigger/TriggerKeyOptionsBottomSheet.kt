package io.github.sds100.keymapper.base.trigger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.keymaps.ClickType
import io.github.sds100.keymapper.base.system.accessibility.FingerprintGestureType
import io.github.sds100.keymapper.base.utils.ui.CheckBoxListItem
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.RadioButtonText
import io.github.sds100.keymapper.base.utils.ui.compose.openUriSafe
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriggerKeyOptionsBottomSheet(
    modifier: Modifier = Modifier,
    sheetState: SheetState,
    state: TriggerKeyOptionsState,
    onDismissRequest: () -> Unit = {},
    onCheckDoNotRemap: (Boolean) -> Unit = {},
    onSelectClickType: (ClickType) -> Unit = {},
    onSelectDevice: (String) -> Unit = {},
    onSelectAssistantType: (AssistantTriggerType) -> Unit = {},
    onSelectFingerprintGestureType: (FingerprintGestureType) -> Unit = {},
    onEditFloatingButtonClick: () -> Unit = {},
    onEditFloatingLayoutClick: () -> Unit = {},
) {
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // Hide drag handle because other bottom sheets don't have it
        dragHandle = {},
    ) {
        val uriHandler = LocalUriHandler.current
        val ctx = LocalContext.current
        val helpUrl = stringResource(R.string.url_trigger_key_options_guide)
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center,
                    text = stringResource(R.string.trigger_key_options_title),
                    style = MaterialTheme.typography.headlineMedium,
                )

                IconButton(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(horizontal = 8.dp),
                    onClick = { uriHandler.openUriSafe(ctx, helpUrl) },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                        contentDescription = null,
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // TODO use segmented button to switch between key code and scancode.

            if (state is TriggerKeyOptionsState.KeyEvent) {
                CheckBoxText(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(R.string.flag_dont_override_default_action),
                    isChecked = state.doNotRemapChecked,
                    onCheckedChange = onCheckDoNotRemap,
                )
            }

            if (state is TriggerKeyOptionsState.EvdevEvent) {
                CheckBoxText(
                    modifier = Modifier.padding(8.dp),
                    text = stringResource(R.string.flag_dont_override_default_action),
                    isChecked = state.doNotRemapChecked,
                    onCheckedChange = onCheckDoNotRemap,
                )
            }

            if (state.showClickTypes) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.trigger_key_click_types_header),
                    style = MaterialTheme.typography.titleSmall,
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                ) {
                    Spacer(Modifier.width(8.dp))

                    RadioButtonText(
                        modifier = Modifier.weight(1f),
                        isSelected = state.clickType == ClickType.SHORT_PRESS,
                        text = stringResource(R.string.radio_button_short_press),
                        onSelected = { onSelectClickType(ClickType.SHORT_PRESS) },
                    )

                    if (state.showLongPressClickType) {
                        RadioButtonText(
                            modifier = Modifier.weight(1f),
                            isSelected = state.clickType == ClickType.LONG_PRESS,
                            text = stringResource(R.string.radio_button_long_press),
                            onSelected = { onSelectClickType(ClickType.LONG_PRESS) },
                        )
                    }

                    RadioButtonText(
                        modifier = Modifier.weight(1f),
                        isSelected = state.clickType == ClickType.DOUBLE_PRESS,
                        text = stringResource(R.string.radio_button_double_press),
                        onSelected = { onSelectClickType(ClickType.DOUBLE_PRESS) },
                    )

                    Spacer(Modifier.width(8.dp))
                }
            }

            if (state is TriggerKeyOptionsState.KeyEvent) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.trigger_key_device_header),
                    style = MaterialTheme.typography.titleSmall,
                )

                for (device in state.devices) {
                    RadioButtonText(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        text = device.label,
                        isSelected = device.isChecked,
                        onSelected = { onSelectDevice(device.id) },
                    )
                }
            } else if (state is TriggerKeyOptionsState.Assistant) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.trigger_key_assistant_type_header),
                    style = MaterialTheme.typography.titleSmall,
                )

                RadioButtonText(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.assistant_any_trigger_name),
                    isSelected = state.assistantType == AssistantTriggerType.ANY,
                    onSelected = { onSelectAssistantType(AssistantTriggerType.ANY) },
                )

                RadioButtonText(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.assistant_device_trigger_name),
                    isSelected = state.assistantType == AssistantTriggerType.DEVICE,
                    onSelected = { onSelectAssistantType(AssistantTriggerType.DEVICE) },
                )

                RadioButtonText(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.assistant_voice_trigger_name),
                    isSelected = state.assistantType == AssistantTriggerType.VOICE,
                    onSelected = { onSelectAssistantType(AssistantTriggerType.VOICE) },
                )
            } else if (state is TriggerKeyOptionsState.FingerprintGesture) {
                Text(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    text = stringResource(R.string.trigger_key_fingerprint_gesture_type_header),
                    style = MaterialTheme.typography.titleSmall,
                )

                RadioButtonText(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.fingerprint_gesture_down),
                    isSelected = state.gestureType == FingerprintGestureType.SWIPE_DOWN,
                    onSelected = { onSelectFingerprintGestureType(FingerprintGestureType.SWIPE_DOWN) },
                )

                RadioButtonText(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.fingerprint_gesture_up),
                    isSelected = state.gestureType == FingerprintGestureType.SWIPE_UP,
                    onSelected = { onSelectFingerprintGestureType(FingerprintGestureType.SWIPE_UP) },
                )

                RadioButtonText(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.fingerprint_gesture_left),
                    isSelected = state.gestureType == FingerprintGestureType.SWIPE_LEFT,
                    onSelected = { onSelectFingerprintGestureType(FingerprintGestureType.SWIPE_LEFT) },
                )

                RadioButtonText(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    text = stringResource(R.string.fingerprint_gesture_right),
                    isSelected = state.gestureType == FingerprintGestureType.SWIPE_RIGHT,
                    onSelected = { onSelectFingerprintGestureType(FingerprintGestureType.SWIPE_RIGHT) },
                )
            }

            Spacer(Modifier.height(8.dp))

            if (state is TriggerKeyOptionsState.FloatingButton && state.isPurchased) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onEditFloatingButtonClick,
                    ) {
                        Text(stringResource(R.string.floating_button_trigger_option_configure_button))
                    }

                    Spacer(Modifier.width(16.dp))

                    FilledTonalButton(
                        modifier = Modifier.weight(1f),
                        onClick = onEditFloatingLayoutClick,
                    ) {
                        Text(stringResource(R.string.floating_button_trigger_option_edit_layout))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Spacer(Modifier.weight(1f))

                Spacer(Modifier.width(16.dp))

                FilledTonalButton(
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismissRequest()
                        }
                    },
                ) {
                    Spacer(Modifier.width(16.dp))
                    Text(stringResource(R.string.button_done))
                    Spacer(Modifier.width(16.dp))
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun Preview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = Expanded,
        )

        TriggerKeyOptionsBottomSheet(
            sheetState = sheetState,
            state = TriggerKeyOptionsState.KeyEvent(
                doNotRemapChecked = true,
                clickType = ClickType.DOUBLE_PRESS,
                showClickTypes = true,
                devices = listOf(
                    CheckBoxListItem(
                        id = "id1",
                        label = "Device 1",
                        isChecked = true,
                    ),
                    CheckBoxListItem(
                        id = "id2",
                        label = "Device 2",
                        isChecked = false,
                    ),
                ),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun AssistantPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = Expanded,
        )

        TriggerKeyOptionsBottomSheet(
            sheetState = sheetState,
            state = TriggerKeyOptionsState.Assistant(
                assistantType = AssistantTriggerType.VOICE,
                clickType = ClickType.DOUBLE_PRESS,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun FloatingButtonPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = Expanded,
        )

        TriggerKeyOptionsBottomSheet(
            sheetState = sheetState,
            state = TriggerKeyOptionsState.FloatingButton(
                clickType = ClickType.SHORT_PRESS,
                showClickTypes = true,
                isPurchased = true,
            ),
        )
    }
}
