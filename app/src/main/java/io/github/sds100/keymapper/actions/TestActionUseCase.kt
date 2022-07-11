package io.github.sds100.keymapper.actions

import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.Result
import javax.inject.Inject

/**
 * Created by sds100 on 20/02/2021.
 */

class TestActionUseCaseImpl @Inject constructor(
    private val serviceAdapter: AccessibilityServiceAdapter
) : TestActionUseCase {
    override suspend fun invoke(action: ActionData): Result<*> {
        return serviceAdapter.send(Event.TestAction(action))
    }
}

interface TestActionUseCase {
    suspend operator fun invoke(action: ActionData): Result<*>
}