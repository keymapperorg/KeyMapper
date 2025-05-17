package io.github.sds100.keymapper.base.system.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.CountDownTimer
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import io.github.sds100.keymapper.system.accessibility.NodeInteractionType
import io.github.sds100.keymapper.system.accessibility.RecordAccessibilityNodeState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AccessibilityNodeRecorder(
    private val nodeRepository: AccessibilityNodeRepository,
    private val service: AccessibilityService,
) {
    companion object {
        private const val RECORD_DURATION = 60000L
    }

    private val timerLock = Any()
    private var timer: CountDownTimer? = null
    private val _recordState: MutableStateFlow<RecordAccessibilityNodeState> =
        MutableStateFlow(RecordAccessibilityNodeState.Idle)
    val recordState = _recordState.asStateFlow()

    fun startRecording() {
        synchronized(timerLock) {
            timer?.cancel()
            timer = object : CountDownTimer(RECORD_DURATION, 1000) {

                override fun onTick(millisUntilFinished: Long) {
                    _recordState.update {
                        RecordAccessibilityNodeState.CountingDown(
                            timeLeft = (millisUntilFinished / 1000).toInt(),
                        )
                    }
                }

                override fun onFinish() {
                    _recordState.update { RecordAccessibilityNodeState.Idle }
                }
            }

            timer!!.start()
        }
    }

    fun stopRecording() {
        synchronized(timerLock) {
            timer?.cancel()
            timer = null
            _recordState.update { RecordAccessibilityNodeState.Idle }
        }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (_recordState.value is RecordAccessibilityNodeState.Idle) {
            return
        }

        if (event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED ||
            event.eventType == AccessibilityEvent.TYPE_VIEW_FOCUSED
        ) {
            val source = event.source ?: return

            buildNodeEntity(source, interacted = true)?.also { nodeRepository.insert(it) }
        } else if (event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED) {
            // Only dump the whole window when a window is added because there can be
            // many windows changed events sent in rapid succession.
            val windowRoot: AccessibilityNodeInfo = service.rootInActiveWindow ?: return

            // This searches for all nodes that are within the bounds of the source of the
            // AccessibilityEvent because the source is not necessarily the element
            // the user wants to tap.
            val entities = getNodesRecursively(windowRoot).toTypedArray()
            nodeRepository.insert(*entities)
        }
    }

    private fun getNodesRecursively(
        node: AccessibilityNodeInfo,
    ): Set<AccessibilityNodeEntity> {
        val set = mutableSetOf<AccessibilityNodeEntity>()

        val entity = buildNodeEntity(node, interacted = false)

        if (entity != null) {
            set.add(entity)
        }

        if (node.childCount > 0) {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue

                set.addAll(getNodesRecursively(child))
            }
        }

        return set
    }

    /**
     * @param interacted Whether the user interacted with this node.
     */
    private fun buildNodeEntity(
        source: AccessibilityNodeInfo,
        interacted: Boolean,
    ): AccessibilityNodeEntity? {
        val interactionTypes = source.actionList.mapNotNull { action ->
            NodeInteractionType.entries.find { it.accessibilityActionId == action.id }
        }.distinct()

        if (interactionTypes.isEmpty()) {
            return null
        }

        return AccessibilityNodeEntity(
            packageName = source.packageName.toString(),
            text = source.text?.toString(),
            contentDescription = source.contentDescription?.toString(),
            className = source.className?.toString(),
            viewResourceId = source.viewIdResourceName,
            uniqueId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                source.uniqueId
            } else {
                null
            },
            actions = interactionTypes.toSet(),
            interacted = interacted,
            tooltip = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                source.tooltipText?.toString()
            } else {
                null
            },
            hint = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                source.hintText?.toString()
            } else {
                null
            },
        )
    }

    fun teardown() {
        synchronized(timerLock) {
            timer?.cancel()
            timer = null
        }
    }
}
