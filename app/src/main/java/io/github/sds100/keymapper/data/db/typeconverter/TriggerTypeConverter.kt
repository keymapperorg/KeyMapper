package io.github.sds100.keymapper.data.db.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.TriggerEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity



class TriggerTypeConverter {
    private val gson = GsonBuilder()
        .registerTypeAdapter(TriggerEntity.DESERIALIZER)
        .registerTypeAdapter(TriggerKeyEntity.SERIALIZER)
        .registerTypeAdapter(TriggerKeyEntity.DESERIALIZER)
        .registerTypeAdapter(EntityExtra.DESERIALIZER).create()

    @TypeConverter
    fun toTrigger(json: String): TriggerEntity {
        return gson.fromJson(json)
    }

    @TypeConverter
    fun toJsonString(trigger: TriggerEntity) = gson.toJson(trigger)!!
}
