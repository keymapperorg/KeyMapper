package io.github.sds100.keymapper.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.Trigger

/**
 * Created by sds100 on 05/09/2018.
 */

class TriggerListTypeConverter {
    @TypeConverter
    fun toTriggerList(json: String) = Gson().fromJson<MutableList<Trigger>>(json)

    @TypeConverter
    fun toJsonString(triggerList: MutableList<Trigger>) = Gson().toJson(triggerList)!!
}