package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.groups.GroupListItemModel
import io.github.sds100.keymapper.home.HomeWarningListItem
import io.github.sds100.keymapper.home.SelectedKeyMapsEnabled
import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel

sealed class KeyMapAppBarState {
    data class RootGroup(
        val subGroups: List<GroupListItemModel> = emptyList(),
        val warnings: List<HomeWarningListItem> = emptyList(),
        val isPaused: Boolean = false,
    ) : KeyMapAppBarState()

    data class ChildGroup(
        val groupName: String,
        val constraints: List<ComposeChipModel>,
        val constraintMode: ConstraintMode,
        val parentConstraintCount: Int,
        val subGroups: List<GroupListItemModel>,
        val breadcrumbs: List<GroupListItemModel>,
        val isEditingGroupName: Boolean,
        val isNewGroup: Boolean,
    ) : KeyMapAppBarState()

    data class Selecting(
        val selectionCount: Int,
        val selectedKeyMapsEnabled: SelectedKeyMapsEnabled,
        val isAllSelected: Boolean,
        val groups: List<GroupListItemModel>,
        val breadcrumbs: List<GroupListItemModel>,
        val showThisGroup: Boolean,
    ) : KeyMapAppBarState()
}
