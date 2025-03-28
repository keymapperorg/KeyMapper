package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyMapListItemModel
import io.github.sds100.keymapper.util.State

data class KeyMapListState(
    val appBarState: KeyMapAppBarState,
    val listItems: State<List<KeyMapListItemModel>>,
)
