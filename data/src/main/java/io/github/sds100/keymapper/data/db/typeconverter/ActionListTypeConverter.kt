package io.github.sds100.keymapper.data.db.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.ConstraintEntity

class ActionListTypeConverter {
    private val gson = GsonBuilder().registerTypeAdapter(ConstraintEntity.DESERIALIZER).create()

    @TypeConverter
    fun toActionList(json: String): List<ActionEntity> = gson.fromJson<MutableList<ActionEntity>>(json)

    @TypeConverter
    fun toJsonString(actionList: List<ActionEntity>): String = gson.toJson(actionList)!!
}
