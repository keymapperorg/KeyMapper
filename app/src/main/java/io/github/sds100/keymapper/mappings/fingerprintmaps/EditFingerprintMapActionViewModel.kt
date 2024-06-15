package io.github.sds100.keymapper.mappings.fingerprintmaps

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.CreateActionUseCase
import io.github.sds100.keymapper.actions.RepeatMode
import io.github.sds100.keymapper.mappings.EditActionViewModel
import io.github.sds100.keymapper.mappings.OptionMinimums
import io.github.sds100.keymapper.mappings.isDelayBeforeNextActionAllowed
import io.github.sds100.keymapper.util.Defaultable
import io.github.sds100.keymapper.util.ui.CheckBoxListItem
import io.github.sds100.keymapper.util.ui.DividerListItem
import io.github.sds100.keymapper.util.ui.ListItem
import io.github.sds100.keymapper.util.ui.RadioButtonPairListItem
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.util.ui.SliderListItem
import io.github.sds100.keymapper.util.ui.SliderMaximums
import io.github.sds100.keymapper.util.ui.SliderModel
import io.github.sds100.keymapper.util.ui.SliderStepSizes
import kotlinx.coroutines.CoroutineScope

/**
 * Created by sds100 on 27/06/20.
 */
class EditFingerprintMapActionViewModel(
    coroutineScope: CoroutineScope,
    val config: ConfigFingerprintMapUseCase,
    createActionUseCase: CreateActionUseCase,
    resourceProvider: ResourceProvider,
) : EditActionViewModel<FingerprintMap, FingerprintMapAction>(
    resourceProvider,
    coroutineScope,
    config,
    createActionUseCase,
),
    ResourceProvider by resourceProvider {

    companion object {
        private const val ID_REPEAT_RATE = "repeat_rate"
        private const val ID_REPEAT = "repeat_until_swiped_again"

        private const val ID_STOP_REPEATING_TRIGGER_PRESSED_AGAIN =
            "stop_repeating_trigger_pressed_again"
        private const val ID_STOP_REPEATING_LIMIT_REACHED = "stop_repeating_limit_reached"

        private const val ID_MULTIPLIER = "multiplier"
        private const val ID_HOLD_DOWN_UNTIL_SWIPED_AGAIN = "hold_down"
        private const val ID_DELAY_BEFORE_NEXT_ACTION = "delay_before_next_action"
        private const val ID_HOLD_DOWN_DURATION = "hold_down_duration"
        private const val ID_REPEAT_LIMIT = "repeat_limit"
    }

    override fun setRadioButtonValue(id: String, value: Boolean) {
        val actionUid = actionUid.value ?: return

        when (id) {
            ID_STOP_REPEATING_TRIGGER_PRESSED_AGAIN -> if (value) {
                config.setActionStopRepeatingWhenTriggerPressedAgain(actionUid)
            }

            ID_STOP_REPEATING_LIMIT_REACHED -> if (value) {
                config.setActionStopRepeatingWhenLimitReached(actionUid)
            }
        }
    }

    override fun setSliderValue(id: String, value: Defaultable<Int>) {
        val actionUid = actionUid.value ?: return

        when (id) {
            ID_REPEAT_RATE -> config.setActionRepeatRate(actionUid, value.nullIfDefault())
            ID_HOLD_DOWN_DURATION -> config.setActionHoldDownDuration(
                actionUid,
                value.nullIfDefault(),
            )

            ID_MULTIPLIER -> config.setActionMultiplier(actionUid, value.nullIfDefault())
            ID_DELAY_BEFORE_NEXT_ACTION -> config.setDelayBeforeNextAction(
                actionUid,
                value.nullIfDefault(),
            )

            ID_REPEAT_LIMIT -> config.setActionRepeatLimit(actionUid, value.nullIfDefault())
        }
    }

    override fun setCheckboxValue(id: String, value: Boolean) {
        val actionUid = actionUid.value ?: return

        when (id) {
            ID_REPEAT -> config.setActionRepeatEnabled(actionUid, value)
            ID_HOLD_DOWN_UNTIL_SWIPED_AGAIN -> config.setActionHoldDownEnabled(actionUid, value)
        }
    }

    override fun createListItems(
        mapping: FingerprintMap,
        action: FingerprintMapAction,
    ): List<ListItem> = sequence {
        if (mapping.isRepeatingActionsAllowed()) {
            yield(
                CheckBoxListItem(
                    id = ID_REPEAT,
                    label = getString(R.string.flag_repeat_actions),
                    isChecked = action.repeat,
                ),
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
                        stepSize = SliderStepSizes.ACTION_REPEAT_RATE,
                    ),
                ),
            )
        }

        if (mapping.isChangingRepeatLimitAllowed(action)) {
            // only allow setting it to no limit if the action doesn't repeat until the limit is reached
            val isNoLimitAllowed = action.repeatMode != RepeatMode.LIMIT_REACHED

            val sliderValue = if (action.repeatMode == RepeatMode.LIMIT_REACHED) {
                if (action.repeatLimit == null) {
                    Defaultable.Custom(1)
                } else {
                    Defaultable.Custom(action.repeatLimit)
                }
            } else {
                Defaultable.create(action.repeatLimit)
            }

            yield(
                SliderListItem(
                    id = ID_REPEAT_LIMIT,
                    label = getString(R.string.extra_label_repeat_limit),
                    sliderModel = SliderModel(
                        value = sliderValue,
                        isDefaultStepEnabled = isNoLimitAllowed,
                        min = 1,
                        max = 20,
                        stepSize = 1,
                        customButtonDefaultText = getString(R.string.button_slider_repeat_no_limit),
                    ),
                ),
            )
        }

        if (mapping.isChangingRepeatModeAllowed(action)) {
            yield(
                RadioButtonPairListItem(
                    id = "repeat_mode",
                    header = getString(R.string.stop_repeating_dot_dot_dot),

                    leftButtonId = ID_STOP_REPEATING_TRIGGER_PRESSED_AGAIN,
                    leftButtonText = getString(R.string.stop_repeating_when_swiped_again),
                    leftButtonChecked = action.repeatMode == RepeatMode.TRIGGER_PRESSED_AGAIN,

                    rightButtonId = ID_STOP_REPEATING_LIMIT_REACHED,
                    rightButtonText = getString(R.string.stop_repeating_limit_reached),
                    rightButtonChecked = action.repeatMode == RepeatMode.LIMIT_REACHED,
                ),
            )
        }

        if (mapping.isHoldingDownActionUntilSwipedAgainAllowed(action)) {
            yield(DividerListItem("hold_down_divider"))

            yield(
                CheckBoxListItem(
                    id = ID_HOLD_DOWN_UNTIL_SWIPED_AGAIN,
                    label = getString(R.string.flag_hold_down_until_swiped_again),
                    isChecked = action.holdDownUntilSwipedAgain,
                ),
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
                        stepSize = SliderStepSizes.ACTION_HOLD_DOWN_DURATION,
                    ),
                ),
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
                        stepSize = SliderStepSizes.DELAY_BEFORE_NEXT_ACTION,
                    ),
                ),
            )
        }

        val multiplierLabel = if (action.repeat && mapping.isRepeatingActionsAllowed()) {
            getString(R.string.extra_label_action_multiplier_with_repeat)
        } else {
            getString(R.string.extra_label_action_multiplier)
        }

        yield(
            SliderListItem(
                id = ID_MULTIPLIER,
                label = multiplierLabel,
                sliderModel = SliderModel(
                    value = Defaultable.create(action.multiplier),
                    isDefaultStepEnabled = true,
                    min = OptionMinimums.ACTION_MULTIPLIER,
                    max = SliderMaximums.ACTION_MULTIPLIER,
                    stepSize = SliderStepSizes.ACTION_MULTIPLIER,
                ),
            ),
        )
    }.toList()
}
