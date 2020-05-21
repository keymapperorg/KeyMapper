package io.github.sds100.keymapper.data

import androidx.lifecycle.LiveData
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 17/05/2020.
 */
interface KeymapRepository {

    val keymapList: LiveData<List<KeyMap>>

    suspend fun getKeymaps(): List<KeyMap>

    suspend fun getKeymap(id: Long): KeyMap

    suspend fun createKeymap(keymap: KeyMap)
    suspend fun updateKeymap(keymap: KeyMap)

    suspend fun enableKeymapById(vararg id: Long)
    suspend fun disableKeymapById(vararg id: Long)

    suspend fun deleteKeymap(vararg id: Long)

    suspend fun enableAll()
    suspend fun disableAll()
}