package io.github.sds100.keymapper.base.keymaps

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.sds100.keymapper.base.actions.ConfigActionsViewModel
import io.github.sds100.keymapper.base.constraints.ConfigConstraintsViewModel
import io.github.sds100.keymapper.base.onboarding.OnboardingTapTarget
import io.github.sds100.keymapper.base.onboarding.OnboardingUseCase
import io.github.sds100.keymapper.base.trigger.BaseConfigTriggerViewModel
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.ui.PopupViewModel
import io.github.sds100.keymapper.common.utils.dataOrNull
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

abstract class BaseConfigKeyMapViewModel(
    private val config: ConfigKeyMapUseCase,
    private val onboarding: OnboardingUseCase,
    navigationProvider: NavigationProvider,
    popupViewModel: PopupViewModel,
) : ViewModel(),
    NavigationProvider by navigationProvider,
    PopupViewModel by popupViewModel {

    abstract val configActionsViewModel: ConfigActionsViewModel
    abstract val configTriggerViewModel: BaseConfigTriggerViewModel
    abstract val configConstraintsViewModel: ConfigConstraintsViewModel

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

    fun onDoneClick() {
        config.save()

        viewModelScope.launch {
            popBackStack()
        }
    }

    fun loadNewKeyMap(floatingButtonUid: String? = null, groupUid: String?) {
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

    fun onBackClick() {
        viewModelScope.launch {
            popBackStack()
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
}
