package io.github.sds100.keymapper.data.model

import androidx.annotation.StringDef
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_HOLD_DOWN_DELAY
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_LENS
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_PACKAGE_NAME
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_REPEAT_DELAY
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_RINGER_MODE
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_SHORTCUT_TITLE
import io.github.sds100.keymapper.data.model.Action.Companion.EXTRA_STREAM_TYPE
import io.github.sds100.keymapper.data.model.Constraint.Companion.EXTRA_BT_ADDRESS
import io.github.sds100.keymapper.data.model.Constraint.Companion.EXTRA_BT_NAME
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_DOUBLE_PRESS_DELAY
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_LONG_PRESS_DELAY
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_SEQUENCE_TRIGGER_TIMEOUT
import io.github.sds100.keymapper.data.model.Trigger.Companion.EXTRA_VIBRATION_DURATION
import io.github.sds100.keymapper.util.result.ExtraNotFound
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import java.io.Serializable

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
    EXTRA_HOLD_DOWN_DELAY,
    EXTRA_REPEAT_DELAY,
    EXTRA_VIBRATION_DURATION,
    EXTRA_BT_ADDRESS,
    EXTRA_BT_NAME
])
annotation class ExtraId

data class Extra(@ExtraId
                 @SerializedName(NAME_ID)
                 val id: String,

                 @SerializedName(NAME_DATA)
                 val data: String) : Serializable {
    companion object {

        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_ID = "id"
        const val NAME_DATA = "data"
    }
}

fun List<Extra>.putExtraData(id: String, data: String): List<Extra> {
    return this.toMutableList().apply {
        removeAll { it.id == id }
        add(Extra(id, data))
    }
}

fun List<Extra>.getData(extraId: String): Result<String> {

    return find { it.id == extraId }.let {
        it ?: return@let ExtraNotFound(extraId)

        Success(it.data)
    }
}
