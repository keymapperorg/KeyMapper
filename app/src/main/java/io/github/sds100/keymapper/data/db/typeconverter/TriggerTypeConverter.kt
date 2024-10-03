package io.github.sds100.keymapper.data.db.typeconverter

import androidx.room.TypeConverter
import com.github.salomonbrys.kotson.fromJson
import com.github.salomonbrys.kotson.registerTypeAdapter
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import io.github.sds100.keymapper.data.entities.Extra
import io.github.sds100.keymapper.data.entities.TriggerEntity
import io.github.sds100.keymapper.data.entities.TriggerKeyEntity

/**
 * Created by sds100 on 05/09/2018.
 */

class TriggerTypeConverter {
    @TypeConverter
    fun toTrigger(json: String): TriggerEntity {
        val gson = GsonBuilder()
            .registerTypeAdapter(TriggerEntity.DESERIALIZER)
            .registerTypeAdapter(TriggerKeyEntity.DESERIALIZER)
            .registerTypeAdapter(Extra.DESERIALIZER).create()

        return gson.fromJson(json)
    }

    @TypeConverter
    fun toJsonString(trigger: TriggerEntity) = Gson().toJson(trigger)!!
}
