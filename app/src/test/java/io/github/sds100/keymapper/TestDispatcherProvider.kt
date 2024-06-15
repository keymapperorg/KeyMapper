package io.github.sds100.keymapper

import io.github.sds100.keymapper.util.DispatcherProvider
import kotlinx.coroutines.test.TestCoroutineDispatcher

/**
 * Created by sds100 on 01/05/2021.
 */

class TestDispatcherProvider(
    private val testDispatcher: TestCoroutineDispatcher,
) : DispatcherProvider {
    override fun main() = testDispatcher
    override fun default() = testDispatcher
    override fun io() = testDispatcher
    override fun unconfined() = testDispatcher
}
