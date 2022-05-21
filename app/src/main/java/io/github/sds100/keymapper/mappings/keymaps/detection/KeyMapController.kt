package io.github.sds100.keymapper.mappings.keymaps.detection

import android.view.KeyEvent
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 06/05/2022.
 */
class KeyMapController(
    private val coroutineScope: CoroutineScope,
    private val detectKeyMapsUseCase: DetectKeyMapsUseCase,
    private val performActionsUseCase: PerformActionsUseCase,
    private val detectConstraints: DetectConstraintsUseCase
) {

    /**
     * This is where all the possible event trees are stored. An event tree represents
     * the order in which keys can be pressed to make stuff happen.
     */
    private var eventTrees: List<KeyEventNode> = emptyList()

    /**
     * This stores the next event that is expected in each tree.
     */
    private var eventTreeLocations: MutableList<KeyEventNode> = mutableListOf()

    private val defaultKeyMapOptions: StateFlow<DefaultKeyMapOptions> = 

    init {
        detectKeyMapsUseCase.allKeyMapList.onEach { keyMaps ->
            eventTrees = EventTreeBuilder.createEventTrees(keyMaps)
            // set the locations to the initial node
            eventTreeLocations = eventTrees.toMutableList()
        }.launchIn(coroutineScope)
    }

    fun onKeyEvent(
        keyCode: Int,
        keyEventAction: Int,
        metaState: Int,
        scanCode: Int = 0,
        device: InputDeviceInfo?
    ): Boolean {

        for ((eventTreeIndex, eventNode) in eventTreeLocations.withIndex()) {
            val keyEventMatchesEventNode = keyCode == eventNode.keyCode
                && (eventNode.device == null || device == eventNode.device)
                && (
                (keyEventAction == KeyEvent.ACTION_DOWN && eventNode.type == KeyEventAction.DOWN)
                    ||
                    (keyEventAction == KeyEvent.ACTION_UP && eventNode.type == KeyEventAction.UP)
                )

            if (!keyEventMatchesEventNode) {
                continue
            }

            eventNode.eventTime = detectKeyMapsUseCase.currentTime

            for (taskNode in eventNode.tasks) {
                taskNode.cancel()

                val job = coroutineScope.launch {
                    doTaskNode(taskNode)
                }

                taskNode.setJob(job)
            }

            eventNode.tasksToCancel.forEach { it.cancel() }

            if (eventNode.next != null) {
                eventTreeLocations[eventTreeIndex] = eventNode.next!!
            }
        }

        return true
    }

    fun reset() {
        //todo
    }

    private suspend fun doTaskNode(node: TaskNode) {
        when (node) {
            is ActionNode -> {
                node.actions.forEach { action ->
                    performActionsUseCase.perform(action)
                }
            }
            is DelayNode -> {
                delay(node.delay)

                if (node.next != null) {
                    doTaskNode(node.next!!)
                }
            }
        }

        if (node.next != null) {
            doTaskNode(node.next!!)
        }
    }



}