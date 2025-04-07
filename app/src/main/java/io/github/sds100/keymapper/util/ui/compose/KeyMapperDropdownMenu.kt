package io.github.sds100.keymapper.util.ui.compose

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.network.HttpMethod

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun KeyMapperDropdownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit = {},
    value: String,
    onValueChanged: (String) -> Unit = {},
) {
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        TextField(
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            value = value,
            onValueChange = onValueChanged,
            readOnly = true,
            label = { Text(stringResource(R.string.action_http_request_method_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )
        ExposedDropdownMenu(
            matchTextFieldWidth = true,
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            for (method in HttpMethod.entries) {
                DropdownMenuItem(
                    text = {
                        Text(
                            method.toString(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = {
                        onValueChanged(method.toString())
                        onExpandedChange(false)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
