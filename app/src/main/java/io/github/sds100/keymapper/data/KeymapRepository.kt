package com.example.architecturetest.data

import com.example.architecturetest.data.db.dao.KeyMapDao
import com.example.architecturetest.data.model.KeyMap
import io.github.sds100.keymapper.data.KeyMapDao
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 26/01/2020.
 */
class KeymapRepository private constructor(private val keymapDao: KeyMapDao) {

    companion object{
        @Volatile private var instance: KeymapRepository? = null

        fun getInstance(keymapDao: KeyMapDao) =
                instance ?: synchronized(this) {
                    instance ?: KeymapRepository(keymapDao).also { instance = it}
                }
    }

    val keymapList = keymapDao.getAll()

    suspend fun getKeymap(id: Long) = keymapDao.getById(id)

    suspend fun createKeymap(keymap: KeyMap){
        keymapDao.insert(keymap)
    }

    suspend fun updateKeymap(keymap: KeyMap){
        keymapDao.update(keymap)
    }

    suspend fun deleteKeymap(vararg id: Long){
        keymapDao.deleteById(*id)
    }
}