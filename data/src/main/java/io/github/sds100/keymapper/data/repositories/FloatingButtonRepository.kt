package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.data.entities.FloatingButtonEntity
import io.github.sds100.keymapper.data.entities.FloatingButtonEntityWithLayout
import kotlinx.coroutines.flow.Flow

interface FloatingButtonRepository {
    val buttonsList: Flow<State<List<FloatingButtonEntityWithLayout>>>

    fun insert(vararg button: FloatingButtonEntity)

    fun update(button: FloatingButtonEntity)

    suspend fun get(uid: String): FloatingButtonEntityWithLayout?

    fun delete(vararg uid: String)
}
