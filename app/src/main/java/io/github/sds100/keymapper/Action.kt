package io.github.sds100.keymapper

import androidx.room.ColumnInfo
import io.github.sds100.keymapper.Data.KeyMapDao

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [data] The information required to perform the action. E.g if the type is [ActionType.APP]
 * then the data will be the package name of the application
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
         * - App shortcuts: the name of the activity to launch
         * - Keycode: the keycode
         * - Key: the keycode of the key
         * - Block of text: text to insert
         * - System action: string representation of the [SystemAction] enum
         */
        @ColumnInfo(name = KeyMapDao.KEY_ACTION_DATA)
        val data: String
) {
    companion object {
        const val EXTRA_ACTION = "extra_action"
    }
}