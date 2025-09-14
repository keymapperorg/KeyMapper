package io.github.sds100.keymapper.base.trigger

import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

@ViewModelScoped
class SetupInputMethodUseCaseImpl @Inject constructor(
    private val inputMethodAdapter: InputMethodAdapter,
    private val buildConfigProvider: BuildConfigProvider
) : SetupInputMethodUseCase {
    private val keyMapperImeHelper =
        KeyMapperImeHelper(inputMethodAdapter, buildConfigProvider.packageName)

    override val isEnabled: Flow<Boolean> = keyMapperImeHelper.isCompatibleImeEnabledFlow

    override suspend fun enableInputMethod() {
        keyMapperImeHelper.enableCompatibleInputMethods()
    }

    override val isChosen: Flow<Boolean> = keyMapperImeHelper.isCompatibleImeChosenFlow

    override suspend fun chooseInputMethod() {
        keyMapperImeHelper.chooseCompatibleInputMethod()
    }
}

interface SetupInputMethodUseCase {
    val isEnabled: Flow<Boolean>
    suspend fun enableInputMethod()

    val isChosen: Flow<Boolean>
    suspend fun chooseInputMethod()
}
