package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.constraints.ConstraintState

/**
 * Created by sds100 on 04/04/2021.
 */

interface Mapping<ACTION : Action> {
    val isEnabled: Boolean
    val constraintState: ConstraintState
    val actionList: List<ACTION>
    val showToast: Boolean
    val vibrate: Boolean
    val vibrateDuration: Int?
}

fun Mapping<*>.isDelayBeforeNextActionAllowed(): Boolean = actionList.isNotEmpty()
