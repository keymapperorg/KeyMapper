package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.data.entities.AccessibilityNodeEntity
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow

interface AccessibilityNodeRepository {
    val nodes: Flow<State<List<AccessibilityNodeEntity>>>
    suspend fun get(id: Long): AccessibilityNodeEntity?
    fun insert(vararg node: AccessibilityNodeEntity)
    suspend fun deleteAll()
}

