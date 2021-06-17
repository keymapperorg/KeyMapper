package io.github.sds100.keymapper.mappings.keymaps.detection

import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.mappings.keymaps.KeyMapAction
import io.github.sds100.keymapper.util.InputEventType
import kotlinx.coroutines.CoroutineScope
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
    fun onTriggered(metaState: Int) {
        /*
        this job shouldn't be cancelled when the trigger is released. all actions should be performed
        once before repeating (if configured).
         */
        coroutineScope.launch {
            actionList.forEachIndexed { actionIndex, action ->

                performAction(action, InputEventType.DOWN_UP, metaState)

                delay(action.delayBeforeNextAction?.toLong() ?: 0L)
            }
        }
    }

    private fun performAction(action: KeyMapAction, inputEventType: InputEventType, metaState: Int) {
        repeat(action.multiplier ?: 1) {
            useCase.perform(action.data, inputEventType, metaState)
        }
    }
}