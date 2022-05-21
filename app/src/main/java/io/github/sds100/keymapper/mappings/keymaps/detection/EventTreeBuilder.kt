package io.github.sds100.keymapper.mappings.keymaps.detection

import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.util.InputEventType

/**
 * Created by sds100 on 21/05/2022.
 */

object EventTreeBuilder{

    fun createEventTrees(keyMaps: List<KeyMap>, options: DefaultKeyMapOptions): List<KeyEventNode> {
        val eventTrees = mutableListOf<KeyEventNode>()

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
                        val onDownTasks = createTasksOnTriggered(keyMap)

                        val onDownNode = KeyEventNode(
                            KeyEventAction.DOWN,
                            triggerKey.keyCode,
                            tasks = onDownTasks,
                            next = KeyEventNode(
                                KeyEventAction.UP,
                                triggerKey.keyCode
                            )
                        )

                        val onUpNode = KeyEventNode(
                            KeyEventAction.UP,
                            triggerKey.keyCode,
                            tasksToCancel = listOf() //todo
                        )

                        onDownNode.next = onUpNode

                        eventTrees.add(onDownNode)
                    }

                    ClickType.LONG_PRESS -> {
                        val onTriggeredTasks = createTasksOnTriggered(keyMap)

                        val longPressDelayNode = DelayNode(
                            500,

                            )

                        val onDownNode = KeyEventNode(
                            KeyEventAction.DOWN,
                            triggerKey.keyCode,

                            )

                    }

                    ClickType.DOUBLE_PRESS -> {

                    }
                }
            }
        }

        return eventTrees
    }

    fun createTasksOnTriggered(keyMap: KeyMap): List<TaskNode> {
        val tasks = mutableListOf<TaskNode>()

        tasks.add(ActionNode(keyMap.actionList.map { it.data }, InputEventType.DOWN_UP))

        for (action in keyMap.actionList) {
            //TODO if multiple repeat actions
            if (action.repeat) {
                val actionNode = ActionNode(
                    keyMap.actionList.map { it.data },
                    inputEventType = InputEventType.DOWN_UP
                )

                // link the delayed node back to the action node so it repeats.
                actionNode.next = DelayNode(50, actionNode)

                val delayRepeatNode = DelayNode(
                    300,
                    actionNode
                )

                tasks.add(delayRepeatNode)
            }
        }

        return tasks
    }
}