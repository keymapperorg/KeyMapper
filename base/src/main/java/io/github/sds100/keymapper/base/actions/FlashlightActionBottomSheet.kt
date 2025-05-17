package io.github.sds100.keymapper.base.actions

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.CameraFront
import androidx.compose.material.icons.rounded.FlashlightOff
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import io.github.sds100.keymapper.system.camera.CameraFlashInfo
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSliderThumb
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.base.utils.ui.compose.RadioButtonText
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnableFlashlightActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.enableFlashlightActionState != null) {
        EnableFlashlightActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                delegate.enableFlashlightActionState = null
            },
            state = delegate.enableFlashlightActionState!!,
            onSelectStrength = delegate::onSelectStrength,
            onSelectLens = {
                delegate.enableFlashlightActionState =
                    delegate.enableFlashlightActionState?.copy(selectedLens = it)
            },
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneConfigEnableFlashlightClick()
                }
            },
            onTestClick = delegate::onTestFlashlightConfigClick,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnableFlashlightActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    state: EnableFlashlightActionState,
    onSelectLens: (CameraLens) -> Unit = {},
    onSelectStrength: (Int) -> Unit = {},
    onDoneClick: () -> Unit = {},
    onTestClick: () -> Unit = {},
) {
    FlashlightActionBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        title = stringResource(ActionUtils.getTitle(state.actionToCreate)),
        selectedLens = state.selectedLens,
        availableLenses = state.lensData.keys,
        onSelectLens = onSelectLens,
        onDoneClick = onDoneClick,
    ) {
        OptionsHeaderRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = Icons.Rounded.BrightnessMedium,
            text = stringResource(R.string.action_config_flashlight_brightness),
        )

        Spacer(modifier = Modifier.height(8.dp))

        val errorText = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            stringResource(R.string.action_config_flashlight_brightness_unsupported_android_version)
        } else if (!state.lensData[state.selectedLens]!!.supportsVariableStrength) {
            stringResource(R.string.action_config_flashlight_brightness_unsupported)
        } else {
            null
        }

        if (errorText != null) {
            Text(
                modifier = Modifier.padding(horizontal = 16.dp),
                text = errorText,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val interactionSource = remember { MutableInteractionSource() }
        val sliderDefault = state.lensData[state.selectedLens]!!.defaultStrength
        val sliderMax = state.lensData[state.selectedLens]!!.maxStrength.toFloat()

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Slider(
                modifier = Modifier.weight(1f),
                value = state.flashStrength.toFloat(),
                onValueChange = { onSelectStrength(it.roundToInt()) },
                enabled = errorText == null,
                interactionSource = interactionSource,
                thumb = {
                    KeyMapperSliderThumb(
                        interactionSource,
                        enabled = errorText == null,
                    )
                },
                valueRange = 1f..sliderMax,
                steps = sliderMax.toInt(),
            )

            Spacer(Modifier.width(8.dp))

            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = "${state.flashStrength} / ${sliderMax.toInt()}",
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }

        if (errorText == null) {
            Row(modifier = Modifier.padding(horizontal = 16.dp)) {
                Box(modifier = Modifier.weight(1f)) {
                    TextButton(
                        modifier = Modifier.align(Alignment.TopStart),
                        onClick = { onSelectStrength(1) },
                    ) {
                        Text(stringResource(R.string.action_config_flashlight_brightness_min))
                    }
                    TextButton(
                        modifier = Modifier.align(Alignment.TopCenter),
                        onClick = { onSelectStrength(((sliderMax - 1) / 2).toInt()) },
                    ) {
                        Text(stringResource(R.string.action_config_flashlight_brightness_half))
                    }
                    TextButton(
                        modifier = Modifier.align(Alignment.TopEnd),
                        onClick = { onSelectStrength(sliderMax.toInt()) },
                    ) {
                        Text(stringResource(R.string.action_config_flashlight_brightness_max))
                    }
                }

                Spacer(Modifier.width(8.dp))

                AnimatedVisibility(visible = state.flashStrength != sliderDefault) {
                    IconButton(onClick = { onSelectStrength(sliderDefault) }) {
                        Icon(
                            Icons.Rounded.RestartAlt,
                            contentDescription = stringResource(R.string.slider_reset_content_description),
                        )
                    }
                }
            }
        }

        if (errorText == null) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.action_config_flashlight_brightness_test),
                    style = MaterialTheme.typography.titleSmall,
                )

                Spacer(Modifier.width(8.dp))

                FilledTonalIconToggleButton(
                    checked = state.isFlashEnabled,
                    onCheckedChange = { onTestClick() },
                ) {
                    AnimatedContent(state.isFlashEnabled) { isEnabled ->
                        if (isEnabled) {
                            Icon(
                                imageVector = Icons.Rounded.FlashlightOn,
                                contentDescription = null,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.FlashlightOff,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeFlashlightStrengthActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.changeFlashlightStrengthActionState != null) {
        ChangeFlashlightStrengthActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                delegate.changeFlashlightStrengthActionState = null
            },
            state = delegate.changeFlashlightStrengthActionState!!,
            onSelectStrength = {
                delegate.changeFlashlightStrengthActionState =
                    delegate.changeFlashlightStrengthActionState?.copy(flashStrength = it)
            },
            onSelectLens = {
                delegate.changeFlashlightStrengthActionState =
                    delegate.changeFlashlightStrengthActionState?.copy(selectedLens = it)
            },
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneChangeFlashlightBrightnessClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangeFlashlightStrengthActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    state: ChangeFlashlightStrengthActionState,
    onSelectLens: (CameraLens) -> Unit = {},
    onSelectStrength: (Int) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    FlashlightActionBottomSheet(
        sheetState = sheetState,
        onDismissRequest = onDismissRequest,
        title = stringResource(R.string.action_flashlight_change_strength),
        selectedLens = state.selectedLens,
        availableLenses = state.lensData.entries.map { it.key }.toSet(),
        onSelectLens = onSelectLens,
        onDoneClick = onDoneClick,
    ) {
        OptionsHeaderRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            icon = Icons.Rounded.BrightnessMedium,
            text = stringResource(R.string.action_config_flashlight_brightness_factor),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val interactionSource = remember { MutableInteractionSource() }
            val lensData = state.lensData[state.selectedLens]!!
            val maxStrength = lensData.maxStrength

            Slider(
                modifier = Modifier.weight(1f),
                value = state.flashStrength.toFloat(),
                onValueChange = { onSelectStrength(it.roundToInt()) },
                interactionSource = interactionSource,
                thumb = {
                    KeyMapperSliderThumb(interactionSource)
                },
                valueRange = -maxStrength.toFloat()..maxStrength.toFloat(),
                // add 1 for the center value of 0.
                steps = (maxStrength * 2) + 1,
            )

            Spacer(Modifier.width(8.dp))

            val percentInt = ((state.flashStrength / maxStrength.toFloat()) * 100).toInt()
            val textPercent = if (state.flashStrength > 0) {
                "+$percentInt%"
            } else {
                "$percentInt%"
            }

            Text(
                modifier = Modifier.padding(horizontal = 4.dp),
                text = textPercent,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FlashlightActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    title: String,
    selectedLens: CameraLens,
    availableLenses: Set<CameraLens>,
    onSelectLens: (CameraLens) -> Unit = {},
    onDoneClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                textAlign = TextAlign.Center,
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (availableLenses.size > 1) {
                OptionsHeaderRow(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    icon = Icons.Rounded.CameraFront,
                    text = stringResource(R.string.action_config_flashlight_choose_side),
                )

                Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                    RadioButtonText(
                        modifier = Modifier,
                        text = stringResource(R.string.lens_front),
                        isSelected = selectedLens == CameraLens.FRONT,
                        onSelected = { onSelectLens(CameraLens.FRONT) },
                        isEnabled = availableLenses.contains(CameraLens.FRONT),
                    )

                    RadioButtonText(
                        modifier = Modifier,
                        text = stringResource(R.string.lens_back),
                        isSelected = selectedLens == CameraLens.BACK,
                        onSelected = { onSelectLens(CameraLens.BACK) },
                        isEnabled = availableLenses.contains(CameraLens.BACK),
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            content()
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                modifier = Modifier.weight(1f),
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismissRequest()
                    }
                },
            ) {
                Text(stringResource(R.string.neg_cancel))
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                modifier = Modifier.weight(1f),
                onClick = onDoneClick,
            ) {
                Text(stringResource(R.string.pos_done))
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

data class EnableFlashlightActionState(
    val actionToCreate: ActionId,
    val selectedLens: CameraLens,
    val lensData: Map<CameraLens, CameraFlashInfo>,
    val flashStrength: Int = 1,
    val isFlashEnabled: Boolean,
)

data class ChangeFlashlightStrengthActionState(
    val selectedLens: CameraLens,
    val lensData: Map<CameraLens, CameraFlashInfo>,
    /**
     * This can be positive or negative and must be less than +- max strength.
     */
    val flashStrength: Int = 0,
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewBothLenses() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        EnableFlashlightActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = EnableFlashlightActionState(
                actionToCreate = ActionId.ENABLE_FLASHLIGHT,
                selectedLens = CameraLens.BACK,
                flashStrength = 3,
                lensData = mapOf(
                    CameraLens.FRONT to CameraFlashInfo(
                        supportsVariableStrength = true,
                        defaultStrength = 5,
                        maxStrength = 10,
                    ),
                    CameraLens.BACK to CameraFlashInfo(
                        supportsVariableStrength = true,
                        defaultStrength = 5,
                        maxStrength = 10,
                    ),
                ),
                isFlashEnabled = true,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewOnlyBackLens() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        EnableFlashlightActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = EnableFlashlightActionState(
                actionToCreate = ActionId.TOGGLE_FLASHLIGHT,
                selectedLens = CameraLens.BACK,
                flashStrength = 3,
                lensData = mapOf(
                    CameraLens.BACK to CameraFlashInfo(
                        supportsVariableStrength = true,
                        defaultStrength = 5,
                        maxStrength = 10,
                    ),
                ),
                isFlashEnabled = false,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewOnlyBackLensChangeStrength() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        ChangeFlashlightStrengthActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = ChangeFlashlightStrengthActionState(
                selectedLens = CameraLens.BACK,
                flashStrength = -5,
                lensData = mapOf(
                    CameraLens.BACK to CameraFlashInfo(
                        supportsVariableStrength = true,
                        defaultStrength = 5,
                        maxStrength = 10,
                    ),
                ),
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(apiLevel = Build.VERSION_CODES.R)
@Composable
private fun PreviewUnsupportedAndroidVersion() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        EnableFlashlightActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = EnableFlashlightActionState(
                actionToCreate = ActionId.TOGGLE_FLASHLIGHT,
                selectedLens = CameraLens.BACK,
                flashStrength = 2,
                lensData = mapOf(
                    CameraLens.BACK to CameraFlashInfo(
                        supportsVariableStrength = true,
                        defaultStrength = 5,
                        maxStrength = 10,
                    ),
                ),
                isFlashEnabled = true,
            ),
        )
    }
}
