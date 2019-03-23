package io.github.sds100.keymapper.data

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import io.github.sds100.keymapper.*
import io.github.sds100.keymapper.util.KeycodeUtils
import org.jetbrains.anko.doAsync

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
        keyMapList = mDb.keyMapDao().getAll()

        if (BuildConfig.DEBUG) {
            addDebugItems()
        }
    }

    /**
     * Get a copy of a keymap so any changes made to it won't appear in the list.
     */
    fun getKeyMapCopy(id: Long): KeyMap {
        /*must be copied otherwise any changes made to it (even without updating it in the database)
        will appear in the list */
        return keyMapList.value!!.find { it.id == id }!!.clone()
    }

    fun deleteKeyMap(vararg keyMap: KeyMap) = doAsync { mDb.keyMapDao().delete(*keyMap) }

    fun deleteKeyMapById(vararg id: Long) = doAsync { mDb.keyMapDao().deleteById(*id.toList().toLongArray()) }

    fun insertKeyMap(vararg keyMap: KeyMap) = doAsync { mDb.keyMapDao().insert(*keyMap) }

    fun updateKeyMap(vararg keyMap: KeyMap) = doAsync { mDb.keyMapDao().update(*keyMap) }

    fun disableAllKeymaps() = doAsync { mDb.keyMapDao().disableAll() }

    fun enableAllKeymaps() = doAsync { mDb.keyMapDao().enableAll() }

    fun enableKeymapById(vararg id: Long) = doAsync { mDb.keyMapDao().enableKeymapById(*id) }
    fun disableKeymapById(vararg id: Long) = doAsync { mDb.keyMapDao().disableKeymapById(*id) }

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

                val testKeyMapList = sequence {
                    for (i in 1..sizeDifference) {
                        //ensure the id doesn't already exist
                        val id = minimumId + i

                        val triggerList = mutableListOf(
                                Trigger(listOf(KeycodeUtils.getKeyCodes().random()))
                        )

                        val keyMap = KeyMap(id, triggerList).apply {
                            action = Action(ActionType.APP, Constants.PACKAGE_NAME)
                        }

                        yield(keyMap)
                    }
                }.toList()

                insertKeyMap(*testKeyMapList.toTypedArray())
            }
        }

        keyMapList.observeForever(observer)
    }
}