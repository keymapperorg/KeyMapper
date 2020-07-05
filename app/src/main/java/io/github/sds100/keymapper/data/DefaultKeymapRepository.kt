package io.github.sds100.keymapper.data

import androidx.lifecycle.LiveData
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.model.KeyMap

/**
 * Created by sds100 on 26/01/2020.
 */
class DefaultKeymapRepository internal constructor(private val mKeymapDao: KeyMapDao) : KeymapRepository {

    override val keymapList: LiveData<List<KeyMap>> = mKeymapDao.observeAll()

    override suspend fun getKeymaps(): List<KeyMap> = mKeymapDao.getAll()

    override suspend fun getKeymap(id: Long) = mKeymapDao.getById(id)

    override suspend fun insertKeymap(vararg keymap: KeyMap) {
        mKeymapDao.insert(*keymap)
    }

    override suspend fun updateKeymap(keymap: KeyMap) {
        mKeymapDao.update(keymap)
    }

    override suspend fun duplicateKeymap(vararg id: Long) {
        id.forEach { keymapId ->
            insertKeymap(getKeymap(keymapId).clone())
        }
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

    override suspend fun deleteAll() {
        mKeymapDao.deleteAll()
    }

    override suspend fun enableAll() {
        mKeymapDao.enableAll()
    }

    override suspend fun disableAll() {
        mKeymapDao.disableAll()
    }
}