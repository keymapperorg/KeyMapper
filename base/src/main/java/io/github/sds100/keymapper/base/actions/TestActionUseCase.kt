package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceEvent

class TestActionUseCaseImpl(
    private val serviceAdapter: AccessibilityServiceAdapter,
) : TestActionUseCase {
    override suspend fun invoke(action: ActionData): Result<*> = serviceAdapter.send(TestActionEvent(action))
}

interface TestActionUseCase {
    suspend operator fun invoke(action: ActionData): Result<*>
}
