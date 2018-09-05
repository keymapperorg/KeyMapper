package io.github.sds100.keymapper.Data

import android.content.Context
import androidx.lifecycle.LiveData
import io.github.sds100.keymapper.DatabaseAsyncTasks.DeleteKeyMapAsync
import io.github.sds100.keymapper.DatabaseAsyncTasks.InsertKeyMapAsync
import io.github.sds100.keymapper.DatabaseAsyncTasks.UpdateKeyMapAsync
import io.github.sds100.keymapper.KeyMap

/**
 * Created by sds100 on 08/08/2018.
 */

/**
 * Controls how key maps are saved and retrieved
 */
class KeyMapRepository private constructor(ctx: Context) {
    companion object {
        private var INSTANCE: KeyMapRepository? = null

        fun getInstance(ctx: Context): KeyMapRepository {
            if (INSTANCE == null) {
                INSTANCE = KeyMapRepository(ctx)
            }

            return INSTANCE!!
        }
    }

    val keyMapList: LiveData<List<KeyMap>>

    private val mDb: AppDatabase = AppDatabase.getInstance(ctx)

    init {
        keyMapList = mDb.keyMapDao().getAllKeyMaps()
    }

    fun deleteKeyMap(vararg keyMap: KeyMap) {
        DeleteKeyMapAsync(mDb).execute(*keyMap)
    }

    fun addKeyMap(vararg keyMap: KeyMap) {
        InsertKeyMapAsync(mDb).execute(*keyMap)
    }

    fun updateKeyMap(vararg keyMap: KeyMap) {
        UpdateKeyMapAsync(mDb).execute(*keyMap)
    }
}