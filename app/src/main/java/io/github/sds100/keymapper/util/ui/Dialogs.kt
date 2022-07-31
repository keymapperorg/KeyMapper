package io.github.sds100.keymapper.util.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun CustomDialog(
    title: String,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    onDismissRequest: () -> Unit = {},
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraLarge) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    modifier = Modifier
                        .wrapContentSize(),
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .padding(top = 16.dp)
                ) {
                    content()
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 400)
@Composable
private fun Preview() {
    MaterialTheme {
        CustomDialog(
            title = "Title",
            confirmButton = {
                TextButton(onClick = {}) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
            }
        ) {
            LazyColumn {
                item {
                    RadioButtonWithText(
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = true,
                        text = "Button 1"
                    )
                }
                item {
                    RadioButtonWithText(
                        modifier = Modifier.fillMaxWidth(),
                        isSelected = false,
                        text = "Button 2"
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 400)
@Composable
private fun PreviewNormalDialog() {
    MaterialTheme {
        AlertDialog(onDismissRequest = { },
            confirmButton = {
                TextButton(onClick = {}) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { }) {
                    Text("Cancel")
                }
            },
            title = { Text("Title") },
            text = { Text("I am a dialog text. I am here to fill this gap so I can show a good preview.") }
        )
    }
}
