package io.github.sds100.keymapper.actions

import android.os.Build

/**
 * Created by sds100 on 16/03/2021.
 */
fun ActionData.canBeHeldDown() = when (this) {
    is KeyEventAction -> !useShell
    is TapCoordinateAction -> Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    else -> false
}

fun ActionData.requiresImeToPerform() = when (this) {
    is KeyEventAction -> !useShell
    is TextAction -> true
    is SystemAction -> id == SystemActionId.MOVE_CURSOR_TO_END
    else -> false
}