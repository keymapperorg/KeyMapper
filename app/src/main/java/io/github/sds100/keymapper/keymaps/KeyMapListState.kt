package io.github.sds100.keymapper.keymaps

import io.github.sds100.keymapper.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.util.State

data class KeyMapListState(
    val appBarState: KeyMapAppBarState,
    val listItems: State<List<KeyMapListItemModel>>,
)
