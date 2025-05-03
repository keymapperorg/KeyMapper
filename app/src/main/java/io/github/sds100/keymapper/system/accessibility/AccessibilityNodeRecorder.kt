package io.github.sds100.keymapper.system.accessibility

import android.os.Build
import android.os.CountDownTimer
import android.view.accessibility.AccessibilityEvent
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

        if (source.actionList.isNullOrEmpty()) {
            return
        }

        val entity =
            AccessibilityNodeEntity(
                packageName = event.packageName.toString(),
                text = source.text?.toString(),
                contentDescription = source.contentDescription?.toString(),
                className = source.className?.toString(),
                viewResourceId = source.viewIdResourceName,
                uniqueId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    source.uniqueId
                } else {
                    null
                },
                actions = source.actionList?.map { it.id } ?: emptyList(),
                userInteractedActionId = event.action,
            )

        nodeRepository.insert(entity)
    }

    fun teardown() {
        synchronized(timerLock) {
            timer?.cancel()
            timer = null
        }
    }
}
