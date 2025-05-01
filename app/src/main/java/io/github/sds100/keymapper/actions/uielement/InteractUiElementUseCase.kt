package io.github.sds100.keymapper.actions.uielement

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import io.github.sds100.keymapper.system.accessibility.RecordAccessibilityNodeState
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.ServiceEvent
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.mapData
import io.github.sds100.keymapper.util.onFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class InteractUiElementUseCaseImpl(
    private val serviceAdapter: ServiceAdapter,
    private val nodeRepository: AccessibilityNodeRepository,
    private val packageManagerAdapter: PackageManagerAdapter,
) : InteractUiElementUseCase {
    override val recordState: MutableStateFlow<RecordAccessibilityNodeState> =
        MutableStateFlow(RecordAccessibilityNodeState.Idle)

    override val interactionCount: Flow<State<Int>> =
        nodeRepository.nodes.map { state -> state.mapData { it.size } }

    override val interactedPackages: Flow<State<List<String>>> = nodeRepository.nodes.map { state ->
        state.mapData { nodes ->
            nodes.map { it.packageName }.distinct()
        }
    }

    override fun getInteractionsByPackage(packageName: String): Flow<State<List<AccessibilityNodeEntity>>> {
        return nodeRepository.nodes.map { state ->
            state.mapData { nodes ->
                nodes.filter { it.packageName == packageName }
            }
        }
    }

    override fun getAppName(packageName: String): Result<String> = packageManagerAdapter.getAppName(packageName)

    override fun getAppIcon(packageName: String): Result<Drawable> = packageManagerAdapter.getAppIcon(packageName)

    override suspend fun startRecording(): Result<*> {
        // TODO show snackbar when accessibility service is disabled error
        return serviceAdapter.send(ServiceEvent.StartRecordingTrigger)
    }

    override suspend fun stopRecording() {
        serviceAdapter.send(ServiceEvent.StopRecordingNodes).onFailure {
            recordState.update { RecordAccessibilityNodeState.Idle }
        }
    }
}

interface InteractUiElementUseCase {
    val recordState: StateFlow<RecordAccessibilityNodeState>

    val interactionCount: Flow<State<Int>>
    val interactedPackages: Flow<State<List<String>>>
    fun getInteractionsByPackage(packageName: String): Flow<State<List<AccessibilityNodeEntity>>>

    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>

    suspend fun startRecording(): Result<*>
    suspend fun stopRecording()
}
