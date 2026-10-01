package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.TABLET
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KMBottomSheet(
    title: String,
    posButtonText: String,
    onPosButtonClick: () -> Unit,
    sheetState: SheetState,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    posButtonEnabled: Boolean = true,
    negButtonText: String? = null,
    onNegButtonClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        modifier = modifier
            .widthIn(max = 400.dp)
            // When the bottom sheet fills the whole screen, scrolling pushes the top
            // underneath the status bar, but the content stays below the status bar. This gives
            // it an ugly forehead so make sure the bottom sheet never goes under the status bar.
            .statusBarsPadding(),
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
        // Disable gestures so when scrolling down, the user doesn't accidentally dismiss the
        // bottom sheet. Also, to disable the bottom sheet (not scrolling container) springing
        // up when over-scrolling.
        sheetGesturesEnabled = false,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 16.dp),
                textAlign = TextAlign.Center,
                text = title,
                style = MaterialTheme.typography.titleLarge,
            )

            Spacer(Modifier.height(16.dp))

            content()

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (negButtonText != null) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            if (onNegButtonClick != null) {
                                onNegButtonClick()
                            } else {
                                scope.launch {
                                    sheetState.hide()
                                    onDismissRequest()
                                }
                            }
                        },
                    ) {
                        Text(negButtonText)
                    }

                    Spacer(modifier = Modifier.width(16.dp))
                }

                Button(
                    modifier = Modifier.weight(1f),
                    enabled = posButtonEnabled,
                    onClick = onPosButtonClick,
                ) {
                    Text(posButtonText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true, device = TABLET)
@Composable
private fun KMBottomSheetPreview() {
    KeyMapperTheme {
        KMBottomSheet(
            title = "Title",
            negButtonText = "Cancel",
            posButtonText = "Done",
            onPosButtonClick = {},
            onDismissRequest = {},
            sheetState = SheetState(
                skipPartiallyExpanded = true,
                positionalThreshold = { 0f },
                velocityThreshold = { 0f },
                initialValue = SheetValue.Expanded,
            ),
        ) {
            Text("Content")
        }
    }
}
