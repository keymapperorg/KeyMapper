package io.github.sds100.keymapper.mappings.fingerprintmaps

import android.os.Bundle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.actions.TestActionUseCase
import io.github.sds100.keymapper.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.constraints.ConstraintUtils
import io.github.sds100.keymapper.mappings.ConfigMappingUiState
import io.github.sds100.keymapper.mappings.ConfigMappingViewModel
import io.github.sds100.keymapper.mappings.DisplaySimpleMappingUseCase
import io.github.sds100.keymapper.ui.utils.getJsonSerializable
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.ui.PopupViewModel
import io.github.sds100.keymapper.util.ui.PopupViewModelImpl
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by sds100 on 08/11/20.
 */

class ConfigFingerprintMapViewModel(
    private val config: ConfigFingerprintMapUseCase,
    private val testAction: TestActionUseCase,
    private val display: DisplaySimpleMappingUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(), ConfigMappingViewModel, PopupViewModel by PopupViewModelImpl() {

    companion object {
        private const val STATE_FINGERPRINT_MAP = "config_fingerprint_map"
        private const val STATE_KEY_ID = "config_fingerprint_map_id"
    }

    val configActionOptionsViewModel =
        ConfigFingerprintMapActionOptionsViewModel(viewModelScope, config, resourceProvider)

    val configOptionsViewModel =
        ConfigFingerprintMapOptionsViewModel(viewModelScope, config, resourceProvider)

    override val configActionsViewModel = ConfigActionsViewModel(
        viewModelScope,
        display,
        testAction,
        config,
        FingerprintMapActionUiHelper(display, resourceProvider),
        resourceProvider
    )

    override val configConstraintsViewModel = ConfigConstraintsViewModel(
        viewModelScope,
        display,
        config,
        ConstraintUtils.FINGERPRINT_MAP_ALLOWED_CONSTRAINTS,
        resourceProvider
    )

    override val state = MutableStateFlow<ConfigMappingUiState>(buildUiState(State.Loading))

    private val rebuildUiState = MutableSharedFlow<Unit>()

    init {
        viewModelScope.launch {
            combine(rebuildUiState, config.mapping) { _, mapping ->
                buildUiState(mapping)
            }.collectLatest {
                state.value = it
            }
        }

        runBlocking { rebuildUiState.emit(Unit) } //build the initial state on init
    }

    override fun setEnabled(enabled: Boolean) = config.setEnabled(enabled)

    override fun save() = config.save()

    override fun saveState(outState: Bundle) {
        config.getState().ifIsData { pair ->
            val (id, fingerprintMap) = pair
            outState.putString(STATE_FINGERPRINT_MAP, Json.encodeToString(fingerprintMap))
            outState.putString(STATE_KEY_ID, Json.encodeToString(id))
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun restoreState(state: Bundle) {
        val fingerprintMap =
            state.getJsonSerializable<FingerprintMap>(STATE_FINGERPRINT_MAP) ?: return
        val id = state.getJsonSerializable<FingerprintMapId>(STATE_KEY_ID) ?: return

        config.restoreState(id, fingerprintMap)
    }

    fun loadFingerprintMap(id: FingerprintMapId) {
        viewModelScope.launch {
            config.loadFingerprintMap(id)
        }
    }

    private fun buildUiState(configState: State<FingerprintMap>): ConfigFingerprintMapUiState {
        return when (configState) {
            is State.Data -> ConfigFingerprintMapUiState(configState.data.isEnabled)
            is State.Loading -> ConfigFingerprintMapUiState(isEnabled = false)
        }
    }

    class Factory(
        private val config: ConfigFingerprintMapUseCase,
        private val testAction: TestActionUseCase,
        private val display: DisplaySimpleMappingUseCase,
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.Factory {

        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel?> create(modelClass: Class<T>) =
            ConfigFingerprintMapViewModel(
                config,
                testAction,
                display,
                resourceProvider
            ) as T
    }
}

data class ConfigFingerprintMapUiState(
    override val isEnabled: Boolean
) : ConfigMappingUiState