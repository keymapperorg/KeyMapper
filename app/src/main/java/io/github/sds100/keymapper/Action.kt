package io.github.sds100.keymapper

import androidx.annotation.StringDef
import androidx.room.ColumnInfo
import io.github.sds100.keymapper.Data.KeyMapDao
import io.github.sds100.keymapper.SystemAction.CATEGORY_VOLUME
import io.github.sds100.keymapper.Utils.ErrorCodeUtils
import io.github.sds100.keymapper.Utils.SystemActionUtils
import java.io.Serializable

/**
 * Created by sds100 on 16/07/2018.
 */

@StringDef(value = [
    Action.EXTRA_PACKAGE_NAME,
    Action.EXTRA_SHORTCUT_TITLE
])
annotation class ExtraId

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
        @ColumnInfo(name = KeyMapDao.KEY_ACTION_TYPE)
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
        @ColumnInfo(name = KeyMapDao.KEY_ACTION_DATA)
        val data: String,

        @ColumnInfo(name = KeyMapDao.KEY_ACTION_EXTRAS)
        val extras: MutableList<Extra> = mutableListOf()
) : Serializable {
    companion object {
        const val EXTRA_ACTION = "extra_action"

        //DON'T CHANGE THESE IDs!!!!
        const val EXTRA_SHORTCUT_TITLE = "extra_title"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
    }

    val requiresIME: Boolean
        get() = type == ActionType.KEY ||
                type == ActionType.KEYCODE ||
                type == ActionType.TEXT_BLOCK

    val isVolumeAction: Boolean
        get() = type == ActionType.SYSTEM_ACTION &&
                SystemActionUtils.getSystemActionDef(data).handle(
                        onSuccess = { it.category == CATEGORY_VOLUME },
                        onFailure = { false }
                )

    fun getExtraData(extraId: String) = extras.find { it.id == extraId }?.data.createResult(
            ErrorCodeUtils.ERROR_CODE_ACTION_EXTRA_NOT_FOUND, extraId
    )
}