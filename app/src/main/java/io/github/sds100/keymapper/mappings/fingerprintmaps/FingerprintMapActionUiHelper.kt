package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.BaseActionUiHelper
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.mappings.DisplayActionUseCase
import io.github.sds100.keymapper.util.ui.ResourceProvider

/**
 * Created by sds100 on 04/03/2021.
 */

class FingerprintMapActionUiHelper(
    displayActionUseCase: DisplayActionUseCase,
    resourceProvider: ResourceProvider
) : BaseActionUiHelper<FingerprintMap, FingerprintMapAction>(
    displayActionUseCase,
    resourceProvider
) {

    override fun getOptionLabels(mapping: FingerprintMap, action: FingerprintMapAction): List<String> = sequence {

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
                    append(getString(R.string.flag_repeat_build_description_repeat_rate, action.repeatRate))
                }

                append(" ")

                when (action.repeatMode) {
                    RepeatMode.TRIGGER_PRESSED_AGAIN -> {
                        append(getString(R.string.flag_repeat_build_description_until_swiped_again))
                    }
                    else -> Unit
                }
            }

            yield(repeatDescription)
        }

        if (mapping.isHoldingDownActionUntilSwipedAgainAllowed(action) && action.holdDownUntilSwipedAgain) {
            yield(getString(R.string.flag_hold_down_until_swiped_again))
        }

    }.toList()
}