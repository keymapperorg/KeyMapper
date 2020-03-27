package io.github.sds100.keymapper.data.model

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ActionType
import splitties.bitflags.hasFlag
import splitties.resources.appStr
import java.io.Serializable

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [data] The information required to perform the action. E.g if the type is [ActionType.APP],
 * the data will be the package name of the application
 *
 * Different Types of actions:
 * - Apps
 * - App shortcuts
 * - Keycode
 * - Key
 * - Insert a block of text
 * - System actions/settings
 */
data class Action(
    val type: ActionType,

    /**
     * How each action type saves data:
     *
     * - Apps: package name
     * - App shortcuts: the intent for the shortcut as a parsed URI
     * - Keycode: the keycode
     * - Key: the keycode of the key
     * - Block of text: text to insert
     * - System action: the system action id
     */
    val data: String,
    val extras: MutableList<Extra> = mutableListOf(),
    var flags: Int = 0

) : Serializable {
    companion object {
        const val ACTION_FLAG_SHOW_VOLUME_UI = 1

        val ACTION_FLAG_LABEL_MAP = mapOf(
            ACTION_FLAG_SHOW_VOLUME_UI to R.string.flag_show_volume_dialog
        )

        fun appAction(packageName: String): Action {
            return Action(ActionType.APP, packageName)
        }
    }

    constructor(type: ActionType, data: String, extra: Extra) : this(type, data, mutableListOf(extra))

    fun getFlagLabelList(): List<String> = sequence {
        ACTION_FLAG_LABEL_MAP.keys.forEach { flag ->
            if (flags.hasFlag(flag)) {
                yield(appStr(ACTION_FLAG_LABEL_MAP.getValue(flag)))
            }
        }
    }.toList()

    /**
     * A unique identifier describing this action
     */
    val uniqueId: String
        get() = buildString {
            append(type)
            append(data)
            extras.forEach {
                append("${it.id}${it.data}")
            }
        }
}