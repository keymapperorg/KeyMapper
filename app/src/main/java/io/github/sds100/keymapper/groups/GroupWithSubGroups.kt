package io.github.sds100.keymapper.groups

data class GroupWithSubGroups(
    val group: Group?,
    val subGroups: List<Group>,
)
