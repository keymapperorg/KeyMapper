package io.github.sds100.keymapper.util

import android.util.Log
import io.github.sds100.keymapper.data.model.FlagModel
import io.github.sds100.keymapper.data.model.KeyMap
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag
import splitties.resources.appStr

/**
 * Created by sds100 on 08/03/2020.
 */

object FlagUtils {
    fun createKeymapFlagModels(flags: Int): List<FlagModel> = sequence {
        KeyMap.KEYMAP_FLAG_LABEL_MAP.keys.forEach { flag ->
            if (flags.hasFlag(flag)) {
                val text = appStr(KeyMap.KEYMAP_FLAG_LABEL_MAP.getValue(flag))
                val drawableId = KeyMap.KEYMAP_FLAG_ICON_MAP.getValue(flag)
                yield(FlagModel(text, drawableId))
            }
        }
    }.toList()
}

fun Int.toggleFlag(flag: Int): Int =
    if (this.hasFlag(flag)) {
        this.minusFlag(flag)
    } else {
        this.withFlag(flag)
    }