package io.github.sds100.keymapper.base.constraints

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.utils.ui.compose.KeyMapperSegmentedButtonRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfigNotificationConstraintScreen(
    modifier: Modifier = Modifier,
    viewModel: ConfigNotificationConstraintViewModel,
) {
    ConfigNotificationConstraintScreen(
        modifier = modifier,
        field = viewModel.selectedField,
        matchMode = viewModel.matchMode,
        value = viewModel.value,
        appName = viewModel.selectedAppName,
        isValid = viewModel.isValid,
        onSelectField = viewModel::onSelectField,
        onSelectMatchMode = viewModel::onSelectMatchMode,
        onValueChange = viewModel::onValueChange,
        onChooseAppClick = viewModel::onChooseAppClick,
        onDoneClick = viewModel::onDoneClick,
        onNavigateBack = viewModel::onNavigateBack,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfigNotificationConstraintScreen(
    modifier: Modifier = Modifier,
    field: NotificationField = NotificationField.TITLE,
    matchMode: TextMatchMode = TextMatchMode.CONTAINS,
    value: String = "",
    appName: String? = null,
    isValid: Boolean = false,
    onSelectField: (NotificationField) -> Unit = {},
    onSelectMatchMode: (TextMatchMode) -> Unit = {},
    onValueChange: (String) -> Unit = {},
    onChooseAppClick: () -> Unit = {},
    onDoneClick: () -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.constraint_notification_screen_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_go_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            // Grows by the height of the keyboard so that the content above it shrinks and the
            // focused text field is scrolled into view.
            BottomAppBar(
                modifier = Modifier.imePadding(),
                actions = {
                    Spacer(modifier = Modifier.weight(1f))

                    OutlinedButton(onClick = onNavigateBack) {
                        Text(stringResource(R.string.neg_cancel))
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(onClick = onDoneClick, enabled = isValid) {
                        Text(stringResource(R.string.pos_done))
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = innerPadding.calculateBottomPadding(),
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                )
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader(stringResource(R.string.constraint_notification_field_header))

            KeyMapperSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth(),
                buttonStates = NotificationField.entries.map {
                    it to stringResource(ConstraintUtils.getNotificationFieldLabel(it))
                },
                selectedState = field,
                onStateSelected = onSelectField,
            )

            Text(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(R.string.constraint_notification_sensitive_content_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )

            if (field.isFreeText) {
                SectionHeader(
                    stringResource(R.string.constraint_notification_match_mode_header),
                )

                KeyMapperSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth(),
                    buttonStates = TextMatchMode.entries.map {
                        it to stringResource(ConstraintUtils.getTextMatchModeLabel(it))
                    },
                    selectedState = matchMode,
                    onStateSelected = onSelectMatchMode,
                )

                OutlinedTextField(
                    modifier = Modifier.fillMaxWidth(),
                    value = value,
                    onValueChange = onValueChange,
                    label = {
                        val text = when (field) {
                            NotificationField.TEXT -> stringResource(
                                R.string.constraint_notification_value_hint_text,
                            )

                            else -> stringResource(
                                R.string.constraint_notification_value_hint_title,
                            )
                        }
                        Text(text)
                    },
                    singleLine = true,
                )
            } else {
                SectionHeader(stringResource(R.string.constraint_notification_value_header))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (appName != null) {
                        Text(
                            text = appName,
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }

                    Spacer(Modifier.weight(1f))

                    FilledTonalButton(onClick = onChooseAppClick) {
                        Text(stringResource(R.string.constraint_notification_choose_app))
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
    )
}

@Preview
@Composable
private fun TitlePreview() {
    KeyMapperTheme {
        ConfigNotificationConstraintScreen(
            field = NotificationField.TITLE,
            value = "New message",
            isValid = true,
        )
    }
}

@Preview
@Composable
private fun AppPreview() {
    KeyMapperTheme {
        ConfigNotificationConstraintScreen(
            field = NotificationField.PACKAGE,
            value = "com.termux",
            appName = "Termux",
            isValid = true,
        )
    }
}
