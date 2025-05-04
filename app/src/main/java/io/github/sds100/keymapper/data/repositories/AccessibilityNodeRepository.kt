package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.mapData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

interface AccessibilityNodeRepository {
    val nodes: Flow<State<List<AccessibilityNodeEntity>>>
    fun get(id: Long): AccessibilityNodeEntity?
    fun insert(node: AccessibilityNodeEntity)
    fun deleteAll()
}

class AccessibilityNodeRepositoryImpl(private val coroutineScope: CoroutineScope) : AccessibilityNodeRepository {
    // TODO have a DAO to remember between app launches and so it isn't all cached in memory - also handles IDs automatically.
    // TODO do not insert duplicates where all fields are the same
    override val nodes =
        MutableStateFlow<State<List<AccessibilityNodeEntity>>>(State.Data(ArrayList(128)))

    override fun insert(node: AccessibilityNodeEntity) {
        nodes.update { currentState ->
            currentState.mapData { list ->
                val nodeWithId = node.copy(id = list.size + 1L)
                list.plus(nodeWithId)
            }
        }
    }

    override fun get(id: Long): AccessibilityNodeEntity? {
        val nodes = nodes.value.dataOrNull() ?: return null
        return nodes.find { it.id == id }
    }

    override fun deleteAll() {
        coroutineScope.launch {
            nodes.emit(State.Data(ArrayList(128)))
        }
    }
}
