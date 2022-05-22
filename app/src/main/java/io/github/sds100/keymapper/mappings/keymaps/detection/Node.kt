package io.github.sds100.keymapper.mappings.keymaps.detection

import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
import io.github.sds100.keymapper.system.keyevents.KeyEventUtils
import io.github.sds100.keymapper.util.InputEventType
import kotlinx.coroutines.Job

/**
 * Created by sds100 on 21/05/2022.
 */

class JobNode(val task: TaskNode) {
    private val jobLock: Any = Any()
    private var job: Job? = null

    fun cancel() {
        synchronized(jobLock) {
            job?.cancel()
            job = null
        }
    }

    fun startJob(job: Job) {
        synchronized(jobLock) {
            cancel()
            this.job = job
            job.start()
        }
    }
}

sealed class TaskNode(val delay: Long, var next: TaskNode? = null)

class VibrateNode(val duration: Long, next: TaskNode? = null, delay: Long = -1) : TaskNode(delay)

class ActionNode(
    val actions: List<ActionData>,
    val inputEventType: InputEventType,
    delay: Long = -1,
    next: TaskNode? = null,
) : TaskNode(delay) {
    override fun toString(): String {
        return "delay = $delay, actions = $actions"
    }
}

class ImitateKeyNode(val keyCode: Int, next: TaskNode? = null, delay: Long = -1) : TaskNode(delay, next)

/**
 * @param device this node will only be triggered if a key event comes from a device matching this.
 *               null if any device is accepted.
 */
class KeyEventNode(
    val type: KeyEventAction,
    val keyCode: Int,
    val consume: Boolean,
    val device: InputDeviceInfo? = null,
    val jobs: List<JobNode> = emptyList(),
    val jobsToCancel: List<JobNode> = emptyList(),
    val timeouts: List<Timeout> = emptyList(),
    var next: KeyEventNode? = null
) {
    var eventTime: Long = -1

    override fun toString(): String {
        val typeString = when (type) {
            KeyEventAction.DOWN -> "DOWN"
            KeyEventAction.UP -> "UP"
        }

        val keyCodeString = KeyEventUtils.keyCodeToString(keyCode)

        return "$typeString $keyCodeString consume = $consume"
    }
}

class Timeout(
    val time: Long,
    val sinceEvent: KeyEventNode,
    val tasks: List<TaskNode> = emptyList(),
    val jobsToCancel: List<JobNode> = emptyList()
)