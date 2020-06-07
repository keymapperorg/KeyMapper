package io.github.sds100.keymapper.data.db.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.model.Action

/**
 * Created by sds100 on 05/09/2018.
 */

class ActionListTypeConverter {
    @TypeConverter
    fun toActionList(json: String) = Gson().fromJson<MutableList<Action>>(json)

    @TypeConverter
    fun toJsonString(actionList: MutableList<Action>) = Gson().toJson(actionList)!!
}