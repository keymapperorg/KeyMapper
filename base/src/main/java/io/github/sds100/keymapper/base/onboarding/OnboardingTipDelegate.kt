package io.github.sds100.keymapper.base.onboarding

import android.os.Build
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.Action
import io.github.sds100.keymapper.base.actions.ActionData
import io.github.sds100.keymapper.base.actions.ConfigActionsUseCase
import io.github.sds100.keymapper.base.trigger.ConfigTriggerUseCase
import io.github.sds100.keymapper.base.trigger.KeyCodeTriggerKey
import io.github.sds100.keymapper.base.trigger.KeyEventTriggerKey
import io.github.sds100.keymapper.base.trigger.Trigger
import io.github.sds100.keymapper.base.trigger.TriggerMode
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.Constants
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
    private val configActionsUseCase: ConfigActionsUseCase,
    resourceProvider: ResourceProvider,
    navigationProvider: NavigationProvider,
) : OnboardingTipDelegate,
    NavigationProvider by navigationProvider,
    PreferenceRepository by preferenceRepository,
    ResourceProvider by resourceProvider {

    companion object {
        private const val POWER_BUTTON_EMERGENCY_TIP_ID = "power_button_emergency_tip"
        private const val PARALLEL_TRIGGER_TIP_ID = "parallel_trigger_tip"
        private const val SEQUENCE_TRIGGER_TIP_ID = "sequence_trigger_tip"
        private const val TRIGGER_CONSTRAINTS_TIP_ID = "trigger_constraints_tip"
        const val CAPS_LOCK_TIP_ID = "caps_lock_tip"
        const val SCREEN_PINNING_TIP_ID = "screen_pinning_tip"
        const val IME_DETECTION_TIP_ID = "ime_detection_tip"
        const val RINGER_MODE_TIP_ID = "ringer_mode_tip"
    }

    override val triggerTip: MutableStateFlow<OnboardingTipModel?> = MutableStateFlow(null)
    override val actionsTip: MutableStateFlow<OnboardingTipModel?> = MutableStateFlow(null)

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

    private var shownCapsLockTip: Boolean by PrefDelegate(
        Keys.shownCapsLockTip,
        false,
    )

    private var shownScreenPinningTip: Boolean by PrefDelegate(
        Keys.shownScreenPinningTip,
        false,
    )

    private var shownImeDetectionTip: Boolean by PrefDelegate(
        Keys.shownTriggerKeyboardIconExplanation,
        false,
    )

    private var shownRingerModeTip: Boolean by PrefDelegate(
        Keys.shownRingerModeTip,
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

        viewModelScope.launch {
            configActionsUseCase.keyMap
                .mapNotNull { it.dataOrNull()?.actionList }
                .collect { actionList ->
                    onCollectActions(actionList)
                }
        }
    }

    override fun onTriggerTipDismissClick() {
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

            CAPS_LOCK_TIP_ID -> {
                shownCapsLockTip = true
            }

            SCREEN_PINNING_TIP_ID -> {
                shownScreenPinningTip = true
            }

            IME_DETECTION_TIP_ID -> {
                shownImeDetectionTip = true
            }

            // POWER_BUTTON_EMERGENCY_TIP_ID doesn't need preference setting as it's non-dismissable
        }

        triggerTip.value = null
    }

    override fun onActionTipDismissClick() {
        val currentTip = actionsTip.value

        when (currentTip?.id) {
            RINGER_MODE_TIP_ID -> {
                shownRingerModeTip = true
            }
        }

        actionsTip.value = null
    }

    override fun onTipButtonClick(tipId: String) {
        when (tipId) {
            RINGER_MODE_TIP_ID -> {
                viewModelScope.launch {
                    navigate("ringer_mode_tip_pro_mode", NavDestination.ProMode)
                }
            }
        }
    }

    private fun onCollectTrigger(trigger: Trigger) {
        val showPowerButtonEmergencyTip = trigger.keys.any {
            it is KeyCodeTriggerKey && KeyEventUtils.isPowerButtonKey(
                it.keyCode,
                it.scanCode ?: -1,
            )
        }

        val hasCapsLockKey =
            trigger.keys.any { it is KeyEventTriggerKey && it.keyCode == android.view.KeyEvent.KEYCODE_CAPS_LOCK }
        val hasBackKey =
            trigger.keys.any { it is KeyEventTriggerKey && it.keyCode == android.view.KeyEvent.KEYCODE_BACK }
        val hasImeKey = trigger.keys.any { it is KeyEventTriggerKey && it.requiresIme }

        when {
            showPowerButtonEmergencyTip -> {
                val tipModel = OnboardingTipModel(
                    id = POWER_BUTTON_EMERGENCY_TIP_ID,
                    title = getString(R.string.pro_mode_emergency_tip_title),
                    message = getString(R.string.pro_mode_emergency_tip_text),
                    isDismissable = false,
                )

                triggerTip.value = tipModel
            }

            trigger.mode is TriggerMode.Parallel && !shownParallelTriggerOrderExplanation -> {
                val tipModel = OnboardingTipModel(
                    id = PARALLEL_TRIGGER_TIP_ID,
                    title = getString(R.string.tip_parallel_trigger_title),
                    message = getString(R.string.dialog_message_parallel_trigger_order),
                    isDismissable = true,
                )

                triggerTip.value = tipModel
            }

            trigger.mode is TriggerMode.Sequence && !shownSequenceTriggerExplanation -> {
                val tipModel = OnboardingTipModel(
                    id = SEQUENCE_TRIGGER_TIP_ID,
                    title = getString(R.string.tip_sequence_trigger_title),
                    message = getString(R.string.dialog_message_sequence_trigger_explanation),
                    isDismissable = true,
                )

                triggerTip.value = tipModel
            }

            trigger.keys.isNotEmpty() && !shownTriggerConstraintsTip -> {
                val tipModel = OnboardingTipModel(
                    id = TRIGGER_CONSTRAINTS_TIP_ID,
                    title = getString(R.string.trigger_constraints_tip_title),
                    message = getString(R.string.trigger_constraints_tip_text),
                    isDismissable = true,
                )

                triggerTip.value = tipModel
            }

            hasCapsLockKey && !shownCapsLockTip -> {
                val tip = OnboardingTipModel(
                    id = CAPS_LOCK_TIP_ID,
                    title = getString(R.string.tip_caps_lock_title),
                    message = getString(R.string.tip_caps_lock_text),
                    isDismissable = true,
                )
                triggerTip.value = tip
            }

            hasBackKey && !shownScreenPinningTip -> {
                val tip = OnboardingTipModel(
                    id = SCREEN_PINNING_TIP_ID,
                    title = getString(R.string.tip_screen_pinning_title),
                    message = getString(R.string.tip_screen_pinning_text),
                    isDismissable = true,
                )
                triggerTip.value = tip
            }

            hasImeKey && !shownImeDetectionTip -> {
                val tip = OnboardingTipModel(
                    id = IME_DETECTION_TIP_ID,
                    title = getString(R.string.tip_ime_detection_title),
                    message = getString(R.string.tip_ime_detection_text),
                    isDismissable = true,
                )
                triggerTip.value = tip
            }

            else -> {
                triggerTip.value = null
            }
        }
    }

    private fun onCollectActions(actionList: List<Action>) {
        val hasRingerModeAction = actionList.any { action ->
            when (action.data) {
                is ActionData.Volume.SetRingerMode,
                is ActionData.Volume.CycleRingerMode,
                is ActionData.Volume.CycleVibrateRing,
                    -> true

                else -> false
            }
        }

        if (hasRingerModeAction && !shownRingerModeTip && Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API) {
            val tip = OnboardingTipModel(
                id = RINGER_MODE_TIP_ID,
                title = getString(R.string.tip_ringer_mode_title),
                message = getString(R.string.tip_ringer_mode_text),
                isDismissable = true,
                buttonText = getString(R.string.tip_ringer_mode_button),
            )
            actionsTip.value = tip
        }
    }
}

interface OnboardingTipDelegate {
    val triggerTip: StateFlow<OnboardingTipModel?>
    val actionsTip: StateFlow<OnboardingTipModel?>

    fun onTriggerTipDismissClick()
    fun onActionTipDismissClick()
    fun onTipButtonClick(tipId: String)
}
