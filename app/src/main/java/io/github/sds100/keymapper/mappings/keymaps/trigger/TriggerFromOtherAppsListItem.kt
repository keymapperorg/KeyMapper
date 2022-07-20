package io.github.sds100.keymapper.mappings.keymaps.trigger

import io.github.sds100.keymapper.util.ui.ListItem

data class TriggerFromOtherAppsListItem(
    override val id: String,
    val isEnabled: Boolean,
    val keyMapUid: String,
    val label: String,
    val isCreateLauncherShortcutButtonEnabled: Boolean
) : ListItem