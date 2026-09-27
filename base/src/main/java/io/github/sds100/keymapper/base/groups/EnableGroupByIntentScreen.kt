package io.github.sds100.keymapper.base.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.sds100.keymapper.base.IntentApi
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.compose.KeyMapperTheme
import io.github.sds100.keymapper.base.keymaps.AdbSection
import io.github.sds100.keymapper.base.keymaps.IntentScreen
import io.github.sds100.keymapper.base.keymaps.buildAdbCommand
import io.github.sds100.keymapper.base.utils.ui.compose.CodeBlock
import io.github.sds100.keymapper.base.utils.ui.compose.OptionPageButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnableGroupByIntentScreen(
    modifier: Modifier = Modifier,
    viewModel: EnableGroupByIntentViewModel,
) {
    val breadcrumbs by viewModel.breadcrumbs.collectAsStateWithLifecycle()
    val groups by viewModel.groups.collectAsStateWithLifecycle()
    val selectedGroupUid by viewModel.selectedGroupUid.collectAsStateWithLifecycle()
    val isGroupPickerVisible by viewModel.isGroupPickerVisible.collectAsStateWithLifecycle()

    if (isGroupPickerVisible) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        SelectGroupBottomSheet(
            sheetState = sheetState,
            breadcrumbs = breadcrumbs,
            groups = groups,
            canConfirm = selectedGroupUid != null,
            onGroupClick = viewModel::onGroupClick,
            onDismissRequest = viewModel::onDismissGroupPicker,
        )
    }

    EnableGroupByIntentScreen(
        modifier = modifier,
        selectedGroupUid = selectedGroupUid,
        onSelectGroupClick = viewModel::onSelectGroupClick,
        onBackClick = viewModel::onBackClick,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnableGroupByIntentScreen(
    modifier: Modifier = Modifier,
    selectedGroupUid: String?,
    onSelectGroupClick: () -> Unit = {},
    onBackClick: () -> Unit = {},
) {
    val packageName = LocalContext.current.packageName
    val clipboardLabel = stringResource(R.string.intent_screen_clipboard_label)

    IntentScreen(
        modifier = modifier,
        title = stringResource(R.string.enable_disable_group_by_intent_title),
        text = stringResource(R.string.intent_screen_enable_group_message),
        onBackClick = onBackClick,
    ) {
        Text(
            text = stringResource(R.string.intent_screen_target_explanation),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_package),
            code = packageName,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_class),
            code = IntentApi.GROUP_RECEIVER_CLASS,
            clipboardLabel = clipboardLabel,
        )

        Text(
            text = stringResource(R.string.intent_screen_action_explanation),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_action_enable),
            code = IntentApi.ACTION_ENABLE_GROUP,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_action_disable),
            code = IntentApi.ACTION_DISABLE_GROUP,
            clipboardLabel = clipboardLabel,
        )

        CodeBlock(
            label = stringResource(R.string.intent_screen_label_action_toggle),
            code = IntentApi.ACTION_TOGGLE_GROUP,
            clipboardLabel = clipboardLabel,
        )

        Text(
            text = stringResource(R.string.intent_screen_extras_explanation_group),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        OptionPageButton(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(R.string.select_group_by_intent_title),
            text = stringResource(R.string.select_group_by_intent_summary),
            icon = Icons.Rounded.FolderOpen,
            onClick = onSelectGroupClick,
        )

        if (selectedGroupUid == null) {
            Text(
                text = stringResource(R.string.intent_screen_select_group_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            CodeBlock(
                label = stringResource(R.string.intent_screen_label_extra_name),
                code = IntentApi.EXTRA_GROUP_UID,
                clipboardLabel = clipboardLabel,
            )

            CodeBlock(
                label = stringResource(R.string.intent_screen_label_extra_value_group),
                code = selectedGroupUid,
                clipboardLabel = clipboardLabel,
            )

            AdbSection(
                command = buildAdbCommand(
                    packageName = packageName,
                    receiverClass = IntentApi.GROUP_RECEIVER_CLASS,
                    action = IntentApi.ACTION_TOGGLE_GROUP,
                    extraName = IntentApi.EXTRA_GROUP_UID,
                    extraValue = selectedGroupUid,
                ),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectGroupBottomSheet(
    sheetState: SheetState,
    breadcrumbs: List<GroupListItemModel>,
    groups: List<GroupListItemModel>,
    canConfirm: Boolean,
    onGroupClick: (String?) -> Unit = {},
    onDismissRequest: () -> Unit = {},
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                text = stringResource(R.string.select_group_by_intent_title),
                style = MaterialTheme.typography.titleLarge,
            )

            GroupBreadcrumbRow(groups = breadcrumbs, onGroupClick = onGroupClick)

            GroupRow(
                modifier = Modifier.fillMaxWidth(),
                groups = groups,
                onGroupClick = onGroupClick,
                showNewGroup = false,
                showThisGroupButton = canConfirm,
                onThisGroupClick = onDismissRequest,
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Preview(heightDp = 1200)
@Composable
private fun EnableGroupByIntentScreenPreview() {
    KeyMapperTheme {
        EnableGroupByIntentScreen(
            selectedGroupUid = null,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
private fun SelectGroupBottomSheetPreview() {
    KeyMapperTheme {
        val sheetState = SheetState(
            skipPartiallyExpanded = true,
            positionalThreshold = { 0f },
            velocityThreshold = { 0f },
            initialValue = SheetValue.Expanded,
        )

        SelectGroupBottomSheet(
            sheetState = sheetState,
            breadcrumbs = listOf(GroupListItemModel(uid = "1", name = "Lockscreen")),
            groups = listOf(GroupListItemModel(uid = "2", name = "Key Mapper")),
            canConfirm = true,
        )
    }
}
