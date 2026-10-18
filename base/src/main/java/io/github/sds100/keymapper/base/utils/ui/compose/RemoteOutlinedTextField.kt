package io.github.sds100.keymapper.base.utils.ui.compose

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.VisualTransformation

/** On TV, focusing a field selects it; pressing OK explicitly starts editing. */
@Composable
internal fun RemoteOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
) {
    val isTv = LocalConfiguration.current.uiMode and Configuration.UI_MODE_TYPE_MASK ==
        Configuration.UI_MODE_TYPE_TELEVISION
    val keyboard = LocalSoftwareKeyboardController.current
    var editing by remember { mutableStateOf(false) }
    var activating by remember { mutableStateOf(false) }
    var sawKeyboard by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    LaunchedEffect(editing) {
        if (isTv && editing) {
            // Wait for the editable field's input connection to be installed.
            withFrameNanos { }
            keyboard?.show()
        }
    }
    LaunchedEffect(imeVisible, editing) {
        if (!editing) {
            sawKeyboard = false
        } else if (imeVisible) {
            sawKeyboard = true
        } else if (sawKeyboard) {
            editing = false
        }
    }
    BackHandler(enabled = isTv && editing) {
        editing = false
        keyboard?.hide()
    }

    OutlinedTextField(
        modifier = modifier
            .onFocusChanged {
                if (!it.isFocused && editing) {
                    editing = false
                    keyboard?.hide()
                }
            }
            .onPreviewKeyEvent { event ->
                val activate = event.key == Key.DirectionCenter ||
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter
                if (!isTv || !activate) return@onPreviewKeyEvent false
                if (!editing && event.type == KeyEventType.KeyDown) {
                    activating = true
                    true
                } else if (activating && event.type == KeyEventType.KeyUp) {
                    activating = false
                    editing = true
                    true
                } else {
                    false
                }
            },
        value = value,
        onValueChange = onValueChange,
        label = label,
        supportingText = supportingText,
        singleLine = singleLine,
        maxLines = maxLines,
        readOnly = isTv && !editing,
        keyboardOptions = if (isTv) {
            keyboardOptions.copy(
                showKeyboardOnFocus = false,
            )
        } else {
            keyboardOptions
        },
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        isError = isError,
    )
}
