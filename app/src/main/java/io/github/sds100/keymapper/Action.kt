package io.github.sds100.keymapper

/**
 * Created by sds100 on 16/07/2018.
 */

/**
 * @property [data] The information required to perform the action. E.g if the type is [TYPE_APP]
 * then the data will be the package name of the application
 *
 * Different Types of actions:
 * - Applications
 * - Application shortcuts
 * - Keycode
 * - Insert a block of text
 * - System settings
 * - Root system settings
 * - Global actions (performGlobalAction)
 */

data class Action(
        val type: Int,
        val data: String
) {
    companion object {
        const val TYPE_APP = 0
        const val TYPE_APP_SHORTCUT = 1
        const val TYPE_KEY_CODE = 2
        const val TYPE_KEY = 3
        const val TYPE_TEXT_BLOCK = 4
        const val TYPE_SYSTEM_SETTING = 5
        const val TYPE_GLOBAL_ACTION = 6
    }
}