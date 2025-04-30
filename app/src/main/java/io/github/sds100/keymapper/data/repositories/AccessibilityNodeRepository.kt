package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

interface AccessibilityNodeRepository {
    val nodes: Flow<State<List<AccessibilityNodeEntity>>>
    fun insert(vararg node: AccessibilityNodeEntity)
    fun deleteAll()
}

class AccessibilityNodeRepositoryImpl(private val coroutineScope: CoroutineScope) : AccessibilityNodeRepository {
    // TODO have a DAO to remember between app launches and so it isn't all cached in memory?
    override val nodes =
        MutableStateFlow<State<List<AccessibilityNodeEntity>>>(State.Data(ArrayList(128)))

    override fun insert(vararg node: AccessibilityNodeEntity) {
        coroutineScope.launch {
            val currentState = nodes.value
            if (currentState is State.Data) {
                nodes.emit(State.Data(currentState.data.plus(node)))
            }
        }
    }

    override fun deleteAll() {
        coroutineScope.launch {
            nodes.emit(State.Data(ArrayList(128)))
        }
    }
}
