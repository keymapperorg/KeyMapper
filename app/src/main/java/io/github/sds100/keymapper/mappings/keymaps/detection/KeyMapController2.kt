package io.github.sds100.keymapper.mappings.keymaps.detection

import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job

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
     * The jobs for any node that is waiting to happen after a delay is stored here.
     */
    private val delayedJobMap: Map<DelayedNode, Job> = mutableMapOf()

    /**
     * This is where all the possible event trees are stored. An event tree represents
     * the order in which keys can be pressed to make stuff happen.
     */
    private val eventTrees: List<EventNode> = mutableListOf()

    fun onKeyEvent(
        keyCode: Int,
        action: Int,
        metaState: Int,
        scanCode: Int = 0,
        device: InputDeviceInfo?
    ) {
        
    }

    private sealed interface Node{
        val next: Node?
    }

    private data class ActionNode(
        val action: ActionData,
        override val next: Node? = null
    ) : Node

    private data class DelayedNode(
        val delay: Long,
        val node: Node,
        override val next: Node? = null
    ) : Node

    private data class EventNode(
        val type: EventType,
        val actions: List<ActionData>,
        val delayedNodes: List<DelayedNode>,
        val tasksToCancel: List<DelayedNode>,
        override val next: Node? = null
    ) : Node
    
    private enum class EventType {
        DOWN, UP
    }
}