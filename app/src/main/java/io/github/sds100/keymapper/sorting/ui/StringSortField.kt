package io.github.sds100.keymapper.sorting.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.sorting.SortField

@Composable
fun stringSortField(sortField: SortField): String {
    return when (sortField) {
        SortField.TRIGGER -> stringResource(R.string.trigger_header)
        SortField.ACTIONS -> stringResource(R.string.action_list_header)
        SortField.CONSTRAINTS -> stringResource(R.string.constraint_list_header)
        SortField.OPTIONS -> stringResource(R.string.option_list_header)
    }
}
