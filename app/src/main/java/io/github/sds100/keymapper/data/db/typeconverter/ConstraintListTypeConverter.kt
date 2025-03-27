package io.github.sds100.keymapper.data.db.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.google.gson.Gson
import io.github.sds100.keymapper.data.entities.ConstraintEntity

/**
 * Created by sds100 on 05/09/2018.
 */

class ConstraintListTypeConverter {
    @TypeConverter
    fun toConstraintList(json: String) = Gson().fromJson<List<ConstraintEntity>>(json)

    @TypeConverter
    fun toJsonString(constraintList: List<ConstraintEntity>) = Gson().toJson(constraintList)!!
}
