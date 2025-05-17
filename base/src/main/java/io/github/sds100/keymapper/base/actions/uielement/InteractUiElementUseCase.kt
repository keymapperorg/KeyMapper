package io.github.sds100.keymapper.base.actions.uielement

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import io.github.sds100.keymapper.system.accessibility.RecordAccessibilityNodeState
import io.github.sds100.keymapper.system.service.ServiceAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.base.utils.ServiceEvent
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.mapData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

class InteractUiElementController(
    private val coroutineScope: CoroutineScope,
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

    init {
        serviceAdapter.eventReceiver
            .filterIsInstance<ServiceEvent.OnRecordNodeStateChanged>()
            .onEach { event -> recordState.update { event.state } }
            .launchIn(coroutineScope)
    }

    override fun getInteractionsByPackage(packageName: String): Flow<State<List<AccessibilityNodeEntity>>> {
        return nodeRepository.nodes.map { state ->
            state.mapData { nodes ->
                nodes.filter { it.packageName == packageName }
            }
        }
    }

    override suspend fun getInteractionById(id: Long): AccessibilityNodeEntity? {
        return nodeRepository.get(id)
    }

    override fun getAppName(packageName: String): Result<String> = packageManagerAdapter.getAppName(packageName)

    override fun getAppIcon(packageName: String): Result<Drawable> = packageManagerAdapter.getAppIcon(packageName)

    override suspend fun startRecording(): Result<*> {
        nodeRepository.deleteAll()
        return serviceAdapter.send(ServiceEvent.StartRecordingNodes)
    }

    override suspend fun stopRecording() {
        serviceAdapter.send(ServiceEvent.StopRecordingNodes).onFailure {
            recordState.update { RecordAccessibilityNodeState.Idle }
        }
    }

    override fun startService(): Boolean {
        return serviceAdapter.start()
    }
}

interface InteractUiElementUseCase {
    val recordState: StateFlow<RecordAccessibilityNodeState>

    val interactionCount: Flow<State<Int>>
    val interactedPackages: Flow<State<List<String>>>
    fun getInteractionsByPackage(packageName: String): Flow<State<List<AccessibilityNodeEntity>>>
    suspend fun getInteractionById(id: Long): AccessibilityNodeEntity?

    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>

    suspend fun startRecording(): Result<*>
    suspend fun stopRecording()

    fun startService(): Boolean
}
