package io.github.sds100.keymapper.util.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun CustomDialog(
    onDismissRequest: () -> Unit,
    title: String,
    confirmButton: @Composable () -> Unit,
    dismissButton: @Composable () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(color = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium) {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                Text(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .height(64.dp)
                        .wrapContentSize()
                        .padding(start = 24.dp, end = 24.dp),
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )
                Box(
                    Modifier
                        .weight(1f, fill = false)
                        .padding(start = 24.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    content()
                }
                Row(horizontalArrangement = Arrangement.End) {
                    dismissButton()
                    confirmButton()
                }
            }
        }
    }
}