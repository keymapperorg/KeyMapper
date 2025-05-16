package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.common.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 16/04/2021.
 */

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
