package io.github.sds100.keymapper.mappings.keymaps.trigger

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.compose.KeyMapperTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedTriggersBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    viewModel: ConfigTriggerViewModel,
    sheetState: SheetState,
) {
    AdvancedTriggersBottomSheet(
        modifier,
        onDismissRequest,
        sheetState,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AdvancedTriggersBottomSheet(
    modifier: Modifier = Modifier,
    onDismissRequest: () -> Unit,
    sheetState: SheetState,
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        // Hide drag handle because other bottom sheets don't have it
        dragHandle = {},
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            text = stringResource(R.string.advanced_triggers_sheet_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.advanced_triggers_sheet_text),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            text = stringResource(R.string.purchasing_not_implemented_bottom_sheet_text),
            fontStyle = FontStyle.Italic,
        )

        Spacer(modifier = Modifier.height(8.dp))

        val uriHandler = LocalUriHandler.current
        val googlePlayUrl = stringResource(R.string.url_play_store_listing)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                modifier = Modifier,
                onClick = {
                    scope.launch {
                        sheetState.hide()
                        onDismissRequest()
                    }
                },
            ) {
                Text(stringResource(R.string.neg_cancel))
            }

            FilledTonalButton(
                modifier = Modifier,
                onClick = {
                    scope.launch {
                        uriHandler.openUri(googlePlayUrl)
                    }
                },
            ) {
                Text(stringResource(R.string.purchasing_download_key_mapper_from_google_play))
            }
        }

        Spacer(Modifier.height(16.dp))
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

        AdvancedTriggersBottomSheet(
            onDismissRequest = {},
            sheetState = sheetState,
        )
    }
}
