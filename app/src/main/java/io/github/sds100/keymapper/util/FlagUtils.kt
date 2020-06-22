package io.github.sds100.keymapper.util

import android.content.Context
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.KeyMap
import splitties.bitflags.hasFlag
import splitties.bitflags.minusFlag
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 08/03/2020.
 */

fun Int.buildKeymapFlagsDescription(ctx: Context): String = buildString {
    KeyMap.getFlagLabelList(ctx, this@buildKeymapFlagsDescription).forEachIndexed { index, label ->
        if (index > 0) {
            append(" ${ctx.str(R.string.interpunct)} ")
        }

        append(label)
    }
}

fun Int.toggleFlag(flag: Int): Int =
    if (this.hasFlag(flag)) {
        this.minusFlag(flag)
    } else {
        this.withFlag(flag)
    }