package io.github.sds100.keymapper.mappings

import io.github.sds100.keymapper.actions.FakeAction
import io.github.sds100.keymapper.constraints.ConstraintState

/**
 * Created by sds100 on 28/04/2021.
 */
data class FakeMapping(
    override val isEnabled: Boolean = true,
    override val constraintState: ConstraintState = ConstraintState(),
    override val actionList: List<FakeAction> = emptyList(),
    override val showToast: Boolean = false,
    override val vibrate: Boolean = false,
    override val vibrateDuration: Int? = null,
) : Mapping<FakeAction>
