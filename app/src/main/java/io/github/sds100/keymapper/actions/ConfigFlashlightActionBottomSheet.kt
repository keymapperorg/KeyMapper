package io.github.sds100.keymapper.actions

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
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
import io.github.sds100.keymapper.util.ui.compose.KeyMapperSliderThumb
import io.github.sds100.keymapper.util.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.util.ui.compose.RadioButtonText
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigFlashlightActionBottomSheet(viewModel: CreateActionViewModel) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (viewModel.configFlashlightActionState != null) {
        ConfigFlashlightActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                viewModel.configFlashlightActionState = null
            },
            state = viewModel.configFlashlightActionState!!,
            onSelectStrength = {
                viewModel.configFlashlightActionState =
                    viewModel.configFlashlightActionState?.copy(flashStrength = it)
            },
            onSelectLens = {
                viewModel.configFlashlightActionState =
                    viewModel.configFlashlightActionState?.copy(selectedLens = it)
            },
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    viewModel.onDoneConfigFlashlightClicked()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigFlashlightActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    state: ConfigFlashlightActionState,
    onSelectLens: (CameraLens) -> Unit = {},
    onSelectStrength: (Int) -> Unit = {},
    onDoneClick: () -> Unit = {},
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()

    if (state.lensData.isEmpty()) {
        throw IllegalStateException("You can not configure a flashlight action if your device has no flashes.")
    }

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
                text = stringResource(ActionUtils.getTitle(state.actionToCreate)),
                style = MaterialTheme.typography.headlineMedium,
            )

            Spacer(modifier = Modifier.height(8.dp))

            OptionsHeaderRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                icon = Icons.Rounded.CameraFront,
                text = stringResource(R.string.action_config_flashlight_choose_side),
            )

            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                RadioButtonText(
                    modifier = Modifier,
                    text = stringResource(R.string.lens_front),
                    isSelected = state.selectedLens == CameraLens.FRONT,
                    onSelected = { onSelectLens(CameraLens.FRONT) },
                    isEnabled = state.lensData.containsKey(CameraLens.FRONT),
                )

                RadioButtonText(
                    modifier = Modifier,
                    text = stringResource(R.string.lens_back),
                    isSelected = state.selectedLens == CameraLens.BACK,
                    onSelected = { onSelectLens(CameraLens.BACK) },
                    isEnabled = state.lensData.containsKey(CameraLens.BACK),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

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

data class ConfigFlashlightActionState(
    val actionToCreate: ActionId,
    val selectedLens: CameraLens,
    val lensData: Map<CameraLens, CameraFlashInfo>,
    val flashStrength: Int = 1,
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

        ConfigFlashlightActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = ConfigFlashlightActionState(
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

        ConfigFlashlightActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = ConfigFlashlightActionState(
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

        ConfigFlashlightActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = ConfigFlashlightActionState(
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
            ),
        )
    }
}
