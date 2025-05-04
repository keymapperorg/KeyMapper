package io.github.sds100.keymapper.system.accessibility

import android.graphics.Rect
import android.os.Build
import android.os.CountDownTimer
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sds100.keymapper.actions.uielement.NodeInteractionType
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AccessibilityNodeRecorder(
    private val nodeRepository: AccessibilityNodeRepository,
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

        val source = event.source ?: return
        val sourceBounds = Rect()
        source.getBoundsInScreen(sourceBounds)

        val root: AccessibilityNodeInfo = source.window.root ?: return

        // This searches for all nodes that are within the bounds of the source of the
        // AccessibilityEvent because the source is not necessarily the element
        // the user wants to tap.
        val entities = getNodesInBounds(root, sourceBounds).toTypedArray()
        nodeRepository.insert(*entities)
    }

    /**
     * Get all the nodes that are within the given bounds.
     */
    private fun getNodesInBounds(
        node: AccessibilityNodeInfo,
        bounds: Rect,
    ): Set<AccessibilityNodeEntity> {
        val set = mutableSetOf<AccessibilityNodeEntity>()

        val nodeBounds = Rect()
        node.getBoundsInScreen(nodeBounds)

        if (bounds.contains(nodeBounds)) {
            val entity = buildNodeEntity(node)

            if (entity != null) {
                set.add(entity)
            }
        }

        if (node.childCount > 0) {
            for (i in 0 until node.childCount) {
                val child = node.getChild(i) ?: continue

                set.addAll(getNodesInBounds(child, bounds))
            }
        }

        return set
    }

    private fun buildNodeEntity(source: AccessibilityNodeInfo): AccessibilityNodeEntity? {
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
        )
    }

    fun teardown() {
        synchronized(timerLock) {
            timer?.cancel()
            timer = null
        }
    }
}
