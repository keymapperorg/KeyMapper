package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.common.result.Result
import kotlinx.coroutines.flow.Flow



class ToggleCompatibleImeUseCaseImpl(
    private val inputMethodAdapter: InputMethodAdapter,
) : ToggleCompatibleImeUseCase {
    private val keyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    override val sufficientPermissions: Flow<Boolean> =
        inputMethodAdapter.isUserInputRequiredToChangeIme

    override suspend fun toggle(): Result<ImeInfo> =
        keyMapperImeHelper.toggleCompatibleInputMethod()
}

interface ToggleCompatibleImeUseCase {
    val sufficientPermissions: Flow<Boolean>

    suspend fun toggle(): Result<ImeInfo>
}
