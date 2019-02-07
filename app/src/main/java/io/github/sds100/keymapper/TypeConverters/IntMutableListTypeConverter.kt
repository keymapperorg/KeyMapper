package io.github.sds100.keymapper.TypeConverters

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson

/**
 * Created by sds100 on 26/01/2019.
 */

class IntMutableListTypeConverter {
    @TypeConverter
    fun toActionTypeEnum(string: String) = Gson().fromJson<MutableList<Int>>(string)

    @TypeConverter
    fun toString(intList: MutableList<Int>) = Gson().toJson(intList)
}