package io.github.sds100.keymapper.base.utils

import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.system.volume.RingerMode

object RingerModeStrings {
    fun getLabel(ringerMode: RingerMode) = when (ringerMode) {
        RingerMode.NORMAL -> R.string.ringer_mode_normal
        RingerMode.VIBRATE -> R.string.ringer_mode_vibrate
        RingerMode.SILENT -> R.string.ringer_mode_silent
    }
}
