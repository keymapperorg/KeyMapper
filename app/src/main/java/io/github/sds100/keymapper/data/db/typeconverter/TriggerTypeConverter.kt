package io.github.sds100.keymapper.data.db.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.model.Trigger

/**
 * Created by sds100 on 05/09/2018.
 */

class TriggerTypeConverter {
    @TypeConverter
    fun toTrigger(json: String) = Gson().fromJson<Trigger>(json)

    @TypeConverter
    fun toJsonString(trigger: Trigger) = Gson().toJson(trigger)!!
}