package io.github.sds100.keymapper.data.db.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.entities.EntityExtra

/**
 * Created by sds100 on 05/09/2018.
 */

class ExtraListTypeConverter {
    private val gson = GsonBuilder().create()

    @TypeConverter
    fun toExtraObject(string: String) = gson.fromJson<List<EntityExtra>>(string)

    @TypeConverter
    fun toString(extras: List<EntityExtra>) = gson.toJson(extras)!!
}
