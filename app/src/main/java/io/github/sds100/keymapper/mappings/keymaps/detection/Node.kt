package io.github.sds100.keymapper.mappings.keymaps.detection

import io.github.sds100.keymapper.actions.ActionData
import io.github.sds100.keymapper.system.devices.InputDeviceInfo
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
    next: JobNode? = null,
    delay: Long = -1,
) : TaskNode(delay)

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
    val timeout: Timeout? = null,
    var next: KeyEventNode? = null
) {
    var eventTime: Long = -1
}

class Timeout(val time: Long, val sinceNode: KeyEventNode, val tasks: List<JobNode>)