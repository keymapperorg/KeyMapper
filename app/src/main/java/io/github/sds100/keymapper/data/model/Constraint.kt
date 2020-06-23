package io.github.sds100.keymapper.data.model

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.result.ExtraNotFound
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import java.io.Serializable

/**
 * Created by sds100 on 17/03/2020.
 */
@StringDef(value = [
    Constraint.APP_FOREGROUND,
    Constraint.APP_NOT_FOREGROUND,
    Constraint.BT_DEVICE_CONNECTED,
    Constraint.BT_DEVICE_DISCONNECTED,
    Constraint.SCREEN_ON,
    Constraint.SCREEN_OFF
])
annotation class ConstraintType

@IntDef(value = [
    Constraint.MODE_AND,
    Constraint.MODE_OR
])
annotation class ConstraintMode

@IntDef(value = [
    Constraint.CATEGORY_APP,
    Constraint.CATEGORY_BLUETOOTH,
    Constraint.CATEGORY_SCREEN]
)
annotation class ConstraintCategory

data class Constraint(@ConstraintType
                      @SerializedName(NAME_TYPE)
                      val type: String,

                      @SerializedName(NAME_EXTRAS)
                      val extras: List<Extra>) : Serializable {

    constructor(type: String, vararg extra: Extra) : this(type, extra.toList())

    companion object {
        //DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_TYPE = "type"
        const val NAME_EXTRAS = "extras"

        const val MODE_OR = 0
        const val MODE_AND = 1
        const val DEFAULT_MODE = MODE_AND

        const val APP_FOREGROUND = "constraint_app_foreground"
        const val APP_NOT_FOREGROUND = "constraint_app_not_foreground"
        const val BT_DEVICE_CONNECTED = "constraint_bt_device_connected"
        const val BT_DEVICE_DISCONNECTED = "constraint_bt_device_disconnected"
        const val SCREEN_ON = "constraint_screen_on"
        const val SCREEN_OFF = "constraint_screen_off"

        //Categories
        const val CATEGORY_APP = 0
        const val CATEGORY_BLUETOOTH = 1
        const val CATEGORY_SCREEN = 2

        val CATEGORY_LABEL_MAP = mapOf(
            CATEGORY_APP to R.string.constraint_category_app,
            CATEGORY_BLUETOOTH to R.string.constraint_category_bluetooth,
            CATEGORY_SCREEN to R.string.constraint_category_screen
        )

        fun appConstraint(@ConstraintType type: String, packageName: String): Constraint {
            return Constraint(type, Extra(Extra.EXTRA_PACKAGE_NAME, packageName))
        }

        fun btConstraint(@ConstraintType type: String, address: String, name: String): Constraint {
            return Constraint(type, Extra(Extra.EXTRA_BT_ADDRESS, address), Extra(Extra.EXTRA_BT_NAME, name))
        }

        fun screenOnConstraint(): Constraint {
            return Constraint(SCREEN_ON)
        }

        fun screenOffConstraint(): Constraint {
            return Constraint(SCREEN_OFF)
        }
    }

    fun getExtraData(extraId: String): Result<String> {
        val extra = extras.find { it.id == extraId } ?: return ExtraNotFound(extraId)

        return Success(extra.data)
    }

    /**
     * A unique identifier describing this constraint
     */
    val uniqueId: String
        get() = buildString {
            append(type)
            extras.forEach {
                append("${it.id}${it.data}")
            }
        }
}

