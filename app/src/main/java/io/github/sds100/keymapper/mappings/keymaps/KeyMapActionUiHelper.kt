package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.mappings.DisplayActionUseCase
import io.github.sds100.keymapper.actions.BaseActionUiHelper

/**
 * Created by sds100 on 04/03/2021.
 */

class KeyMapActionUiHelper(
    displayActionUseCase: DisplayActionUseCase,
    resourceProvider: ResourceProvider
) : BaseActionUiHelper<KeyMap, KeyMapAction>(displayActionUseCase, resourceProvider) {

    override fun getOptionLabels(mapping: KeyMap, action: KeyMapAction) = sequence {
        if (mapping.isRepeatingActionsAllowed()
            && action.repeat
            && !action.stopRepeatingWhenTriggerPressedAgain
        ) {
            yield(getString(R.string.flag_repeat_actions))
        }

        if (mapping.isStopRepeatingActionWhenTriggerPressedAgainAllowed(action)
            && action.stopRepeatingWhenTriggerPressedAgain
        ) {
            yield(getString(R.string.flag_repeat_until_pressed_again))
        }

        if (mapping.isHoldingDownActionAllowed(action)
            && action.holdDown
            && !action.stopHoldDownWhenTriggerPressedAgain) {
            yield(getString(R.string.flag_hold_down))
        }

        if (mapping.isHoldingDownActionAllowed(action)
            && action.holdDown
            && action.stopHoldDownWhenTriggerPressedAgain) {
            yield(getString(R.string.flag_hold_down_until_pressed_again))
        }


    }.toList()
}