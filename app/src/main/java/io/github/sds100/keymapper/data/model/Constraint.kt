package io.github.sds100.keymapper.data.model

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import com.github.salomonbrys.kotson.byArray
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
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
    Constraint.SCREEN_OFF,
    Constraint.ORIENTATION_PORTRAIT,
    Constraint.ORIENTATION_LANDSCAPE,
    Constraint.ORIENTATION_0,
    Constraint.ORIENTATION_90,
    Constraint.ORIENTATION_180,
    Constraint.ORIENTATION_270
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
        const val ORIENTATION_0 = "constraint_orientation_0"
        const val ORIENTATION_90 = "constraint_orientation_90"
        const val ORIENTATION_180 = "constraint_orientation_180"
        const val ORIENTATION_270 = "constraint_orientation_270"
        const val ORIENTATION_PORTRAIT = "constraint_orientation_portrait"
        const val ORIENTATION_LANDSCAPE = "constraint_orientation_landscape"

        val ORIENTATION_CONSTRAINTS = arrayOf(
            ORIENTATION_PORTRAIT,
            ORIENTATION_LANDSCAPE,
            ORIENTATION_0,
            ORIENTATION_90,
            ORIENTATION_180,
            ORIENTATION_270
        )

        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_BT_ADDRESS = "extra_bluetooth_device_address"
        const val EXTRA_BT_NAME = "extra_bluetooth_device_name"

        //Categories
        const val CATEGORY_APP = 0
        const val CATEGORY_BLUETOOTH = 1
        const val CATEGORY_SCREEN = 2
        const val CATEGORY_ORIENTATION = 3

        val CATEGORY_LABEL_MAP = mapOf(
            CATEGORY_APP to R.string.constraint_category_app,
            CATEGORY_BLUETOOTH to R.string.constraint_category_bluetooth,
            CATEGORY_SCREEN to R.string.constraint_category_screen,
            CATEGORY_ORIENTATION to R.string.constraint_category_orientation
        )

        fun appConstraint(@ConstraintType type: String, packageName: String): Constraint {
            return Constraint(type, Extra(EXTRA_PACKAGE_NAME, packageName))
        }

        fun btConstraint(@ConstraintType type: String, address: String, name: String): Constraint {
            return Constraint(type, Extra(EXTRA_BT_ADDRESS, address), Extra(EXTRA_BT_NAME, name))
        }

        val DESERIALIZER = jsonDeserializer {
            val type by it.json.byString(NAME_TYPE)

            val extrasJsonArray by it.json.byArray(NAME_EXTRAS)
            val extraList = it.context.deserialize<List<Extra>>(extrasJsonArray) ?: listOf()

            Constraint(type, extraList)
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

