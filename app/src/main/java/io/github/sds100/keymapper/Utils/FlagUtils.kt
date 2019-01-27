package io.github.sds100.keymapper.Utils

import android.content.Context
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.Views.multiChoiceDialog

/**
 * Created by sds100 on 26/01/2019.
 */

object FlagUtils {

    //DON't CHANGE THESE IDs!!!
    const val FLAG_LONG_PRESS = 0
    const val FLAG_SHOW_VOLUME_DIALOG = 1

    private val FLAG_LABEL_MAP = mapOf(
            FLAG_LONG_PRESS to R.string.flag_long_press,
            FLAG_SHOW_VOLUME_DIALOG to R.string.flag_show_volume_dialog
    )

    fun showFlagDialog(ctx: Context,
                       keyMap: KeyMap,
                       onPosClick: (newItems: List<Triple<String, Int, Boolean>>) -> Unit) {
        val items = sequence {
            for (item in FLAG_LABEL_MAP) {
                val flag = item.key
                val label = item.value

                yield(Triple(ctx.str(label), flag, keyMap.flags.contains(flag)))
            }
        }.toMutableList()

        ctx.multiChoiceDialog(
                titleRes = R.string.dialog_title_flags,
                items = items,
                onPosClick = { onPosClick(it) }
        )
    }
}