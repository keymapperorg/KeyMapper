package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.system.service.ServiceAdapter
import io.github.sds100.keymapper.base.utils.ServiceEvent



class TestActionUseCaseImpl(
    private val serviceAdapter: ServiceAdapter,
) : TestActionUseCase {
    override suspend fun invoke(action: ActionData): Result<*> =
        serviceAdapter.send(ServiceEvent.TestAction(action))
}

interface TestActionUseCase {
    suspend operator fun invoke(action: ActionData): Result<*>
}
