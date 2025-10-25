package io.github.sds100.keymapper.base.actions

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
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.VolumeStreamStrings
import io.github.sds100.keymapper.base.utils.ui.compose.CheckBoxText
import io.github.sds100.keymapper.base.utils.ui.compose.OptionsHeaderRow
import io.github.sds100.keymapper.base.utils.ui.compose.RadioButtonText
import io.github.sds100.keymapper.system.volume.VolumeStream
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeUpActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.volumeUpActionState != null) {
        VolumeActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                delegate.volumeUpActionState = null
            },
            state = delegate.volumeUpActionState!!,
            title = stringResource(R.string.action_volume_up),
            onSelectStream = {
                delegate.volumeUpActionState = delegate.volumeUpActionState?.copy(volumeStream = it)
            },
            onToggleShowVolumeUi = {
                delegate.volumeUpActionState = delegate.volumeUpActionState?.copy(showVolumeUi = it)
            },
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneConfigVolumeUpClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolumeDownActionBottomSheet(delegate: CreateActionDelegate) {
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (delegate.volumeDownActionState != null) {
        VolumeActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {
                delegate.volumeDownActionState = null
            },
            state = delegate.volumeDownActionState!!,
            title = stringResource(R.string.action_volume_down),
            onSelectStream = {
                delegate.volumeDownActionState = delegate.volumeDownActionState?.copy(volumeStream = it)
            },
            onToggleShowVolumeUi = {
                delegate.volumeDownActionState = delegate.volumeDownActionState?.copy(showVolumeUi = it)
            },
            onDoneClick = {
                scope.launch {
                    sheetState.hide()
                    delegate.onDoneConfigVolumeDownClick()
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeActionBottomSheet(
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    state: VolumeActionBottomSheetState,
    title: String,
    onSelectStream: (VolumeStream?) -> Unit = {},
    onToggleShowVolumeUi: (Boolean) -> Unit = {},
    onDoneClick: () -> Unit = {},
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

            Spacer(modifier = Modifier.height(16.dp))

            OptionsHeaderRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                icon = Icons.AutoMirrored.Outlined.VolumeUp,
                text = stringResource(R.string.action_config_volume_stream),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Default stream option (null means use system default)
            RadioButtonText(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.action_config_volume_stream_default),
                isSelected = state.volumeStream == null,
                onSelected = { onSelectStream(null) },
            )

            // Individual stream options
            VolumeStream.entries.forEach { stream ->
                RadioButtonText(
                    modifier = Modifier.padding(start = 8.dp),
                    text = stringResource(VolumeStreamStrings.getLabel(stream)),
                    isSelected = state.volumeStream == stream,
                    onSelected = { onSelectStream(stream) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OptionsHeaderRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                icon = Icons.Outlined.Visibility,
                text = stringResource(R.string.action_config_volume_options),
            )

            Spacer(modifier = Modifier.height(8.dp))

            CheckBoxText(
                modifier = Modifier.padding(start = 8.dp),
                text = stringResource(R.string.flag_show_volume_dialog),
                isChecked = state.showVolumeUi,
                onCheckedChange = onToggleShowVolumeUi,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

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

data class VolumeActionBottomSheetState(
    val volumeStream: VolumeStream?,
    val showVolumeUi: Boolean,
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewVolumeActionBottomSheet() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        var state by remember {
            mutableStateOf(
                VolumeActionBottomSheetState(
                    volumeStream = VolumeStream.MUSIC,
                    showVolumeUi = true,
                ),
            )
        }

        VolumeActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = state,
            title = stringResource(R.string.action_volume_up),
            onSelectStream = { state = state.copy(volumeStream = it) },
            onToggleShowVolumeUi = { state = state.copy(showVolumeUi = it) },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun PreviewVolumeActionBottomSheetDefaultStream() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            density = LocalDensity.current,
            initialValue = SheetValue.Expanded,
        )

        var state by remember {
            mutableStateOf(
                VolumeActionBottomSheetState(
                    volumeStream = null,
                    showVolumeUi = false,
                ),
            )
        }

        VolumeActionBottomSheet(
            sheetState = sheetState,
            onDismissRequest = {},
            state = state,
            title = stringResource(R.string.action_volume_down),
            onSelectStream = { state = state.copy(volumeStream = it) },
            onToggleShowVolumeUi = { state = state.copy(showVolumeUi = it) },
        )
    }
}
