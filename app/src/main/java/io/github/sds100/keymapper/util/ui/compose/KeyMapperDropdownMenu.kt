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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun <T> KeyMapperDropdownMenu(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit = {},
    label: String,
    selectedValue: T,
    values: List<Pair<T, String>>,
    onValueChanged: (T) -> Unit = {},
) {
    ExposedDropdownMenuBox(
        modifier = modifier,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        TextField(
            modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable),
            value = values.single { it.first == selectedValue }.second,
            onValueChange = { newValue ->
                onValueChanged(values.single { it.second == newValue }.first)
            },
            readOnly = true,
            label = { Text(text = label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
        )

        ExposedDropdownMenu(
            matchTextFieldWidth = true,
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) },
        ) {
            for ((value, valueText) in values) {
                DropdownMenuItem(
                    text = {
                        Text(
                            valueText,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    },
                    onClick = {
                        onValueChanged(value)
                        onExpandedChange(false)
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
    }
}
