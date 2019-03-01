package io.github.sds100.keymapper.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.Extra

/**
 * Created by sds100 on 05/09/2018.
 */

class ExtraListTypeConverter {
    @TypeConverter
    fun toExtraObject(string: String) = Gson().fromJson<MutableList<Extra>>(string)

    @TypeConverter
    fun toString(extras: MutableList<Extra>) = Gson().toJson(extras)!!
}