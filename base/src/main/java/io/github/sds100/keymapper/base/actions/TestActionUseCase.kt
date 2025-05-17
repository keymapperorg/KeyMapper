package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.result.Result
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.util.ServiceEvent



class TestActionUseCaseImpl(
    private val serviceAdapter: ServiceAdapter,
) : TestActionUseCase {
    override suspend fun invoke(action: ActionData): Result<*> =
        serviceAdapter.send(ServiceEvent.TestAction(action))
}

interface TestActionUseCase {
    suspend operator fun invoke(action: ActionData): Result<*>
}
