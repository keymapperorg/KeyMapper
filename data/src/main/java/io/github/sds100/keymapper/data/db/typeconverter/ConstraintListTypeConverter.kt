package io.github.sds100.keymapper.data.db.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.entities.ConstraintEntity
import io.github.sds100.keymapper.data.entities.ConstraintGroupEntity

class ConstraintListTypeConverter {
    private val gson = GsonBuilder()
        .registerTypeAdapter(ConstraintEntity.DESERIALIZER)
        .registerTypeAdapter(ConstraintGroupEntity.DESERIALIZER)
        .create()

    @TypeConverter
    fun toConstraintList(json: String) = gson.fromJson<List<ConstraintEntity>>(json)

    @TypeConverter
    fun toJsonString(constraintList: List<ConstraintEntity>) = gson.toJson(constraintList)!!

    @TypeConverter
    fun toConstraintGroupList(json: String) = gson.fromJson<List<ConstraintGroupEntity>>(json)

    @TypeConverter
    fun constraintGroupListToJsonString(constraintGroups: List<ConstraintGroupEntity>) =
        gson.toJson(constraintGroups)!!
}
