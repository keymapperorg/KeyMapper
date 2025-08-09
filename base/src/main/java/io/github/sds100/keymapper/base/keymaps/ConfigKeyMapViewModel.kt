package io.github.sds100.keymapper.base.keymaps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.onboarding.OnboardingTapTarget
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.trigger.ConfigTriggerUseCase
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.common.utils.dataOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    val isKeyMapEdited: Boolean
        get() = configKeyMapState.isEdited

    val isEnabled: StateFlow<Boolean> = configTrigger.keyMap
        .map { state -> state.dataOrNull()?.isEnabled ?: true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    val showActionsTapTarget: StateFlow<Boolean> =
        combine(
            onboarding.showTapTarget(OnboardingTapTarget.CHOOSE_ACTION),
            configKeyMapState.keyMap,
        ) { showTapTarget, keyMapState ->
            // Show the choose action tap target if they have recorded a key.
            showTapTarget && keyMapState.dataOrNull()?.trigger?.keys?.isNotEmpty() ?: false
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    val showConstraintsTapTarget: StateFlow<Boolean> =
        combine(
            onboarding.showTapTarget(OnboardingTapTarget.CHOOSE_CONSTRAINT),
            configKeyMapState.keyMap,
        ) { showTapTarget, keyMapState ->
            // Show the choose constraint tap target if they have added an action.
            showTapTarget && keyMapState.dataOrNull()?.actionList?.isNotEmpty() ?: false
        }.stateIn(viewModelScope, SharingStarted.Lazily, false)

    fun onDoneClick() {
        configKeyMapState.save()

        viewModelScope.launch {
            popBackStack()
        }
    }

    fun loadNewKeyMap(floatingButtonUid: String? = null, groupUid: String?) {
        configKeyMapState.loadNewKeyMap(groupUid)
        if (floatingButtonUid != null) {
            viewModelScope.launch {
                configTrigger.addFloatingButtonTriggerKey(floatingButtonUid)
            }
        }
    }

    fun loadKeyMap(uid: String) {
        viewModelScope.launch {
            configKeyMapState.loadKeyMap(uid)
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            popBackStack()
        }
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

    fun onEnabledChanged(enabled: Boolean) {
        configTrigger.setEnabled(enabled)
    }

}
