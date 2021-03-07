package io.github.sds100.keymapper.data.db.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.model.Action

/**
 * Created by sds100 on 05/09/2018.
 */

class ActionListTypeConverter {
    @TypeConverter
    fun toActionList(json: String): List<Action> {
        val gson = GsonBuilder().registerTypeAdapter(Action.DESERIALIZER).create()
        return gson.fromJson<MutableList<Action>>(json)
    }

    @TypeConverter
    fun toJsonString(actionList: MutableList<Action>): String {
        return Gson().toJson(actionList)!!
    }
}