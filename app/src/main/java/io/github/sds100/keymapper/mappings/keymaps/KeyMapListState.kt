package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.constraints.ConstraintMode
import io.github.sds100.keymapper.groups.SubGroupListModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.compose.ComposeChipModel

sealed class KeyMapListState {
    abstract val subGroups: List<SubGroupListModel>
    abstract val listItems: State<List<KeyMapListItemModel>>

    data class Root(
        override val subGroups: List<SubGroupListModel> = emptyList(),
        override val listItems: State<List<KeyMapListItemModel>> = State.Loading,
    ) : KeyMapListState()

    data class Child(
        val groupName: String,
        val constraints: List<ComposeChipModel>,
        val constraintMode: ConstraintMode,
        override val subGroups: List<SubGroupListModel>,
        override val listItems: State<List<KeyMapListItemModel>>,

    ) : KeyMapListState()
}
