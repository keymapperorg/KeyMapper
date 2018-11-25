package io.github.sds100.keymapper

/**
 * Created by sds100 on 25/11/2018.
 */
class CantFindSystemActionException(systemActionId: String) : Exception() {
    override val message: String? = "Can't find system action $systemActionId!"
}