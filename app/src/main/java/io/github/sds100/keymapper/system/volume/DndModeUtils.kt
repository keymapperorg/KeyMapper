package io.github.sds100.keymapper.system.volume

import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 23/03/2021.
 */
object DndModeUtils {
    fun getLabel(dndMode: DndMode) = when (dndMode) {
        DndMode.ALARMS -> R.string.dnd_mode_alarms
        DndMode.PRIORITY -> R.string.dnd_mode_priority
        DndMode.NONE -> R.string.dnd_mode_none
    }
}
