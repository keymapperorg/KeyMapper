package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.BaseActionUiHelper
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
                when {
                    action.repeatLimit != null -> {
                        append(getString(R.string.flag_repeat_actions_limit_reached, action.repeatLimit))
                    }

                    else -> {
                        append(getString(R.string.flag_repeat_actions_swiped_again))
                    }
                }
            }

            yield(repeatDescription)
        }

        if (mapping.isHoldingDownActionUntilSwipedAgainAllowed(action) && action.holdDownUntilSwipedAgain) {
            yield(getString(R.string.flag_hold_down_until_swiped_again))
        }

    }.toList()
}