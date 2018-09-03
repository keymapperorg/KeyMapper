package io.github.sds100.keymapper

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
        val type: ActionType,
        val data: String
) {
    companion object {
        const val EXTRA_ACTION = "extra_action"
    }
}