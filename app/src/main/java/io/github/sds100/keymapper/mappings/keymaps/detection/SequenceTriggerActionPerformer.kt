package io.github.sds100.keymapper.mappings.keymaps.detection

import io.github.sds100.keymapper.actions.Action
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.util.InputEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 16/06/2021.
 */
class SequenceTriggerActionPerformer(
    private val coroutineScope: CoroutineScope,
    private val useCase: PerformActionsUseCase,
    private val actionList: List<Action>,
) {
    private var job: Job? = null

    fun onTriggered(metaState: Int) {
        /*
        this job shouldn't be cancelled when the trigger is released. all actions should be performed
        once before repeating (if configured).
         */
        job?.cancel()
        job = coroutineScope.launch {
            for (action in actionList) {
                performAction(action, metaState)

                delay(action.delayBeforeNextAction?.toLong() ?: 0L)
            }
        }
    }

    fun reset() {
        job?.cancel()
        job = null
    }

    private suspend fun performAction(action: Action, metaState: Int) {
        repeat(action.multiplier ?: 1) {
            useCase.perform(action.data, InputEventType.DOWN_UP, metaState)
        }
    }
}
