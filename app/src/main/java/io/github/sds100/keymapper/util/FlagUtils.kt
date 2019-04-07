package io.github.sds100.keymapper.util

import android.content.Context
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_FLAG_NOT_FOUND
import io.github.sds100.keymapper.view.multiChoiceDialog

/**
 * Created by sds100 on 26/01/2019.
 */

object FlagUtils {

    //DON'T CHANGE THESE AND THEY MUST BE POWERS OF 2!!
    const val FLAG_LONG_PRESS = 1
    const val FLAG_SHOW_VOLUME_UI = 2
    const val FLAG_VIBRATE = 4

    private val FLAG_LABEL_MAP = mapOf(
            FLAG_LONG_PRESS to R.string.flag_long_press,
            FLAG_SHOW_VOLUME_UI to R.string.flag_show_volume_dialog,
            FLAG_VIBRATE to R.string.flag_vibrate
    )

    fun showFlagDialog(ctx: Context,
                       keyMap: KeyMap,
                       onPosClick: (selectedItems: List<Triple<String, Int, Boolean>>) -> Unit) {

        val items = sequence {
            for (item in FLAG_LABEL_MAP) {
                val flag = item.key
                val label = item.value

                //only show the volume-ui flag if the action is volume-related
                if ((!keyMap.action.isVolumeAction
                                || keyMap.action?.data == SystemAction.VOLUME_SHOW_DIALOG)
                        && flag == FLAG_SHOW_VOLUME_UI) {
                    continue
                }

                //1st = label for the flag, 2nd = the flag, 3rd = whether the flag should be checked
                yield(Triple(ctx.str(label), flag, containsFlag(keyMap.flags, flag)))
            }
        }.toMutableList()

        ctx.multiChoiceDialog(
                titleRes = R.string.dialog_title_flags,
                items = items,
                onPosClick = { onPosClick(it) }
        )
    }

    fun getFlagLabel(flagId: Int) = FLAG_LABEL_MAP[flagId].result(ERROR_CODE_FLAG_NOT_FOUND, flagId.toString())

    fun getFlags(flagSet: Int) = sequence {
        FLAG_LABEL_MAP.keys.forEach { flag ->
            if (containsFlag(flagSet, flag)) {
                yield(flag)
            }
        }
    }.toList()
}

/**
 * @return a new flag set which contains the flag
 */
fun addFlag(flagSet: Int, flag: Int) = flagSet or flag

/**
 * @return a new flag set which doesn't contain the flag
 */
fun removeFlag(flagSet: Int, flag: Int): Int {
    //same as flagSet &(~flag)
    return flagSet and flag.inv()
}

fun containsFlag(flagSet: Int, flag: Int) = (flagSet and flag) == flag