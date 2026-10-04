package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Done
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun EditableAppBarTextField(
    modifier: Modifier = Modifier,
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit = {},
    placeholder: String,
    isEditing: Boolean,
    onConfirmClick: () -> Unit,
    onEditClick: () -> Unit = {},
    error: String? = null,
    confirmContentDescription: String,
    editContentDescription: String,
) {
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        focusRequester.requestFocus()
    }

    AnimatedContent(modifier = modifier, targetState = isEditing) { isEditing ->
        Row(
            Modifier
                .height(IntrinsicSize.Min)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            val interactionSource = remember { MutableInteractionSource() }

            // Use a custom text field so the content padding can be customised.
            BasicTextField(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .height(IntrinsicSize.Max)
                    .then(
                        if (isEditing) {
                            Modifier.weight(1f)
                        } else {
                            Modifier.weight(1f, fill = false)
                        },
                    ),
                value = value,
                onValueChange = onValueChange,
                textStyle = MaterialTheme.typography.titleLarge.copy(
                    color = LocalContentColor.current,
                ),
                enabled = isEditing,
                keyboardActions = KeyboardActions(onDone = { onConfirmClick() }),
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Done,
                    showKeyboardOnFocus = true,
                ),
                singleLine = true,
                maxLines = 1,
                interactionSource = interactionSource,
            ) { innerTextField ->
                @OptIn(ExperimentalMaterial3Api::class)
                OutlinedTextFieldDefaults.DecorationBox(
                    value = value.text,
                    placeholder = {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            color = OutlinedTextFieldDefaults.colors().disabledPlaceholderColor,
                            overflow = TextOverflow.Clip,
                            softWrap = false,
                        )
                    },
                    innerTextField = {
                        Box(
                            Modifier
                                .width(IntrinsicSize.Min)
                                .height(48.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) { innerTextField() }
                    },
                    singleLine = true,
                    colors = if (isEditing) {
                        OutlinedTextFieldDefaults.colors()
                    } else {
                        OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color.Transparent,
                            focusedBorderColor = Color.Transparent,
                            disabledBorderColor = Color.Transparent,
                            disabledTextColor = LocalContentColor.current,
                        )
                    },
                    isError = error != null,
                    enabled = isEditing,
                    supportingText = if (error == null) {
                        null
                    } else {
                        { Text(error, maxLines = 1) }
                    },
                    visualTransformation = VisualTransformation.None,
                    interactionSource = interactionSource,
                    contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                        top = 0.dp,
                        bottom = 0.dp,
                        end = 4.dp,
                        start = 8.dp,
                    ),
                )
            }

            if (isEditing) {
                IconButton(onClick = onConfirmClick) {
                    Icon(
                        Icons.Rounded.Done,
                        contentDescription = confirmContentDescription,
                    )
                }
            } else {
                IconButton(onClick = onEditClick) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = editContentDescription,
                    )
                }
            }
        }
    }
}
