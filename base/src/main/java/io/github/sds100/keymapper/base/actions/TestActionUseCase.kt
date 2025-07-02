package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import javax.inject.Inject

class TestActionUseCaseImpl @Inject constructor(
    private val serviceAdapter: AccessibilityServiceAdapter,
) : TestActionUseCase {
    override suspend fun invoke(action: ActionData): KMResult<*> = serviceAdapter.send(TestActionEvent(action))
}

interface TestActionUseCase {
    suspend operator fun invoke(action: ActionData): KMResult<*>
}
