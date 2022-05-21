package io.github.sds100.keymapper.mappings.keymaps.detection

import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.util.InputEventType
import kotlinx.coroutines.Job

/**
 * Created by sds100 on 21/05/2022.
 */

sealed interface Node

sealed class TaskNode(var next: TaskNode?) : Node {
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

class VibrateNode(val duration: Long, next: TaskNode? = null) : TaskNode(next)

class ActionNode(
    val actions: List<ActionData>,
    val inputEventType: InputEventType,
    next: TaskNode? = null
) : TaskNode(next)

class DelayNode(val delay: Long, next: TaskNode? = null) : TaskNode(next)

/**
 * @param device this node will only be triggered if a key event comes from a device matching this.
 *               null if any device is accepted.
 */
class KeyEventNode(
    val type: KeyEventAction,
    val keyCode: Int,
    val device: InputDeviceInfo? = null,
    val tasks: List<TaskNode> = emptyList(),
    val tasksToCancel: List<TaskNode> = emptyList(),
    val timeout: Timeout? = null,
    var next: KeyEventNode? = null
) : Node {
    var eventTime: Long = -1
}

class Timeout(val time: Long, val sinceNode: KeyEventNode, val tasks: List<TaskNode>)