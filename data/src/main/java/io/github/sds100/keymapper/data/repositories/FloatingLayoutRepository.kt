package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntity
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntityWithButtons
import kotlinx.coroutines.flow.Flow

interface FloatingLayoutRepository {
    val layouts: Flow<State<List<FloatingLayoutEntityWithButtons>>>
    suspend fun insert(vararg layout: FloatingLayoutEntity)

    /**
     * @return whether the update happened successfully. It can be false if some constraints
     * failed.
     */
    suspend fun update(vararg layout: FloatingLayoutEntity): Boolean
    fun get(uid: String): Flow<FloatingLayoutEntityWithButtons?>
    fun delete(vararg uid: String)
    suspend fun count(): Int
}
