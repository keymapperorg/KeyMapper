package io.github.sds100.keymapper.base.expertmode

import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import javax.inject.Inject
import javax.inject.Named
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@ViewModelScoped
class ExpertModeSetupDelegateImpl @Inject constructor(
    @Named("viewmodel")
    viewModelScope: CoroutineScope,
    useCase: SystemBridgeSetupUseCase,
    private val navigationProvider: NavigationProvider,
) : SystemBridgeSetupDelegateImpl(
    viewModelScope,
    useCase,
) {
    override fun onFinishClick() {
        viewModelScope.launch {
            navigationProvider.popBackStack()
        }
    }
}
