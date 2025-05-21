package io.github.sds100.keymapper.base.system.inputmethod

import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleCompatibleImeUseCaseImpl @Inject constructor(
    private val inputMethodAdapter: InputMethodAdapter,
    private val buildConfigProvider: BuildConfigProvider,
) : ToggleCompatibleImeUseCase {
    private val keyMapperImeHelper =
        KeyMapperImeHelper(inputMethodAdapter, buildConfigProvider.packageName)

    override val sufficientPermissions: Flow<Boolean> =
        inputMethodAdapter.isUserInputRequiredToChangeIme

    override suspend fun toggle(): Result<ImeInfo> = keyMapperImeHelper.toggleCompatibleInputMethod()
}

interface ToggleCompatibleImeUseCase {
    val sufficientPermissions: Flow<Boolean>

    suspend fun toggle(): Result<ImeInfo>
}
