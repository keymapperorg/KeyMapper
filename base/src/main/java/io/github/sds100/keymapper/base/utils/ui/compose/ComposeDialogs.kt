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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import kotlinx.coroutines.launch

@Composable
fun TextFieldDialog(
    title: String,
    submitButtonText: String,
    initialText: String,
    hint: String? = null,
    canBeEmpty: Boolean = false,
    /**
     * Returns an error message.
     */
    onSubmitClick: suspend (newText: String) -> String? = { null },
    onDismissRequest: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }
    var textFieldValue: TextFieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialText,
                selection = TextRange(initialText.length),
            ),
        )
    }

    var error: String? by remember { mutableStateOf(null) }

    val isError by remember { derivedStateOf { error != null } }

    val emptyErrorText = stringResource(R.string.error_cant_be_empty)

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val submit = {
        scope.launch {
            error = onSubmitClick(textFieldValue.text)

            if (error == null) {
                onDismissRequest()
            }
        }
        Unit
    }

    CustomDialog(
        title = title,
        confirmButton = {
            TextButton(
                onClick = submit,
                enabled = !isError,
            ) {
                Text(submitButtonText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.neg_cancel))
            }
        },
        onDismissRequest = onDismissRequest,
    ) {
        TextField(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .focusRequester(focusRequester),
            value = textFieldValue,
            onValueChange = {
                error = null
                textFieldValue = it

                if (!canBeEmpty && textFieldValue.text.isBlank()) {
                    error = emptyErrorText
                }
            },
            placeholder = if (hint == null) {
                null
            } else {
                { Text(hint) }
            },
            singleLine = true,
            maxLines = 1,
            isError = isError,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (!isError) {
                        submit()
                    }
                },
            ),
            supportingText = {
                if (error != null) Text(error!!)
            },
        )
    }
}

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

            Spacer(modifier = Modifier.height(8.dp))
//                HorizontalDivider()
            Box(Modifier.weight(1f, fill = false), content = content)
//                HorizontalDivider()

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.End)
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
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

@Preview(widthDp = 800, heightDp = 400)
@Composable
private fun TextFieldDialogPreview() {
    KeyMapperTheme {
        TextFieldDialog(
            title = "Title",
            submitButtonText = "Submit",
            initialText = "Some dialog text",
        )
    }
}

@Preview(widthDp = 800, heightDp = 400)
@Composable
private fun TextFieldDialogHintPreview() {
    KeyMapperTheme {
        TextFieldDialog(
            title = "Title",
            submitButtonText = "Submit",
            initialText = "",
            hint = "Some placeholder text",
        )
    }
}
