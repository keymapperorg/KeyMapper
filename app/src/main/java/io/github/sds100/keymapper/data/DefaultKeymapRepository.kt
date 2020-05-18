package com.example.architecturetest.data

import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.KeymapRepository
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 26/01/2020.
 */
class DefaultKeymapRepository private constructor(private val mKeymapDao: KeyMapDao) : KeymapRepository {

    companion object {
        @Volatile
        private var instance: DefaultKeymapRepository? = null

        fun getInstance(keymapDao: KeyMapDao) =
            instance ?: synchronized(this) {
                instance ?: DefaultKeymapRepository(keymapDao).also { instance = it }
            }
    }

    override val keymapList = mKeymapDao.getAll()

    override suspend fun getKeymap(id: Long) = mKeymapDao.getById(id)

    override suspend fun createKeymap(keymap: KeyMap) {
        mKeymapDao.insert(keymap)
    }

    override suspend fun updateKeymap(keymap: KeyMap) {
        mKeymapDao.update(keymap)
    }

    override suspend fun enableKeymapById(vararg id: Long) {
        mKeymapDao.enableKeymapById(*id)
    }

    override suspend fun disableKeymapById(vararg id: Long) {
        mKeymapDao.disableKeymapById(*id)
    }

    override suspend fun deleteKeymap(vararg id: Long) {
        mKeymapDao.deleteById(*id)
    }

    override suspend fun enableAll() {
        mKeymapDao.enableAll()
    }

    override suspend fun disableAll() {
        mKeymapDao.disableAll()
    }
}