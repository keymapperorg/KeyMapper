package io.github.sds100.keymapper

import android.content.Context
import android.preference.PreferenceManager
import androidx.core.content.edit
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import java.lang.ref.WeakReference

/**
 * Created by sds100 on 08/08/2018.
 */

/**
 * Controls how key maps are saved and retrieved
 */
class KeyMapRepository private constructor(ctx: Context) {
    companion object {
        const val KEY_KEY_MAP_LIST = "key_map_list"

        private var INSTANCE: KeyMapRepository? = null

        fun getInstance(ctx: Context): KeyMapRepository {
            if (INSTANCE == null) {
                INSTANCE = KeyMapRepository(ctx)
            }

            return INSTANCE!!
        }
    }

    private var mKeyMapList: MutableList<KeyMap>
    private val mCtx: WeakReference<Context> = WeakReference(ctx.applicationContext)

    private val mSharedPreferences
        get() = PreferenceManager.getDefaultSharedPreferences(mCtx.get())

    init {
        //must be application context
        mKeyMapList = getKeyMapListFromSharedPrefs()
    }

    fun getKeyMapList(): List<KeyMap> = mKeyMapList

    fun removeKeyMap(id: Long) {
        removeKeyMap(mKeyMapList.single { it.id == id })
    }

    fun removeKeyMap(keyMap: KeyMap) {
        mKeyMapList.remove(keyMap)
        saveKeyMapListToSharedPrefs()
    }

    fun saveKeyMap(keyMap: KeyMap) {
        mKeyMapList.add(keyMap)
        saveKeyMapListToSharedPrefs()
    }

    fun createUniqueId(): Long {
        if (mKeyMapList.isEmpty()) return 0L

        return mKeyMapList.maxBy { it.id }!!.id + 1
    }

    private fun saveKeyMapListToSharedPrefs() {
        /*I would use the anko library to get the default shared preferences from context
         but it stopped working */
        mSharedPreferences.edit {
            val json = Gson().toJson(mKeyMapList)
            putString(KEY_KEY_MAP_LIST, json)
        }
    }

    private fun getKeyMapListFromSharedPrefs(): MutableList<KeyMap> {
        val json = mSharedPreferences.getString(KEY_KEY_MAP_LIST, null)

        if (json == null) {
            return mutableListOf()
        }

        return Gson().fromJson(json)
    }
}