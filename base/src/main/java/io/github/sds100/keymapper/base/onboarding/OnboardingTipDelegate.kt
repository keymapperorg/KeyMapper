package io.github.sds100.keymapper.base.onboarding

import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.trigger.ConfigTriggerUseCase
import io.github.sds100.keymapper.base.trigger.KeyCodeTriggerKey
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.base.trigger.TriggerMode
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.data.utils.PrefDelegate
import io.github.sds100.keymapper.system.inputevents.KeyEventUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

@ViewModelScoped
class OnboardingTipDelegateImpl @Inject constructor(
    @Named("viewmodel")
    private val viewModelScope: CoroutineScope,
    private val preferenceRepository: PreferenceRepository,
    private val configTriggerUseCase: ConfigTriggerUseCase,
    resourceProvider: ResourceProvider
) : OnboardingTipDelegate, PreferenceRepository by preferenceRepository,
    ResourceProvider by resourceProvider {

    companion object {
        private const val POWER_BUTTON_EMERGENCY_TIP_ID = "power_button_emergency_tip"
        private const val PARALLEL_TRIGGER_TIP_ID = "parallel_trigger_tip"
        private const val SEQUENCE_TRIGGER_TIP_ID = "sequence_trigger_tip"
        private const val TRIGGER_CONSTRAINTS_TIP_ID = "trigger_constraints_tip"
    }

    override val triggerTip: MutableStateFlow<OnboardingTipModel?> = MutableStateFlow(null)

    private var shownParallelTriggerOrderExplanation: Boolean by PrefDelegate(
        Keys.shownParallelTriggerOrderExplanation,
        false,
    )

    private var shownSequenceTriggerExplanation: Boolean by PrefDelegate(
        Keys.shownSequenceTriggerExplanation,
        false,
    )

    private var shownTriggerConstraintsTip: Boolean by PrefDelegate(
        Keys.shownTriggerConstraintsTip,
        false,
    )

    init {
        viewModelScope.launch {
            configTriggerUseCase.keyMap
                .mapNotNull { it.dataOrNull()?.trigger }
                .collect { trigger ->
                    onCollectTrigger(trigger)
                }
        }
    }

    override fun onDismissClick() {
        val currentTip = triggerTip.value

        when (currentTip?.id) {
            PARALLEL_TRIGGER_TIP_ID -> {
                shownParallelTriggerOrderExplanation = true
            }

            SEQUENCE_TRIGGER_TIP_ID -> {
                shownSequenceTriggerExplanation = true
            }

            TRIGGER_CONSTRAINTS_TIP_ID -> {
                shownTriggerConstraintsTip = true
            }
            // POWER_BUTTON_EMERGENCY_TIP_ID doesn't need preference setting as it's non-dismissable
        }

        triggerTip.value = null
    }

    private fun onCollectTrigger(trigger: Trigger) {
        val showPowerButtonEmergencyTip = trigger.keys.any {
            it is KeyCodeTriggerKey && KeyEventUtils.isPowerButtonKey(
                it.keyCode,
                it.scanCode ?: -1,
            )
        }

        if (showPowerButtonEmergencyTip) {
            val tipModel = OnboardingTipModel(
                id = POWER_BUTTON_EMERGENCY_TIP_ID,
                title = getString(R.string.pro_mode_emergency_tip_title),
                message = getString(R.string.pro_mode_emergency_tip_text),
                isDismissable = false
            )

            triggerTip.value = tipModel

        } else if (trigger.mode is TriggerMode.Parallel) {
            if (shownParallelTriggerOrderExplanation) {
                return
            }

            val tipModel = OnboardingTipModel(
                id = PARALLEL_TRIGGER_TIP_ID,
                title = getString(R.string.tip_parallel_trigger_title),
                message = getString(R.string.dialog_message_parallel_trigger_order),
                isDismissable = true
            )

            triggerTip.value = tipModel
        } else if (trigger.mode is TriggerMode.Sequence) {
            if (shownSequenceTriggerExplanation) {
                return
            }

            val tipModel = OnboardingTipModel(
                id = SEQUENCE_TRIGGER_TIP_ID,
                title = getString(R.string.tip_sequence_trigger_title),
                message = getString(R.string.dialog_message_sequence_trigger_explanation),
                isDismissable = true
            )

            triggerTip.value = tipModel
        } else {
            triggerTip.value = null
        }
    }
}

interface OnboardingTipDelegate {
    val triggerTip: StateFlow<OnboardingTipModel?>

    fun onDismissClick()
}