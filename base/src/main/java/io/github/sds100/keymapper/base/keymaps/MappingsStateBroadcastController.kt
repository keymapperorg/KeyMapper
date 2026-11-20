package io.github.sds100.keymapper.base.keymaps

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.base.IntentApi
import io.github.sds100.keymapper.common.utils.DispatcherProvider
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

/**
 * Tells other apps whether mappings are paused. This broadcasts when the state changes, when
 * the process starts, and when the accessibility service starts.
 */
@Singleton
class MappingsStateBroadcastController @Inject constructor(
    @ApplicationContext private val ctx: Context,
    private val coroutineScope: CoroutineScope,
    private val pauseKeyMapsUseCase: PauseKeyMapsUseCase,
    private val accessibilityServiceAdapter: AccessibilityServiceAdapter,
    private val dispatchers: DispatcherProvider,
) {

    fun start() {
        val onServiceStarted = accessibilityServiceAdapter.state
            .filter { it == AccessibilityServiceState.ENABLED }
            .map { pauseKeyMapsUseCase.isPaused.first() }

        merge(pauseKeyMapsUseCase.isPaused.distinctUntilChanged(), onServiceStarted)
            .onEach { isPaused -> broadcast(isPaused) }
            .flowOn(dispatchers.default())
            .launchIn(coroutineScope)
    }

    private fun broadcast(isPaused: Boolean) {
        val intent = Intent(IntentApi.ACTION_MAPPINGS_STATE_CHANGED)
            .putExtra(IntentApi.EXTRA_MAPPINGS_PAUSED, isPaused)

        ctx.sendBroadcast(intent)

        // Implicit broadcasts don't reach manifest receivers on Android 8+, so target them
        // explicitly.
        ctx.packageManager.queryBroadcastReceivers(intent, 0).forEach { receiver ->
            ctx.sendBroadcast(
                Intent(intent).setClassName(
                    receiver.activityInfo.packageName,
                    receiver.activityInfo.name,
                ),
            )
        }
    }
}
