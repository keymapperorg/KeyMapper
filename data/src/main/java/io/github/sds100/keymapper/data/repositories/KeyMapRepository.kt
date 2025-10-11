package io.github.sds100.keymapper.data.repositories

import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import kotlinx.coroutines.flow.Flow

interface KeyMapRepository {
    val keyMapList: Flow<State<List<KeyMapEntity>>>

    fun getAll(): Flow<List<KeyMapEntity>>
    fun getByGroup(groupUid: String?): Flow<List<KeyMapEntity>>
    fun insert(vararg keyMap: KeyMapEntity)
    fun update(vararg keyMap: KeyMapEntity)
    suspend fun get(uid: String): KeyMapEntity?
    fun delete(vararg uid: String)
    suspend fun deleteAll()
    fun count(): Flow<Int>

    fun duplicate(vararg uid: String)
    fun enableById(vararg uid: String)
    fun disableById(vararg uid: String)
    fun enableByGroup(groupUid: String?)
    fun disableByGroup(groupUid: String?)
    fun moveToGroup(groupUid: String?, vararg uid: String)
}
