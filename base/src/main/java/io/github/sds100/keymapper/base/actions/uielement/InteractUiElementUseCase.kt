package io.github.sds100.keymapper.base.actions.uielement

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.base.system.accessibility.RecordAccessibilityNodeEvent
import io.github.sds100.keymapper.base.system.accessibility.RecordAccessibilityNodeState
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.mapData
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.data.repositories.AccessibilityNodeRepository
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InteractUiElementController @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val serviceAdapter: AccessibilityServiceAdapter,
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
            .filterIsInstance<RecordAccessibilityNodeEvent.OnRecordNodeStateChanged>()
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

    override fun getAppName(packageName: String): KMResult<String> = packageManagerAdapter.getAppName(packageName)

    override fun getAppIcon(packageName: String): KMResult<Drawable> = packageManagerAdapter.getAppIcon(packageName)

    override suspend fun startRecording(): KMResult<*> {
        nodeRepository.deleteAll()
        return serviceAdapter.send(RecordAccessibilityNodeEvent.StartRecordingNodes)
    }

    override suspend fun stopRecording() {
        serviceAdapter.send(RecordAccessibilityNodeEvent.StopRecordingNodes).onFailure {
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

    fun getAppName(packageName: String): KMResult<String>
    fun getAppIcon(packageName: String): KMResult<Drawable>

    suspend fun startRecording(): KMResult<*>
    suspend fun stopRecording()

    fun startService(): Boolean
}
