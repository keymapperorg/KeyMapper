package io.github.sds100.keymapper.mappings.keymaps.detection

import io.github.sds100.keymapper.mappings.ClickType
import io.github.sds100.keymapper.mappings.keymaps.KeyMap
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerMode
import io.github.sds100.keymapper.util.InputEventType

/**
 * Created by sds100 on 21/05/2022.
 */

class EventTreeBuilder(private val keyMaps: List<KeyMap>, private val options: DefaultKeyMapOptions) {

    fun build(): List<KeyEventNode> {
        val eventTrees = mutableListOf<KeyEventNode>()

        for (keyMap in keyMaps) {
            //ignore the key map if it can't do anything
            if (keyMap.actionList.isEmpty()) {
                continue
            }

            if (keyMap.trigger.keys.isEmpty()) {
                continue
            }

            if (keyMap.trigger.mode is TriggerMode.Parallel) {
                eventTrees.add(createParallelTriggerTree(keyMap))
            }
        }

        return eventTrees
    }

    private fun createParallelTriggerTree(keyMap: KeyMap): KeyEventNode {
        val trigger = keyMap.trigger

        require(trigger.mode is TriggerMode.Parallel)

        val longPressDelay = trigger.longPressDelay?.toLong() ?: options.longPressDelay

        val initialActionsJob = JobNode(
            ActionNode(keyMap.actionList.map { it.data }, InputEventType.DOWN_UP)
        )
        val repeatActionsJobs = createRepeatActionsJobs(keyMap)
        var startNode: KeyEventNode? = null

        when (trigger.mode.clickType) {
            ClickType.SHORT_PRESS -> {

                var node: KeyEventNode? = null

                for (triggerKey in trigger.keys) {
                    val nextNode: KeyEventNode

                    if (triggerKey == trigger.keys.last()) {
                        nextNode = KeyEventNode(
                            KeyEventAction.DOWN,
                            listOf(triggerKey.keyCode),
                            consume = true,
                            jobs = listOf(initialActionsJob).plus(repeatActionsJobs)
                        )
                    } else {
                        nextNode = KeyEventNode(
                            KeyEventAction.DOWN,
                            listOf(triggerKey.keyCode),
                            consume = true,
                        )
                    }

                    node?.next = nextNode

                    if (node == null) {
                        startNode = nextNode
                    }

                    node = nextNode
                }
            }

            ClickType.LONG_PRESS -> {
                throw NotImplementedError()
//                val initialActionsJob = JobNode(
//                    ActionNode(
//                        keyMap.actionList.map { it.data },
//                        InputEventType.DOWN_UP,
//                        delay = longPressDelay
//                    )
//                )
//
//                val repeatActionsJobs = createRepeatActionsJobs(keyMap, additionalDelay = longPressDelay)
//
//                val onDownNode = KeyEventNode(
//                    KeyEventAction.DOWN,
//                    triggerKey.keyCode,
//                    consume = true,
//                    jobs = listOf(initialActionsJob).plus(repeatActionsJobs)
//                )
//
//                val timeouts = listOf(
//                    Timeout(
//                        longPressDelay,
//                        onDownNode,
//                        tasks = listOf(ImitateKeyNode(triggerKey.keyCode, InputEventType.DOWN_UP)),
//                        jobsToCancel = listOf(initialActionsJob)
//                    )
//                )
//
//                val onUpNode = KeyEventNode(
//                    KeyEventAction.UP,
//                    triggerKey.keyCode,
//                    consume = true,
//                    jobsToCancel = repeatActionsJobs,
//                    timeouts = timeouts
//                )
//
//                onDownNode.next = onUpNode
//
//                return onDownNode
            }

            ClickType.DOUBLE_PRESS -> {
                throw NotImplementedError()
            }
        }

        return startNode!!
    }

    /**
     * @param additionalDelay Any additional delay on top of the configured repeat delay in the action.
     * E.g long press triggers need to add an extra long press delay.
     */
    private fun createRepeatActionsJobs(keyMap: KeyMap, additionalDelay: Long = 0): List<JobNode> {
        val jobs = mutableListOf<JobNode>()

        for (action in keyMap.actionList) {
            //TODO if multiple repeat actions
            if (action.repeat) {
                val firstRepeatAction =
                    ActionNode(listOf(action.data), InputEventType.DOWN_UP, delay = additionalDelay + 300)
                val repeatedAction =
                    ActionNode(listOf(action.data), InputEventType.DOWN_UP, delay = 50)

                repeatedAction.next = repeatedAction
                firstRepeatAction.next = repeatedAction

                jobs.add(JobNode(firstRepeatAction))
            }
        }

        return jobs
    }
}