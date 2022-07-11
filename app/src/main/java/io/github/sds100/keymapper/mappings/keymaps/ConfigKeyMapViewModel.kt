package io.github.sds100.keymapper.mappings.keymaps

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.actions.CreateActionUseCase
import io.github.sds100.keymapper.actions.TestActionUseCase
import io.github.sds100.keymapper.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.constraints.ConstraintUtils
import io.github.sds100.keymapper.mappings.ConfigMappingUiState
import io.github.sds100.keymapper.mappings.ConfigMappingViewModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.ConfigTriggerKeyViewModel
import io.github.sds100.keymapper.mappings.keymaps.trigger.RecordTriggerUseCase
import io.github.sds100.keymapper.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.ui.utils.getJsonSerializable
import io.github.sds100.keymapper.ui.utils.putJsonSerializable
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.firstBlocking
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by sds100 on 22/11/20.
 */

@HiltViewModel
class ConfigKeyMapViewModel @Inject constructor(
    private val config: ConfigKeyMapUseCase,
    private val testAction: TestActionUseCase,
    private val onboarding: OnboardingUseCase,
    private val recordTrigger: RecordTriggerUseCase,
    private val createKeyMapShortcut: CreateKeyMapShortcutUseCase,
    private val displayMapping: DisplayKeyMapUseCase,
    createActionUseCase: CreateActionUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), ConfigMappingViewModel, ResourceProvider by resourceProvider {

    companion object {
        private const val STATE_KEY = "config_keymap"
    }

    override val editActionViewModel =
        EditKeyMapActionViewModel(viewModelScope, config, resourceProvider, createActionUseCase)

    val configTriggerKeyViewModel =
        ConfigTriggerKeyViewModel(viewModelScope, config, resourceProvider)

    override val configActionsViewModel = ConfigActionsViewModel(
        viewModelScope,
        displayMapping,
        testAction,
        config,
        KeyMapActionUiHelper(displayMapping, resourceProvider),
        onboarding,
        resourceProvider
    )

    val configTriggerViewModel = ConfigKeyMapTriggerViewModel(
        viewModelScope,
        onboarding,
        config,
        recordTrigger,
        createKeyMapShortcut,
        displayMapping,
        resourceProvider
    )

    override val configConstraintsViewModel = ConfigConstraintsViewModel(
        viewModelScope,
        displayMapping,
        config,
        ConstraintUtils.KEY_MAP_ALLOWED_CONSTRAINTS,
        resourceProvider
    )

    override val state = MutableStateFlow<ConfigMappingUiState>(buildUiState(State.Loading))

    override fun setEnabled(enabled: Boolean) = config.setEnabled(enabled)

    init {
        viewModelScope.launch {
            config.mapping.collectLatest {
                state.value = buildUiState(it)
            }
        }
    }

    override fun save() = config.save()

    override fun saveState(outState: Bundle) {
        config.mapping.firstBlocking().ifIsData {
            outState.putJsonSerializable(STATE_KEY, it)
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun restoreState(state: Bundle) {
        val keyMap = state.getJsonSerializable<KeyMap>(STATE_KEY) ?: KeyMap()
        config.restoreState(keyMap)
    }

    fun loadNewKeymap() {
        config.loadNewKeyMap()
    }

    fun loadKeymap(uid: String) {
        viewModelScope.launch {
            config.loadKeyMap(uid)
        }
    }

    private fun buildUiState(configState: State<KeyMap>): ConfigKeymapUiState {
        return when (configState) {
            is State.Data -> ConfigKeymapUiState(configState.data.isEnabled)
            is State.Loading -> ConfigKeymapUiState(isEnabled = false)
        }
    }
}

data class ConfigKeymapUiState(
    override val isEnabled: Boolean
) : ConfigMappingUiState