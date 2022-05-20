package io.github.sds100.keymapper.mappings.keymaps.detection

import android.view.KeyEvent
import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.util.InputEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Created by sds100 on 06/05/2022.
 */
class KeyMapController2(
    private val coroutineScope: CoroutineScope,
    private val detectKeyMapsUseCase: DetectKeyMapsUseCase,
    private val performActionsUseCase: PerformActionsUseCase,
    private val detectConstraints: DetectConstraintsUseCase
) {

    /**
     * Maps an event node to the time when it happens.
     */
    private val eventTimeMap: Map<EventNode, Long> = mutableMapOf()

    /**
     * This is where all the possible event trees are stored. An event tree represents
     * the order in which keys can be pressed to make stuff happen.
     */
    private var eventTrees: List<EventNode> = emptyList()
    private var eventTreeLocations: List<EventNode> = emptyList()

    init {
        detectKeyMapsUseCase.allKeyMapList.onEach { keyMaps ->
            eventTrees = createEventTrees(keyMaps)
            // set the locations to the initial node
            eventTreeLocations = eventTrees.toList()
        }.launchIn(coroutineScope)
    }

    fun onKeyEvent(
        keyCode: Int,
        keyEventAction: Int,
        metaState: Int,
        scanCode: Int = 0,
        device: InputDeviceInfo?
    ): Boolean {

        for (eventNode in eventTreeLocations) {
            val keyEventMatchesEventNode = keyCode == eventNode.keyCode
                && (eventNode.device == null || device == eventNode.device)
                && (
                (keyEventAction == KeyEvent.ACTION_DOWN && eventNode.type == EventType.DOWN)
                    ||
                    (keyEventAction == KeyEvent.ACTION_UP && eventNode.type == EventType.UP)
                )

            if (!keyEventMatchesEventNode) {
                continue
            }

            for (taskNode in eventNode.tasks) {
                taskNode.cancel()

                val job = coroutineScope.launch {
                    doTaskNode(taskNode)
                }

                taskNode.setJob(job)
            }

            eventNode.tasksToCancel.forEach { it.cancel() }
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

            is DelayedNode -> {
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

    private fun createEventTrees(keyMaps: List<KeyMap>): List<EventNode> {
        val eventTrees = mutableListOf<EventNode>()

        for (keyMap in keyMaps) {
            //ignore the key map if it can't do anything
            if (keyMap.actionList.isEmpty()) {
                continue
            }

            val trigger = keyMap.trigger

            if (trigger.keys.size == 1) {
                val triggerKey = trigger.keys[0]

                when (triggerKey.clickType) {
                    ClickType.SHORT_PRESS -> {
                        val onDownTasks = mutableListOf<TaskNode>()

                        onDownTasks.add(ActionNode(keyMap.actionList.map { it.data }, InputEventType.DOWN_UP))

                        for (action in keyMap.actionList) {
                            //TODO if multiple repeat actions
                            if (action.repeat) {
                                val actionNode = ActionNode(
                                    keyMap.actionList.map { it.data },
                                    inputEventType = InputEventType.DOWN_UP
                                )

                                // link the delayed node back to the action node so it repeats.
                                actionNode.next = DelayedNode(50, actionNode)

                                val delayRepeatNode = DelayedNode(
                                    300,
                                    actionNode
                                )

                                onDownTasks.add(delayRepeatNode)
                            }
                        }

                        val onDownNode = EventNode(
                            EventType.DOWN,
                            triggerKey.keyCode,
                            tasks = onDownTasks,
                            next = EventNode(
                                EventType.UP,
                                triggerKey.keyCode
                            )
                        )

                        val onUpNode = EventNode(
                            EventType.UP,
                            triggerKey.keyCode,
                            tasksToCancel = listOf() //todo
                        )

                        onDownNode.next = onUpNode

                        eventTrees.add(onDownNode)
                    }

                    ClickType.LONG_PRESS -> {

                    }

                    ClickType.DOUBLE_PRESS -> {

                    }
                }
            }
        }

        return eventTrees
    }

    private sealed interface Node

    private sealed class TaskNode(
        var next: TaskNode?
    ) : Node {
        private var job: Job? = null

        fun cancel() {
            job?.cancel()
            job = null
        }

        fun setJob(job: Job) {
            cancel()
            this.job = job
        }
    }

    private class ActionNode(
        val actions: List<ActionData>,
        val inputEventType: InputEventType,
        next: TaskNode? = null
    ) : TaskNode(next)

    private class DelayedNode(
        val delay: Long,
        next: TaskNode? = null
    ) : TaskNode(next)

    /**
     * @param device this node will only be triggered if a key event comes from a device matching this.
     *               null if any device is accepted.
     */
    private data class EventNode(
        val type: EventType,
        val keyCode: Int,
        val device: InputDeviceInfo? = null,
        val tasks: List<TaskNode> = emptyList(),
        val tasksToCancel: List<TaskNode> = emptyList(),
        var next: Node? = null
    ) : Node

    private enum class EventType {
        DOWN, UP
    }
}