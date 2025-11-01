package io.github.sds100.keymapper.base.utils.ui.compose

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

@Composable
fun ButtonDefaults.filledTonalButtonColorsError(): ButtonColors {
    return ButtonDefaults.filledTonalButtonColors(
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
    )
}
