package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.BaseActionUiHelperOld
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.mappings.DisplayActionUseCase
import io.github.sds100.keymapper.util.ui.ResourceProvider

/**
 * Created by sds100 on 04/03/2021.
 */

class KeyMapActionUiHelperOld(
    displayActionUseCase: DisplayActionUseCase,
    resourceProvider: ResourceProvider,
) : BaseActionUiHelperOld<KeyMap, KeyMapAction>(displayActionUseCase, resourceProvider) {

    override fun getOptionLabels(mapping: KeyMap, action: KeyMapAction) = sequence {
        if (mapping.isRepeatingActionsAllowed() && action.repeat) {
            val repeatDescription = buildString {
                append(getString(R.string.flag_repeat_build_description_start))

                val repeatLimit = when {
                    action.repeatLimit != null -> action.repeatLimit
                    action.repeatMode == RepeatMode.LIMIT_REACHED -> 1 // and is null
                    else -> null
                }

                if (repeatLimit != null) {
                    append(" ")
                    append(getString(R.string.flag_repeat_build_description_limit, repeatLimit))
                }

                if (action.repeatRate != null) {
                    append(" ")
                    append(
                        getString(
                            R.string.flag_repeat_build_description_repeat_rate,
                            action.repeatRate,
                        ),
                    )
                }

                if (action.repeatDelay != null) {
                    append(" ")
                    append(
                        getString(
                            R.string.flag_repeat_build_description_repeat_delay,
                            action.repeatDelay,
                        ),
                    )
                }

                append(" ")

                when (action.repeatMode) {
                    RepeatMode.TRIGGER_RELEASED -> {
                        append(getString(R.string.flag_repeat_build_description_until_released))
                    }

                    RepeatMode.TRIGGER_PRESSED_AGAIN -> {
                        append(getString(R.string.flag_repeat_build_description_until_pressed_again))
                    }

                    else -> Unit
                }
            }

            yield(repeatDescription)
        }

        if (mapping.isHoldingDownActionAllowed(action) &&
            action.holdDown &&
            !action.stopHoldDownWhenTriggerPressedAgain
        ) {
            yield(getString(R.string.flag_hold_down))
        }

        if (mapping.isHoldingDownActionAllowed(action) &&
            action.holdDown &&
            action.stopHoldDownWhenTriggerPressedAgain
        ) {
            yield(getString(R.string.flag_hold_down_until_pressed_again))
        }
    }.toList()
}
