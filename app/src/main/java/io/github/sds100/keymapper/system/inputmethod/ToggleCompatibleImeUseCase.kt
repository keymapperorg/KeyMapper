package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Created by sds100 on 16/04/2021.
 */

class ToggleCompatibleImeUseCaseImpl @Inject constructor(
    private val inputMethodAdapter: InputMethodAdapter
) : ToggleCompatibleImeUseCase {
    private val keyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    override val sufficientPermissions: Flow<Boolean> =
        inputMethodAdapter.isUserInputRequiredToChangeIme

    override suspend fun toggle(): Result<ImeInfo> {
       return keyMapperImeHelper.toggleCompatibleInputMethod()
    }
}

interface ToggleCompatibleImeUseCase {
    val sufficientPermissions: Flow<Boolean>

   suspend fun toggle(): Result<ImeInfo>
}