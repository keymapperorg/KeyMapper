package io.github.sds100.keymapper.mappings.detection

import io.github.sds100.keymapper.mappings.keymaps.KeyMapAction
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
    private val actionList: List<KeyMapAction>,
) {
    private var job: Job? = null

    fun onTriggered(metaState: Int) {
        /*
        this job shouldn't be cancelled when the trigger is released. all actions should be performed
        once before repeating (if configured).
         */
        job?.cancel()
        job = coroutineScope.launch {
            actionList.forEach { action ->
                performAction(action, metaState)

                delay(action.delayBeforeNextAction?.toLong() ?: 0L)
            }
        }
    }

    fun reset() {
        job?.cancel()
        job = null
    }

    private fun performAction(action: KeyMapAction, metaState: Int) {
        repeat(action.multiplier ?: 1) {
            useCase.perform(action.data, InputEventType.DOWN_UP, metaState)
        }
    }
}