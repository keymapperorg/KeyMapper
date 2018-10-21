package io.github.sds100.keymapper.Data

import android.content.Context
import android.view.KeyEvent
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.DatabaseAsyncTasks.DeleteKeyMapAsync
import io.github.sds100.keymapper.DatabaseAsyncTasks.DeleteKeyMapByIdAsync
import io.github.sds100.keymapper.DatabaseAsyncTasks.InsertKeyMapAsync
import io.github.sds100.keymapper.DatabaseAsyncTasks.UpdateKeyMapAsync
import kotlin.coroutines.experimental.buildSequence

/**
 * Created by sds100 on 08/08/2018.
 */

/**
 * Controls how key maps are saved and retrieved
 */
class KeyMapRepository private constructor(ctx: Context) {
    companion object {
        private const val DEBUG_LIST_COUNT = 100

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

        if (BuildConfig.DEBUG) {
            //addDebugItems()
        }
    }

    fun getKeyMap(id: Long): KeyMap {
        return keyMapList.value!!.find { it.id == id }!!
    }

    fun deleteKeyMap(vararg keyMap: KeyMap) {
        DeleteKeyMapAsync(mDb).execute(*keyMap)
    }

    fun deleteKeyMapById(vararg id: Long) {
        DeleteKeyMapByIdAsync(mDb).execute(*id.toList().toTypedArray())
    }

    fun insertKeyMap(vararg keyMap: KeyMap) {
        InsertKeyMapAsync(mDb).execute(*keyMap)
    }

    fun updateKeyMap(vararg keyMap: KeyMap) {
        UpdateKeyMapAsync(mDb).execute(*keyMap)
    }

    private fun addDebugItems() {
        val observer = Observer<List<KeyMap>> { list ->
            val minimumId: Long = if (list.maxBy { it.id } == null) {
                0
            } else {
                list.maxBy { it.id }!!.id
            }

            val size = list!!.size

            //only create more debug items if the list is smaller than 100
            if (size < DEBUG_LIST_COUNT) {
                val sizeDifference = DEBUG_LIST_COUNT - size

                val testKeyMapList = buildSequence {
                    for (i in 1..sizeDifference) {
                        //ensure the id doesn't already exist
                        val id = minimumId + i

                        val triggerList = mutableListOf(
                                Trigger(listOf(KeyEvent.KEYCODE_VOLUME_UP))
                        )

                        yield(KeyMap(id,
                                triggerList,
                                Action(ActionType.APP, "io.github.sds100.keymapper")))
                    }
                }.toList()

                insertKeyMap(*testKeyMapList.toTypedArray())
            }
        }

        keyMapList.observeForever(observer)
    }
}