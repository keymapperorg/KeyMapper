package io.github.sds100.keymapper.mappings.keymaps

import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.util.State
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 18/03/2021.
 */
interface KeyMapRepository {
    val keyMapList: Flow<State<List<KeyMapEntity>>>
    val requestBackup: Flow<List<KeyMapEntity>>

    fun insert(vararg keyMap: KeyMapEntity)
    fun update(vararg keyMap: KeyMapEntity)
    suspend fun get(uid: String): KeyMapEntity?
    fun delete(vararg uid: String)
    suspend fun deleteAll()

    fun duplicate(vararg uid: String)
    fun enableById(vararg uid: String)
    fun disableById(vararg uid: String)
}
