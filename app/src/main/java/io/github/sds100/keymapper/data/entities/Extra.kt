package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import androidx.annotation.StringDef
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.entities.ActionEntity.Companion.EXTRA_LENS
import io.github.sds100.keymapper.data.entities.ActionEntity.Companion.EXTRA_PACKAGE_NAME
import io.github.sds100.keymapper.data.entities.ActionEntity.Companion.EXTRA_REPEAT_DELAY
import io.github.sds100.keymapper.data.entities.ActionEntity.Companion.EXTRA_REPEAT_RATE
import io.github.sds100.keymapper.data.entities.ActionEntity.Companion.EXTRA_RINGER_MODE
import io.github.sds100.keymapper.data.entities.ActionEntity.Companion.EXTRA_SHORTCUT_TITLE
import io.github.sds100.keymapper.data.entities.ActionEntity.Companion.EXTRA_STREAM_TYPE
import io.github.sds100.keymapper.data.entities.ConstraintEntity.Companion.EXTRA_BT_ADDRESS
import io.github.sds100.keymapper.data.entities.ConstraintEntity.Companion.EXTRA_BT_NAME
import io.github.sds100.keymapper.data.entities.TriggerEntity.Companion.EXTRA_DOUBLE_PRESS_DELAY
import io.github.sds100.keymapper.data.entities.TriggerEntity.Companion.EXTRA_LONG_PRESS_DELAY
import io.github.sds100.keymapper.data.entities.TriggerEntity.Companion.EXTRA_SEQUENCE_TRIGGER_TIMEOUT
import io.github.sds100.keymapper.data.entities.TriggerEntity.Companion.EXTRA_VIBRATION_DURATION
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import kotlinx.android.parcel.Parcelize

/**
 * Created by sds100 on 26/01/2019.
 */

@StringDef(value = [
    EXTRA_PACKAGE_NAME,
    EXTRA_SHORTCUT_TITLE,
    EXTRA_STREAM_TYPE,
    EXTRA_LENS,
    EXTRA_RINGER_MODE,
    EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
    EXTRA_LONG_PRESS_DELAY,
    EXTRA_DOUBLE_PRESS_DELAY,
    EXTRA_REPEAT_DELAY,
    EXTRA_REPEAT_RATE,
    EXTRA_VIBRATION_DURATION,
    EXTRA_BT_ADDRESS,
    EXTRA_BT_NAME
])
annotation class ExtraId

@Parcelize
data class Extra(@ExtraId
                 @SerializedName(NAME_ID)
                 val id: String,

                 @SerializedName(NAME_DATA)
                 val data: String) : Parcelable {
    companion object {

        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_ID = "id"
        const val NAME_DATA = "data"

        val DESERIALIZER = jsonDeserializer {
            val id by it.json.byString(NAME_ID)
            val data by it.json.byString(NAME_DATA)

            Extra(id, data)
        }
    }
}

fun List<Extra>.getData(extraId: String): Result<String> {

    return find { it.id == extraId }.let {
        it ?: return@let Error.ExtraNotFound(extraId)

        Success(it.data)
    }
}
