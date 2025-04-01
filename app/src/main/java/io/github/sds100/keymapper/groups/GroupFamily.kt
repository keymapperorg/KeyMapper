package io.github.sds100.keymapper.groups

data class GroupFamily(
    val group: Group?,
    val children: List<Group>,
    val parents: List<Group>,
)
