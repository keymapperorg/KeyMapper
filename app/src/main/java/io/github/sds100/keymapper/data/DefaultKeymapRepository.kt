package com.example.architecturetest.data

import androidx.lifecycle.LiveData
import io.github.sds100.keymapper.data.KeyMapDao
import io.github.sds100.keymapper.data.KeymapRepository
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 26/01/2020.
 */
class DefaultKeymapRepository private constructor(private val keymapDao: KeyMapDao) : KeymapRepository {

    companion object {
        @Volatile
        private var instance: DefaultKeymapRepository? = null

        fun getInstance(keymapDao: KeyMapDao) =
            instance ?: synchronized(this) {
                instance ?: DefaultKeymapRepository(keymapDao).also { instance = it }
            }
    }

    override val keymapList = keymapDao.getAll()

    override suspend fun getKeymap(id: Long) = keymapDao.getById(id)

    override suspend fun createKeymap(keymap: KeyMap) {
        keymapDao.insert(keymap)
    }

    override suspend fun updateKeymap(keymap: KeyMap) {
        keymapDao.update(keymap)
    }

    override suspend fun enableKeymapById(vararg id: Long) {
        keymapDao.enableKeymapById(*id)
    }

    override suspend fun disableKeymapById(vararg id: Long) {
        keymapDao.disableKeymapById(*id)
    }

    override suspend fun deleteKeymap(vararg id: Long) {
        keymapDao.deleteById(*id)
    }

    override suspend fun enableAll() {
        keymapDao.enableAll()
    }

    override suspend fun disableAll() {
        keymapDao.disableAll()
    }
}