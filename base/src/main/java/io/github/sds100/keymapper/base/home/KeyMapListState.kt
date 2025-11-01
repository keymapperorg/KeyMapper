package io.github.sds100.keymapper.base.home

import io.github.sds100.keymapper.base.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.common.utils.State

data class KeyMapListState(
    val appBarState: KeyMapAppBarState,
    val listItems: State<List<KeyMapListItemModel>>,
    val showCreateKeyMapTapTarget: Boolean,
)
