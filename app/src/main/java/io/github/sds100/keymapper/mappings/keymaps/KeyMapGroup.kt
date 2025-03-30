package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.groups.Group
import io.github.sds100.keymapper.util.State

data class KeyMapGroup(
    val group: Group?,
    val subGroups: List<Group>,
    val parents: List<Group>,
    val keyMaps: State<List<KeyMap>>,
)
