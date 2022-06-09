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

class ImitateKeyNode(
    val keyCode: Int,
    val inputEventType: InputEventType,
    next: TaskNode? = null,
    delay: Long = -1
) : TaskNode(delay, next)

sealed class EventNode {
    class DownEventNode(val event: Event) {

    }

    class UpEventNode(val ) {
        
    }
}

data class Event(val keyCode: Int, val device: InputDeviceInfo? = null)

/**
 * @param device this node will only be triggered if a key event comes from a device matching this.
 *               null if any device is accepted.
 */
class KeyEventNode(
    val type: KeyEventAction,
    /**
     * Which key codes should this node match. E.g there would be multiple key codes
     * here for the UP event for a parallel trigger with multiple keys. The keys
     * can be released in any order and only one needs to be released for the key map
     * to stop.
     */
    val keyCodes: List<Int>,
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

        val keyCodeString = keyCodes.joinToString { KeyEventUtils.keyCodeToString(it) }

        return "$typeString $keyCodeString consume = $consume"
    }
}

class Timeout(
    val time: Long,
    val sinceEvent: KeyEventNode,
    val tasks: List<TaskNode> = emptyList(),
    val jobsToCancel: List<JobNode> = emptyList()
)