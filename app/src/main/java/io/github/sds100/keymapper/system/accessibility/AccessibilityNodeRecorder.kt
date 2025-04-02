package io.github.sds100.keymapper.system.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AccessibilityNodeRecorder(
    private val service: AccessibilityService,
    private val coroutineScope: CoroutineScope,
) {
    companion object {
        private const val RECORD_DURATION = 60000L
    }

    private val nodeRepository: AccessibilityNodeRepository by lazy {
        ServiceLocator.accessibilityNodeRepository(service)
    }

    private var recordJob: Job? = null
    private val _isRecording = MutableStateFlow(false)
    val isRecording = _isRecording.asStateFlow()

    fun startRecording() {
        _isRecording.update { true }
        recordJob?.cancel()
        recordJob = recordJob()
    }

    fun stopRecording() {
        recordJob?.cancel()
        recordJob = null
        _isRecording.update { false }
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!isRecording.value) {
            return
        }

        val source = event.source ?: return

        val entity =
            AccessibilityNodeEntity(
                packageName = event.packageName.toString(),
                text = source.text.firstOrNull()?.toString(),
                contentDescription = source.contentDescription?.toString(),
                className = source.className?.toString(),
                viewResourceId = source.viewIdResourceName,
                uniqueId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    source.uniqueId
                } else {
                    null
                },
                actions = source.actionList?.map { it.id } ?: emptyList(),
            )

        nodeRepository.insert(entity)
    }

    private fun recordJob() = coroutineScope.launch {
        delay(RECORD_DURATION)
        _isRecording.update { false }
    }
}
