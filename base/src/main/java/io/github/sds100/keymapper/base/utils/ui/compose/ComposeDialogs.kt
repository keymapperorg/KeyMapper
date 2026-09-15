package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDialog(
    title: String? = null,
    text: String? = null,
    confirmButton: @Composable () -> Unit = {},
    dismissButton: (@Composable () -> Unit)? = null,
    onDismissRequest: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
    ) {
        CustomDialogContent(title, text, confirmButton, dismissButton, content)
    }
}

@Composable
fun CustomDialogContent(
    title: String?,
    text: String? = null,
    confirmButton: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    content: @Composable (BoxScope.() -> Unit),
) {
    Surface(
        color = AlertDialogDefaults.containerColor,
        shape = AlertDialogDefaults.shape,
        tonalElevation = AlertDialogDefaults.TonalElevation,
    ) {
        Column {
            if (title != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp),
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = AlertDialogDefaults.titleContentColor,
                )
            }

            if (text != null) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp),
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = AlertDialogDefaults.textContentColor,
                )
            }

//                HorizontalDivider()
            Box(Modifier.weight(1f, fill = false), content = content)
//                HorizontalDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.End)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp, alignment = Alignment.End),
            ) {
                if (dismissButton != null) {
                    dismissButton()
                }
                confirmButton()
            }
        }
    }
}
