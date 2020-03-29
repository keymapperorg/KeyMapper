package com.example.architecturetest.data

import io.github.sds100.keymapper.data.KeyMapDao
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 26/01/2020.
 */
class KeymapRepository private constructor(private val keymapDao: KeyMapDao) {

    companion object {
        @Volatile
        private var instance: KeymapRepository? = null

        fun getInstance(keymapDao: KeyMapDao) =
            instance ?: synchronized(this) {
                instance ?: KeymapRepository(keymapDao).also { instance = it }
            }
    }

    val keymapList = keymapDao.getAll()

    suspend fun getKeymap(id: Long) = keymapDao.getById(id)

    suspend fun createKeymap(keymap: KeyMap) {
        keymapDao.insert(keymap)
    }

    suspend fun updateKeymap(keymap: KeyMap) {
        keymapDao.update(keymap)
    }

    suspend fun enableKeymapById(vararg id: Long) {
        keymapDao.enableKeymapById(*id)
    }

    suspend fun disableKeymapById(vararg id: Long) {
        keymapDao.disableKeymapById(*id)
    }

    suspend fun deleteKeymap(vararg id: Long) {
        keymapDao.deleteById(*id)
    }
}