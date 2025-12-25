package io.github.sds100.keymapper.base.utils

import io.github.sds100.keymapper.common.utils.Clock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime

class TestScopeClock(private val testScope: TestScope) : Clock {
    @OptIn(ExperimentalCoroutinesApi::class)
    override fun elapsedRealtime(): Long {
        return testScope.currentTime
    }
}
