package io.github.sds100.keymapper.base.keymaps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.onboarding.OnboardingTapTarget
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.trigger.ConfigTriggerUseCase
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ConfigKeyMapViewModel @Inject constructor(
    private val configKeyMapState: ConfigKeyMapState,
    private val configTrigger: ConfigTriggerUseCase,
    private val onboarding: OnboardingUseCase,
    navigationProvider: NavigationProvider,
    dialogProvider: DialogProvider,
) : ViewModel(),
    NavigationProvider by navigationProvider,
    DialogProvider by dialogProvider {

    private val isEditingName = MutableStateFlow(false)

    private val isEnabled: Flow<Boolean> = configTrigger.keyMap
        .map { state -> state.dataOrNull()?.isEnabled ?: true }

    private val keyMapName: Flow<String> = configKeyMapState.keyMap
        .map { state -> state.dataOrNull()?.name ?: "" }

    private val showActionsTapTarget: Flow<Boolean> =
        combine(
            onboarding.showTapTarget(OnboardingTapTarget.CHOOSE_ACTION),
            configKeyMapState.keyMap.filterIsInstance<State.Data<KeyMap>>(),
        ) { showTapTarget, keyMapState ->
            // Show the choose action tap target if they have recorded a key and
            // have no actions.
            showTapTarget &&
                keyMapState.data.trigger.keys.isNotEmpty() &&
                keyMapState.data.actionList.isEmpty()
        }

    val state: StateFlow<ConfigKeyMapScreenState> = combine(
        isEnabled,
        showActionsTapTarget,
        keyMapName,
        isEditingName,
        configKeyMapState.undoRedoState,
        ::ConfigKeyMapScreenState,
    ).stateIn(viewModelScope, SharingStarted.Eagerly, ConfigKeyMapScreenState())

    fun loadNewKeyMap(groupUid: String?) {
        configKeyMapState.loadNewKeyMap(groupUid)
    }

    fun loadKeyMap(uid: String) {
        viewModelScope.launch {
            configKeyMapState.loadKeyMap(uid)
        }
    }

    fun onBackClick() {
        configKeyMapState.save()

        viewModelScope.launch {
            popBackStack()
        }
    }

    fun onEditNameClick() {
        isEditingName.update { true }
    }

    fun onConfirmNameClick(name: String) {
        configKeyMapState.update { it.copy(name = name.trim()) }
        isEditingName.update { false }
    }

    fun onCancelEditNameClick() {
        isEditingName.update { false }
    }

    fun onUndoClick() {
        configKeyMapState.undo()
    }

    fun onRedoClick() {
        configKeyMapState.redo()
    }

    fun onEnabledChanged(enabled: Boolean) {
        configTrigger.setEnabled(enabled)
    }

    override fun onCleared() {
        configKeyMapState.save()
        super.onCleared()
    }
}
