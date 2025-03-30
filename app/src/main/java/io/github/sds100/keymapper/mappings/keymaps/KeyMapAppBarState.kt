package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.groups.SubGroupListModel
import io.github.sds100.keymapper.home.HomeWarningListItem
import io.github.sds100.keymapper.home.SelectedKeyMapsEnabled
import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel

sealed class KeyMapAppBarState {
    data class RootGroup(
        val subGroups: List<SubGroupListModel> = emptyList(),
        val warnings: List<HomeWarningListItem> = emptyList(),
        val isPaused: Boolean = false,
    ) : KeyMapAppBarState()

    data class ChildGroup(
        val groupName: String,
        val constraints: List<ComposeChipModel>,
        val constraintMode: ConstraintMode,
        val subGroups: List<SubGroupListModel>,
        val parentGroups: List<SubGroupListModel>,

    ) : KeyMapAppBarState()

    data class Selecting(
        val selectionCount: Int,
        val selectedKeyMapsEnabled: SelectedKeyMapsEnabled,
        val isAllSelected: Boolean,
    ) : KeyMapAppBarState()
}
