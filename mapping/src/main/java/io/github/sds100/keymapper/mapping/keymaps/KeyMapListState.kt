package io.github.sds100.keymapper.mapping.keymaps

import io.github.sds100.keymapper.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.common.util.state.State

data class KeyMapListState(
    val appBarState: KeyMapAppBarState,
    val listItems: State<List<KeyMapListItemModel>>,
    val showCreateKeyMapTapTarget: Boolean = false,
)
