package io.github.sds100.keymapper.base.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.trigger.TriggerError
import io.github.sds100.keymapper.base.utils.ui.compose.CustomDialog
import io.github.sds100.keymapper.common.utils.KMError

data class FixErrorsDialogState(val errors: List<KeyMapError>)

@Composable
fun FixErrorsDialog(
    state: FixErrorsDialogState,
    onFixClick: (KeyMapError) -> Unit,
    onDismissRequest: () -> Unit,
) {
    CustomDialog(
        title = stringResource(R.string.dialog_title_home_fix_error),
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.pos_done))
            }
        },
        onDismissRequest = onDismissRequest,
    ) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            ErrorSection(
                title = stringResource(R.string.trigger_header),
                errors = state.errors.filterIsInstance<KeyMapError.Trigger>(),
                onFixClick = onFixClick,
            )
            ErrorSection(
                title = stringResource(R.string.action_list_header),
                errors = state.errors.filterIsInstance<KeyMapError.Action>(),
                onFixClick = onFixClick,
            )
            ErrorSection(
                title = stringResource(R.string.constraint_list_header),
                errors = state.errors.filterIsInstance<KeyMapError.Constraint>(),
                onFixClick = onFixClick,
            )
        }
    }
}

@Composable
private fun ErrorSection(
    title: String,
    errors: List<KeyMapError>,
    onFixClick: (KeyMapError) -> Unit,
) {
    if (errors.isEmpty()) {
        return
    }

    Text(
        modifier = Modifier.padding(horizontal = 16.dp),
        text = title,
        style = MaterialTheme.typography.titleSmall,
    )

    for (item in errors) {
        ErrorListItem(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            text = item.message,
            isFixable = item.isFixable,
            onFixClick = { onFixClick(item) },
        )
    }
}

@Composable
private fun ErrorListItem(
    modifier: Modifier = Modifier,
    text: String,
    isFixable: Boolean,
    onFixClick: () -> Unit = {},
) {
    Row(modifier = modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
            modifier = Modifier.weight(1f),
            text = text,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(modifier = Modifier.width(8.dp))
        FilledTonalButton(
            onClick = onFixClick,
            enabled = isFixable,
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError,
            ),
        ) {
            Text(text = stringResource(R.string.button_fix))
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun PreviewFixErrorsDialog() {
    KeyMapperTheme {
        FixErrorsDialog(
            state = FixErrorsDialogState(
                errors = listOf(
                    KeyMapError.Trigger(
                        TriggerError.DND_ACCESS_DENIED,
                        "Grant Key Mapper access to Do Not Disturb",
                    ),
                    KeyMapError.Action(
                        KMError.NoCompatibleImeChosen,
                        "Choose a compatible keyboard",
                        isFixable = true,
                    ),
                    KeyMapError.Constraint(
                        KMError.AppNotFound("com.example.app"),
                        "App not found",
                        isFixable = false,
                    ),
                ),
            ),
            onFixClick = {},
            onDismissRequest = {},
        )
    }
}
