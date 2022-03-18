package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import kotlinx.coroutines.flow.first

/**
 * Created by sds100 on 25/07/2021.
 */

class CreateActionUseCaseImpl(
    private val inputMethodAdapter: InputMethodAdapter
) : CreateActionUseCase {
    override suspend fun getInputMethods(): List<ImeInfo> {
        return inputMethodAdapter.inputMethods.first()
    }
}

interface CreateActionUseCase {
    suspend fun getInputMethods(): List<ImeInfo>
}