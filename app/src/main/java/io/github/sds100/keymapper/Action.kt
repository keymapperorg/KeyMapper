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
 * - Applications
 * - Application shortcuts
 * - Keycode
 * - Insert a block of text
 * - System actions/settings
 * - Root actions
 */
data class Action(
        @ColumnInfo(name = KeyMapDao.KEY_ACTION_TYPE)
        val type: ActionType,

        @ColumnInfo(name = KeyMapDao.KEY_ACTION_DATA)
        val data: String
) {
    companion object {
        const val EXTRA_ACTION = "extra_action"
    }
}