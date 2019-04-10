package io.github.sds100.keymapper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.sds100.keymapper.KeyMap
import io.github.sds100.keymapper.data.AppDatabase
import org.jetbrains.anko.doAsync

/**
 * Created by sds100 on 29/03/2019.
 */

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val mDb: AppDatabase = AppDatabase.getInstance(application)

    val keyMapList = mDb.keyMapDao().getAll()

    fun deleteKeyMap(vararg keyMap: KeyMap) = doAsync { mDb.keyMapDao().delete(*keyMap) }

    fun deleteKeyMapById(vararg id: Long) = doAsync { mDb.keyMapDao().deleteById(*id.toList().toLongArray()) }

    fun disableAllKeymaps() = doAsync { mDb.keyMapDao().disableAll() }

    fun enableAllKeymaps() = doAsync { mDb.keyMapDao().enableAll() }

    fun enableKeymapById(vararg id: Long) = doAsync { mDb.keyMapDao().enableKeymapById(*id) }

    fun disableKeymapById(vararg id: Long) = doAsync { mDb.keyMapDao().disableKeymapById(*id) }
}