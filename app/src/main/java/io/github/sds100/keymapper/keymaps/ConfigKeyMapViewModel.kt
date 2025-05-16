package io.github.sds100.keymapper.keymaps

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.actions.CreateActionUseCase
import io.github.sds100.keymapper.actions.TestActionUseCase
import io.github.sds100.keymapper.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.onboarding.OnboardingTapTarget
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.purchasing.PurchasingManager
import io.github.sds100.keymapper.trigger.ConfigTriggerViewModel
import io.github.sds100.keymapper.trigger.RecordTriggerUseCase
import io.github.sds100.keymapper.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.ui.utils.getJsonSerializable
import io.github.sds100.keymapper.ui.utils.putJsonSerializable
import io.github.sds100.keymapper.common.state.dataOrNull
import io.github.sds100.keymapper.util.firstBlocking
import io.github.sds100.keymapper.common.state.ifIsData
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConfigKeyMapViewModel(
    private val config: ConfigKeyMapUseCase,
    private val testAction: TestActionUseCase,
    private val onboarding: OnboardingUseCase,
    private val recordTrigger: RecordTriggerUseCase,
    private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    private val displayMapping: DisplayKeyMapUseCase,
    createActionUseCase: CreateActionUseCase,
    resourceProvider: ResourceProvider,
    purchasingManager: PurchasingManager,
    setupGuiKeyboardUseCase: SetupGuiKeyboardUseCase,
    fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
) : ViewModel(),
    ResourceProvider by resourceProvider {

    companion object {
        private const val STATE_KEY = "config_keymap"
    }

    val configActionsViewModel = ConfigActionsViewModel(
        viewModelScope,
        displayMapping,
        createActionUseCase,
        testAction,
        config,
        onboarding,
        resourceProvider,
    )

    val configTriggerViewModel = ConfigTriggerViewModel(
        viewModelScope,
        onboarding,
        config,
        recordTrigger,
        createKeyMapShortcut,
        displayMapping,
        resourceProvider,
        purchasingManager,
        setupGuiKeyboardUseCase,
        fingerprintGesturesSupported,
    )

    val configConstraintsViewModel = ConfigConstraintsViewModel(
        viewModelScope,
        config,
        displayMapping,
        resourceProvider,
    )

    val isEnabled: StateFlow<Boolean> = config.keyMap
        .map { state -> state.dataOrNull()?.isEnabled ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val isKeyMapEdited: Boolean
        get() = config.isEdited

    val showActionsTapTarget: StateFlow<Boolean> =
        combine(
            onboarding.showTapTarget(OnboardingTapTarget.CHOOSE_ACTION),
            config.keyMap,
        ) { showTapTarget, keyMapState ->
            // Show the choose action tap target if they have recorded a key.
            showTapTarget && keyMapState.dataOrNull()?.trigger?.keys?.isNotEmpty() ?: false
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val showConstraintsTapTarget: StateFlow<Boolean> =
        combine(
            onboarding.showTapTarget(OnboardingTapTarget.CHOOSE_CONSTRAINT),
            config.keyMap,
        ) { showTapTarget, keyMapState ->
            // Show the choose constraint tap target if they have added an action.
            showTapTarget && keyMapState.dataOrNull()?.actionList?.isNotEmpty() ?: false
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun save() = config.save()

    fun saveState(outState: Bundle) {
        config.keyMap.firstBlocking().ifIsData {
            outState.putJsonSerializable(STATE_KEY, it)
        }
    }

    fun restoreState(state: Bundle) {
        val keyMap = state.getJsonSerializable<KeyMap>(STATE_KEY) ?: KeyMap()
        config.restoreState(keyMap)
    }

    fun loadNewKeymap(floatingButtonUid: String? = null, groupUid: String?) {
        config.loadNewKeyMap(groupUid)
        if (floatingButtonUid != null) {
            viewModelScope.launch {
                config.addFloatingButtonTriggerKey(floatingButtonUid)
            }
        }
    }

    fun loadKeyMap(uid: String) {
        viewModelScope.launch {
            config.loadKeyMap(uid)
        }
    }

    fun onEnabledChanged(enabled: Boolean) {
        config.setEnabled(enabled)
    }

    fun onActionTapTargetCompleted() {
        onboarding.completedTapTarget(OnboardingTapTarget.CHOOSE_ACTION)
    }

    fun onConstraintTapTargetCompleted() {
        onboarding.completedTapTarget(OnboardingTapTarget.CHOOSE_CONSTRAINT)
    }

    fun onSkipTutorialClick() {
        onboarding.skipTapTargetOnboarding()
    }

    class Factory(
        private val config: ConfigKeyMapUseCase,
        private val testAction: TestActionUseCase,
        private val onboard: OnboardingUseCase,
        private val recordTrigger: RecordTriggerUseCase,
        private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
        private val displayMapping: DisplayKeyMapUseCase,
        private val createActionUseCase: CreateActionUseCase,
        private val resourceProvider: ResourceProvider,
        private val purchasingManager: PurchasingManager,
        private val setupGuiKeyboardUseCase: SetupGuiKeyboardUseCase,
        private val fingerprintGesturesSupported: FingerprintGesturesSupportedUseCase,
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>) = ConfigKeyMapViewModel(
            config,
            testAction,
            onboard,
            recordTrigger,
            createKeyMapShortcut,
            displayMapping,
            createActionUseCase,
            resourceProvider,
            purchasingManager,
            setupGuiKeyboardUseCase,
            fingerprintGesturesSupported,
        ) as T
    }
}
