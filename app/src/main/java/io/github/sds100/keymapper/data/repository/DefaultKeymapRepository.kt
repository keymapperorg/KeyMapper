package io.github.sds100.keymapper.data.repository

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.data.db.dao.KeyMapDao
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.usecase.BackupRestoreUseCase
import io.github.sds100.keymapper.data.usecase.ConfigKeymapUseCase
import io.github.sds100.keymapper.data.usecase.GlobalKeymapUseCase
import io.github.sds100.keymapper.data.usecase.KeymapListUseCase
import io.github.sds100.keymapper.util.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 26/01/2020.
 */
class DefaultKeymapRepository internal constructor(private val mKeymapDao: KeyMapDao
) : GlobalKeymapUseCase, KeymapListUseCase, ConfigKeymapUseCase, BackupRestoreUseCase {

    override val requestBackup = MutableLiveData<Event<List<KeyMap>>>()
    override val keymapList: LiveData<List<KeyMap>> = mKeymapDao.observeAll()

    override suspend fun getKeymaps(): List<KeyMap> = mKeymapDao.getAll()

    override suspend fun getKeymap(id: Long) = mKeymapDao.getById(id)

    override suspend fun insertKeymap(vararg keymap: KeyMap) {
        mKeymapDao.insert(*keymap)

        requestBackup()
    }

    override suspend fun updateKeymap(keymap: KeyMap) {
        mKeymapDao.update(keymap)

        requestBackup()
    }

    override suspend fun duplicateKeymap(vararg id: Long) {
        val keymaps = mutableListOf<KeyMap>()

        id.forEach {
            keymaps.add(getKeymap(it).clone())
        }

        insertKeymap(*keymaps.toTypedArray())
    }

    override suspend fun enableKeymapById(vararg id: Long) {
        mKeymapDao.enableKeymapById(*id)

        requestBackup()
    }

    override suspend fun disableKeymapById(vararg id: Long) {
        mKeymapDao.disableKeymapById(*id)

        requestBackup()
    }

    override suspend fun deleteKeymap(vararg id: Long) {
        mKeymapDao.deleteById(*id)

        requestBackup()
    }

    override suspend fun deleteAll() {
        mKeymapDao.deleteAll()

        requestBackup()
    }

    override suspend fun enableAll() {
        mKeymapDao.enableAll()

        requestBackup()
    }

    override suspend fun disableAll() {
        mKeymapDao.disableAll()

        requestBackup()
    }

    private suspend fun requestBackup() {
        withContext(Dispatchers.Default) {
            requestBackup.postValue(Event(getKeymaps()))
        }
    }
}