package io.github.sds100.keymapper.keymaps

import io.github.sds100.keymapper.groups.Group
import io.github.sds100.keymapper.common.state.State

data class KeyMapGroup(
    val group: Group?,
    val subGroups: List<Group>,
    val parents: List<Group>,
    val keyMaps: State<List<KeyMap>>,
)
