package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.BaseActionUiHelper
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.mappings.DisplayActionUseCase
import io.github.sds100.keymapper.util.ui.ResourceProvider

/**
 * Created by sds100 on 04/03/2021.
 */

class KeyMapActionUiHelper(
    displayActionUseCase: DisplayActionUseCase,
    resourceProvider: ResourceProvider
) : BaseActionUiHelper<KeyMap, KeyMapAction>(displayActionUseCase, resourceProvider) {

    override fun getOptionLabels(mapping: KeyMap, action: KeyMapAction) = sequence {
        if (mapping.isRepeatingActionsAllowed() && action.repeat) {
            val repeatDescription = buildString {
                when (action.repeatMode) {
                    RepeatMode.TRIGGER_RELEASED -> {
                        append(getString(R.string.flag_repeat_actions_trigger_released))
                    }

                    RepeatMode.LIMIT_REACHED -> {
                        append(getString(R.string.flag_repeat_actions_limit_reached, action.repeatLimit ?: 1))
                    }

                    RepeatMode.TRIGGER_PRESSED_AGAIN -> {
                        append(getString(R.string.flag_repeat_actions_pressed_again))
                    }
                }
            }

            yield(repeatDescription)
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