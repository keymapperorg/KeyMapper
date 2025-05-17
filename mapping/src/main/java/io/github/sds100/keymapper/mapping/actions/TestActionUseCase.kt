package io.github.sds100.keymapper.mapping.actions

import io.github.sds100.keymapper.common.util.result.Result
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.base.util.ServiceEvent



class TestActionUseCaseImpl(
    private val serviceAdapter: ServiceAdapter,
) : TestActionUseCase {
    override suspend fun invoke(action: ActionData): Result<*> =
        serviceAdapter.send(ServiceEvent.TestAction(action))
}

interface TestActionUseCase {
    suspend operator fun invoke(action: ActionData): Result<*>
}
