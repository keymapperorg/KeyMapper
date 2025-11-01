package io.github.sds100.keymapper.base.actions.keyevent

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.compose.LocalCustomColorsPalette
import io.github.sds100.keymapper.base.utils.ProModeStatus
import io.github.sds100.keymapper.base.utils.ui.compose.AccessibilityServiceRequirementRow
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.HeaderText
import io.github.sds100.keymapper.base.utils.ui.compose.InputMethodRequirementRow
import io.github.sds100.keymapper.base.utils.ui.compose.ProModeRequirementRow
import io.github.sds100.keymapper.base.utils.ui.compose.filledTonalButtonColorsError
import io.github.sds100.keymapper.base.utils.ui.compose.icons.KeyMapperIcons
import io.github.sds100.keymapper.base.utils.ui.compose.icons.ProModeIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixKeyEventActionBottomSheet(
    modifier: Modifier = Modifier,
    state: FixKeyEventActionState,
    sheetState: SheetState,
    onDismissRequest: () -> Unit = {},
    onSelectInputMethod: () -> Unit = {},
    onSelectProMode: () -> Unit = {},
    onEnableAccessibilityServiceClick: () -> Unit = {},
    onEnableProModeClick: () -> Unit = {},
    onEnableInputMethodClick: () -> Unit = {},
    onChooseInputMethodClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    onAutoSwitchImeCheckedChange: (Boolean) -> Unit = {},
) {
    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // Hide drag handle because other bottom sheets don't have it
        dragHandle = {},
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = stringResource(R.string.fix_key_event_action_title),
                style = MaterialTheme.typography.titleLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            Column(
                modifier = Modifier
                    .animateContentSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.fix_key_event_action_text))

                FixKeyEventActionOptionCard(
                    onClick = onSelectInputMethod,
                    selected = state is FixKeyEventActionState.InputMethod,
                    title = stringResource(R.string.fix_key_event_action_input_method_title),
                    icon = Icons.Rounded.Keyboard,
                ) {
                    val annotatedText = buildAnnotatedString {
                        appendInlineContent("icon", "[icon]")
                        append(" ")
                        append(stringResource(R.string.fix_key_event_action_input_method_text))
                    }
                    val inlineContent = mapOf(
                        Pair(
                            "icon",
                            InlineTextContent(
                                Placeholder(
                                    width = MaterialTheme.typography.bodyLarge.fontSize,
                                    height = MaterialTheme.typography.bodyLarge.fontSize,
                                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Remove,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            },
                        ),
                    )
                    Text(
                        annotatedText,
                        inlineContent = inlineContent,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                val isProModeUnsupported = state.proModeStatus == ProModeStatus.UNSUPPORTED

                FixKeyEventActionOptionCard(
                    onClick = onSelectProMode,
                    selected = state is FixKeyEventActionState.ProMode,
                    title = stringResource(R.string.pro_mode_app_bar_title),
                    icon = KeyMapperIcons.ProModeIcon,
                    enabled = !isProModeUnsupported,
                ) {
                    if (isProModeUnsupported) {
                        Text(
                            stringResource(R.string.trigger_setup_pro_mode_unsupported),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    } else {
                        val annotatedText = buildAnnotatedString {
                            appendInlineContent("icon", "[icon]")
                            append(" ")
                            append(stringResource(R.string.fix_key_event_action_pro_mode_text_1))
                            appendLine()
                            appendInlineContent("icon", "[icon]")
                            append(" ")
                            append(stringResource(R.string.fix_key_event_action_pro_mode_text_2))
                        }
                        val inlineContent = mapOf(
                            Pair(
                                "icon",
                                InlineTextContent(
                                    Placeholder(
                                        width = MaterialTheme.typography.bodyLarge.fontSize,
                                        height = MaterialTheme.typography.bodyLarge.fontSize,
                                        placeholderVerticalAlign =
                                        PlaceholderVerticalAlign.TextCenter,
                                    ),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = null,
                                        tint = LocalCustomColorsPalette.current.green,
                                    )
                                },
                            ),
                        )
                        Text(
                            annotatedText,
                            inlineContent = inlineContent,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                Text(
                    stringResource(R.string.fix_key_event_action_change_in_settings_caption),
                    style = MaterialTheme.typography.labelMedium,
                )
            }

            HeaderText(text = stringResource(R.string.fix_key_event_action_setup_title))

            AccessibilityServiceRequirementRow(
                modifier = Modifier.fillMaxWidth(),
                isServiceEnabled = state.isAccessibilityServiceEnabled,
                buttonColors = ButtonDefaults.filledTonalButtonColorsError(),
                onClick = onEnableAccessibilityServiceClick,
            )

            when (state) {
                is FixKeyEventActionState.InputMethod -> {
                    InputMethodRequirementRow(
                        modifier = Modifier.fillMaxWidth(),
                        isChosen = state.isChosen,
                        isEnabled = state.isEnabled,
                        enablingRequiresUserInput = state.enablingRequiresUserInput,
                        buttonColors = ButtonDefaults.filledTonalButtonColorsError(),
                        onEnableClick = onEnableInputMethodClick,
                        onChooseClick = onChooseInputMethodClick,
                    )

                    HeaderText(text = stringResource(R.string.fix_key_event_action_options_title))

                    CheckBoxText(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.fix_key_event_action_auto_switch_ime_text),
                        isChecked = state.isAutoSwitchImeEnabled,
                        onCheckedChange = onAutoSwitchImeCheckedChange,
                    )
                }

                is FixKeyEventActionState.ProMode -> {
                    ProModeRequirementRow(
                        modifier = Modifier.fillMaxWidth(),
                        isVisible = true,
                        proModeStatus = state.proModeStatus,
                        buttonColors = ButtonDefaults.filledTonalButtonColorsError(),
                        onClick = onEnableProModeClick,
                    )
                }
            }

            Button(modifier = Modifier.align(Alignment.End), onClick = onDoneClick) {
                Text(stringResource(R.string.pos_done))
            }
        }
    }
}

@Composable
private fun FixKeyEventActionOptionCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    selected: Boolean,
    title: String,
    icon: ImageVector,
    enabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardBorder = if (selected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        CardDefaults.outlinedCardBorder()
    }

    val cardElevation = if (selected) {
        CardDefaults.outlinedCardElevation(defaultElevation = 1.dp)
    } else {
        CardDefaults.outlinedCardElevation()
    }

    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        enabled = enabled,
        border = cardBorder,
        elevation = cardElevation,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 16.dp, end = 8.dp, bottom = 16.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                content()
            }
            RadioButton(
                modifier = Modifier.align(Alignment.CenterVertically),
                selected = selected,
                onClick = onClick,
                enabled = enabled,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun InputMethodPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        FixKeyEventActionBottomSheet(
            sheetState = sheetState,
            state = FixKeyEventActionState.InputMethod(
                isEnabled = true,
                isChosen = true,
                enablingRequiresUserInput = true,
                isAccessibilityServiceEnabled = true,
                isAutoSwitchImeEnabled = true,
                proModeStatus = ProModeStatus.ENABLED,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ProModePreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        FixKeyEventActionBottomSheet(
            sheetState = sheetState,
            state = FixKeyEventActionState.ProMode(
                proModeStatus = ProModeStatus.DISABLED,
                isAccessibilityServiceEnabled = true,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun ProModeUnsupportedPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        FixKeyEventActionBottomSheet(
            sheetState = sheetState,
            state = FixKeyEventActionState.InputMethod(
                proModeStatus = ProModeStatus.UNSUPPORTED,
                isEnabled = false,
                isChosen = false,
                enablingRequiresUserInput = true,
                isAutoSwitchImeEnabled = false,
                isAccessibilityServiceEnabled = true,
            ),
        )
    }
}
