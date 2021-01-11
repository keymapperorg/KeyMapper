package io.github.sds100.keymapper.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.usecase.*
import io.github.sds100.keymapper.util.RequestBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 26/01/2020.
 */
class DefaultKeymapRepository internal constructor(private val keymapDao: KeyMapDao
) : GlobalKeymapUseCase, KeymapListUseCase, ConfigKeymapUseCase, BackupRestoreUseCase, MenuKeymapUseCase {

    override val requestBackup = MutableLiveData<RequestBackup>()
    override val keymapList: LiveData<List<KeyMap>> = keymapDao.observeAll()

    override suspend fun getKeymaps(): List<KeyMap> = keymapDao.getAll()

    override suspend fun getKeymap(id: Long) = keymapDao.getById(id)

    override suspend fun insertKeymap(vararg keymap: KeyMap) {
        keymapDao.insert(*keymap)

        requestBackup()
    }

    override suspend fun updateKeymap(keymap: KeyMap) {
        keymapDao.update(keymap)

        requestBackup()
    }

    override suspend fun duplicateKeymap(vararg id: Long) {
        val keymaps = mutableListOf<KeyMap>()

        id.forEach {
            keymaps.add(getKeymap(it).copy(id = 0))
        }

        insertKeymap(*keymaps.toTypedArray())
    }

    override suspend fun enableKeymapById(vararg id: Long) {
        keymapDao.enableKeymapById(*id)

        requestBackup()
    }

    override suspend fun disableKeymapById(vararg id: Long) {
        keymapDao.disableKeymapById(*id)

        requestBackup()
    }

    override suspend fun deleteKeymap(vararg id: Long) {
        keymapDao.deleteById(*id)

        requestBackup()
    }

    override suspend fun deleteAll() {
        keymapDao.deleteAll()

        requestBackup()
    }

    override suspend fun enableAll() {
        keymapDao.enableAll()

        requestBackup()
    }

    override suspend fun disableAll() {
        keymapDao.disableAll()

        requestBackup()
    }

    private suspend fun requestBackup() {
        withContext(Dispatchers.Default) {
            requestBackup.postValue(RequestBackup(getKeymaps()))
        }
    }
}