package io.github.sds100.keymapper.mappings.keymaps.detection

import android.view.KeyEvent
import io.github.sds100.keymapper.actions.PerformActionsUseCase
import io.github.sds100.keymapper.constraints.DetectConstraintsUseCase
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
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

    /**
     * Pointers to the first nodes in each event tree.
     */
    private var eventTreeStartNodes: List<KeyEventNode> = emptyList()

    /**
     * Keeps track of the modifier keys that have been pressed down from key events
     * and actions.
     */
    private var metaState = 0

    init {
        combine(detectKeyMapsUseCase.allKeyMapList, detectKeyMapsUseCase.defaultOptions) { keyMaps, options ->
            EventTreeBuilder(keyMaps, options).build()
        }.onEach {
            eventTrees = it
            eventTreeLocations = eventTrees.toMutableList() //set the pointer to the initial node
            eventTreeStartNodes = eventTrees.toList() //set the pointer to the initial node
        }.launchIn(coroutineScope)
    }

    fun onKeyEvent(
        keyCode: Int,
        keyEventAction: Int,
        metaState: Int,
        scanCode: Int = 0,
        device: InputDeviceInfo?
    ): Boolean {
        var consume = false

        for ((eventTreeIndex, eventNode) in eventTreeLocations.withIndex()) {
            val keyEventMatchesEventNode = keyCode in eventNode.keyCodes
                && (eventNode.device == null || device == eventNode.device)
                && (
                (keyEventAction == KeyEvent.ACTION_DOWN && eventNode.type == KeyEventAction.DOWN)
                    || (keyEventAction == KeyEvent.ACTION_UP && eventNode.type == KeyEventAction.UP)
                )

            if (!keyEventMatchesEventNode) {
                continue
            }

            eventNode.eventTime = detectKeyMapsUseCase.currentTime

            for (jobNode in eventNode.jobs) {
                jobNode.cancel()

                val job = coroutineScope.launch(start = CoroutineStart.LAZY) {
                    doTaskNode(jobNode.task, scanCode, device)
                }

                jobNode.startJob(job)
            }

            eventNode.jobsToCancel.forEach { it.cancel() }

            if (eventNode.next == null) { //if at the last event in the trigger
                eventTreeLocations[eventTreeIndex] = eventTreeStartNodes[eventTreeIndex]
            } else {
                eventTreeLocations[eventTreeIndex] = eventNode.next!!
            }

            for (timeout in eventNode.timeouts) {
                if (detectKeyMapsUseCase.currentTime - timeout.sinceEvent.eventTime < timeout.time) {
                    timeout.jobsToCancel.forEach { it.cancel() }

                    coroutineScope.launch {
                        timeout.tasks.forEach { doTaskNode(it, scanCode, device) }
                    }
                }
            }

            if (eventNode.consume) {
                consume = true
            }
        }

        return consume
    }

    fun reset() {
        //todo
    }

    private tailrec suspend fun doTaskNode(node: TaskNode, scanCode: Int, device: InputDeviceInfo?) {
        if (node.delay != -1L) {
            delay(node.delay)
        }

        when (node) {
            is ActionNode -> {
                node.actions.forEach { action ->
                    performActionsUseCase.perform(action)
                }
            }

            is VibrateNode -> {
                detectKeyMapsUseCase.vibrate(node.duration)
            }

            is ImitateKeyNode -> {
                detectKeyMapsUseCase.imitateButtonPress(
                    node.keyCode,
                    metaState,
                    device?.id ?: 0,
                    node.inputEventType,
                    scanCode
                )
            }
        }

        if (node.next != null) {
            doTaskNode(node.next!!, scanCode, device)
        }
    }
}