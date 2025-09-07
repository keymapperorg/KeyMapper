package io.github.sds100.keymapper.base.home

import io.github.sds100.keymapper.base.groups.Group
import io.github.sds100.keymapper.base.keymaps.KeyMap
import io.github.sds100.keymapper.common.utils.State

data class KeyMapGroup(
    val group: Group?,
    val subGroups: List<Group>,
    val parents: List<Group>,
    val keyMaps: State<List<KeyMap>>,
)
