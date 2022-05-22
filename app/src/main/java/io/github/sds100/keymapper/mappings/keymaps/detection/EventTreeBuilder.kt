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

            val longPressDelay = trigger.longPressDelay?.toLong() ?: options.longPressDelay

            if (trigger.keys.size == 1) {
                val triggerKey = trigger.keys[0]

                when (triggerKey.clickType) {
                    ClickType.SHORT_PRESS -> {
                        val initialActionsJob = JobNode(
                            ActionNode(keyMap.actionList.map { it.data }, InputEventType.DOWN_UP)
                        )
                        val repeatActionsJobs = createRepeatActionsJobs(keyMap)

                        val onDownNode = KeyEventNode(
                            KeyEventAction.DOWN,
                            triggerKey.keyCode,
                            consume = true,
                            jobs = listOf(initialActionsJob).plus(repeatActionsJobs)
                        )

                        val onUpNode = KeyEventNode(
                            KeyEventAction.UP,
                            triggerKey.keyCode,
                            consume = true,
                            jobsToCancel = repeatActionsJobs
                        )

                        onDownNode.next = onUpNode

                        eventTrees.add(onDownNode)
                    }

                    ClickType.LONG_PRESS -> {
                        val initialActionsJob = JobNode(
                            ActionNode(
                                keyMap.actionList.map { it.data },
                                InputEventType.DOWN_UP,
                                delay = longPressDelay
                            )
                        )

                        val repeatActionsJobs = createRepeatActionsJobs(keyMap, additionalDelay = longPressDelay)

                        val onDownNode = KeyEventNode(
                            KeyEventAction.DOWN,
                            triggerKey.keyCode,
                            consume = true,
                            jobs = listOf(initialActionsJob).plus(repeatActionsJobs)
                        )

                        val timeouts = listOf(
                            Timeout(
                                longPressDelay,
                                onDownNode,
                                tasks = listOf(ImitateKeyNode(triggerKey.keyCode)),
                                jobsToCancel = listOf(initialActionsJob)
                            )
                        )

                        val onUpNode = KeyEventNode(
                            KeyEventAction.UP,
                            triggerKey.keyCode,
                            consume = true,
                            jobsToCancel = repeatActionsJobs,
                            timeouts = timeouts
                        )

                        onDownNode.next = onUpNode

                        eventTrees.add(onDownNode)
                    }

                    ClickType.DOUBLE_PRESS -> {

                    }
                }
            }
        }

        return eventTrees
    }

    /**
     * @param additionalDelay Any additional delay on top of the configured repeat delay in the action.
     * E.g long press triggers need to add an extra long press delay.
     */
    fun createRepeatActionsJobs(keyMap: KeyMap, additionalDelay: Long = 0): List<JobNode> {
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