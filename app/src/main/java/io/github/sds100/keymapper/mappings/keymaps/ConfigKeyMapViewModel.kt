package io.github.sds100.keymapper.mappings.keymaps

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.actions.CreateActionUseCase
import io.github.sds100.keymapper.actions.TestActionUseCase
import io.github.sds100.keymapper.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.constraints.ConstraintUtils
import io.github.sds100.keymapper.mappings.FingerprintGesturesSupportedUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.ConfigTriggerViewModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.SetupGuiKeyboardUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.purchasing.PurchasingManager
import io.github.sds100.keymapper.ui.utils.getJsonSerializable
import io.github.sds100.keymapper.ui.utils.putJsonSerializable
import io.github.sds100.keymapper.util.firstBlocking
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 22/11/20.
 */

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
        displayMapping,
        config,
        ConstraintUtils.KEY_MAP_ALLOWED_CONSTRAINTS,
        resourceProvider,
    )

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

    fun loadNewKeymap(floatingButtonUid: String? = null) {
        config.loadNewKeyMap()
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
