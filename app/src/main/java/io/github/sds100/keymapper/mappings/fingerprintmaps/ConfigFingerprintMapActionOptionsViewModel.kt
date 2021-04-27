package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.ui.SliderModel
import io.github.sds100.keymapper.util.Defaultable
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.mappings.ConfigActionOptionsViewModel
import io.github.sds100.keymapper.mappings.OptionMinimums
import io.github.sds100.keymapper.mappings.isDelayBeforeNextActionAllowed
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.CoroutineScope

/**
 * Created by sds100 on 27/06/20.
 */
class ConfigFingerprintMapActionOptionsViewModel(
    coroutineScope: CoroutineScope,
    val config: ConfigFingerprintMapUseCase,
    resourceProvider: ResourceProvider
) : ConfigActionOptionsViewModel<FingerprintMap, FingerprintMapAction>(coroutineScope, config),
    ResourceProvider by resourceProvider {

    companion object {
        private const val ID_REPEAT_RATE = "repeat_rate"
        private const val ID_REPEAT_UNTIL_SWIPED_AGAIN = "repeat_until_swiped_again"
        private const val ID_MULTIPLIER = "multiplier"
        private const val ID_HOLD_DOWN_UNTIL_SWIPED_AGAIN = "hold_down"
        private const val ID_DELAY_BEFORE_NEXT_ACTION = "delay_before_next_action"
        private const val ID_HOLD_DOWN_DURATION = "hold_down_duration"
    }

    override fun setRadioButtonValue(id: String, value: Boolean) {
    }

    override fun setSliderValue(id: String, value: Defaultable<Int>) {
        val actionUid = actionUid.value ?: return

        when (id) {
            ID_REPEAT_RATE -> config.setActionRepeatRate(actionUid, value.nullIfDefault())
            ID_HOLD_DOWN_DURATION -> config.setActionHoldDownDuration(
                actionUid,
                value.nullIfDefault()
            )
            ID_MULTIPLIER -> config.setActionMultiplier(actionUid, value.nullIfDefault())
            ID_DELAY_BEFORE_NEXT_ACTION -> config.setDelayBeforeNextAction(
                actionUid,
                value.nullIfDefault()
            )
        }
    }

    override fun setCheckboxValue(id: String, value: Boolean) {
        val actionUid = actionUid.value ?: return

        when (id) {
            ID_REPEAT_UNTIL_SWIPED_AGAIN -> config.setActionRepeatEnabled(actionUid, value)
            ID_HOLD_DOWN_UNTIL_SWIPED_AGAIN -> config.setActionHoldDownEnabled(actionUid, value)
        }
    }


    override fun createListItems(mapping: FingerprintMap, action: FingerprintMapAction): List<ListItem> {
        return sequence {

            if (mapping.isRepeatingActionUntilSwipedAgainAllowed()) {
                yield(
                    CheckBoxListItem(
                        id = ID_REPEAT_UNTIL_SWIPED_AGAIN,
                        label = getString(R.string.flag_repeat_until_swiped_again),
                        isChecked = action.repeatUntilSwipedAgain
                    )
                )
            }

            if (mapping.isChangingActionRepeatRateAllowed(action)) {
                yield(
                    SliderListItem(
                        id = ID_REPEAT_RATE,
                        label = getString(R.string.extra_label_repeat_rate),
                        sliderModel = SliderModel(
                            value = Defaultable.create(action.repeatRate),
                            isDefaultStepEnabled = true,
                            min = OptionMinimums.ACTION_REPEAT_RATE,
                            max = SliderMaximums.ACTION_REPEAT_RATE,
                            stepSize = SliderStepSizes.ACTION_REPEAT_RATE
                        )
                    )
                )
            }

            if (mapping.isHoldingDownActionUntilSwipedAgainAllowed(action)) {
                yield(DividerListItem("hold_down_divider"))

                yield(
                    CheckBoxListItem(
                        id = ID_HOLD_DOWN_UNTIL_SWIPED_AGAIN,
                        label = getString(R.string.flag_hold_down_until_swiped_again),
                        isChecked = action.holdDownUntilSwipedAgain
                    )
                )
            }

            if (mapping.isHoldingDownActionBeforeRepeatingAllowed(action)) {
                yield(
                    SliderListItem(
                        id = ID_HOLD_DOWN_DURATION,
                        label = getString(R.string.extra_label_hold_down_duration),
                        sliderModel = SliderModel(
                            value = Defaultable.create(action.holdDownDuration),
                            isDefaultStepEnabled = true,
                            min = OptionMinimums.ACTION_HOLD_DOWN_DURATION,
                            max = SliderMaximums.ACTION_HOLD_DOWN_DURATION,
                            stepSize = SliderStepSizes.ACTION_HOLD_DOWN_DURATION
                        )
                    )
                )
            }

            if (mapping.isDelayBeforeNextActionAllowed()) {
                yield(DividerListItem("other_divider"))

                yield(
                    SliderListItem(
                        id = ID_DELAY_BEFORE_NEXT_ACTION,
                        label = getString(R.string.extra_label_delay_before_next_action),
                        sliderModel = SliderModel(
                            value = Defaultable.create(action.delayBeforeNextAction),
                            isDefaultStepEnabled = true,
                            min = OptionMinimums.DELAY_BEFORE_NEXT_ACTION,
                            max = SliderMaximums.DELAY_BEFORE_NEXT_ACTION,
                            stepSize = SliderStepSizes.DELAY_BEFORE_NEXT_ACTION
                        )
                    )
                )
            }

            yield(
                SliderListItem(
                    id = ID_MULTIPLIER,
                    label = getString(R.string.extra_label_action_multiplier),
                    sliderModel = SliderModel(
                        value = Defaultable.create(action.multiplier),
                        isDefaultStepEnabled = true,
                        min = OptionMinimums.ACTION_MULTIPLIER,
                        max = SliderMaximums.ACTION_MULTIPLIER,
                        stepSize = SliderStepSizes.ACTION_MULTIPLIER
                    )
                )
            )

        }.toList()
    }
}

