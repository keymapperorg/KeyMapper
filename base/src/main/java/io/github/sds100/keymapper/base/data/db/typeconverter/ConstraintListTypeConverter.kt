package io.github.sds100.keymapper.base.data.db.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.entities.ConstraintEntity

class ConstraintListTypeConverter {
    private val gson = GsonBuilder().registerTypeAdapter(ConstraintEntity.DESERIALIZER).create()

    @TypeConverter
    fun toConstraintList(json: String) = gson.fromJson<List<ConstraintEntity>>(json)

    @TypeConverter
    fun toJsonString(constraintList: List<ConstraintEntity>) = gson.toJson(constraintList)!!
}
