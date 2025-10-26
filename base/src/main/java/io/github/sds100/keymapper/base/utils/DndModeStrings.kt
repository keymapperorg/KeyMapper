package io.github.sds100.keymapper.base.utils

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.system.volume.DndMode

object DndModeStrings {
    fun getLabel(dndMode: DndMode) =
        when (dndMode) {
            DndMode.ALARMS -> R.string.dnd_mode_alarms
            DndMode.PRIORITY -> R.string.dnd_mode_priority
            DndMode.NONE -> R.string.dnd_mode_none
        }
}
