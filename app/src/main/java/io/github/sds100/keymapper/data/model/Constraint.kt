package io.github.sds100.keymapper.data.model

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.result.ExtraNotFound
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success

/**
 * Created by sds100 on 17/03/2020.
 */
@StringDef(value = [
    Constraint.APP_FOREGROUND,
    Constraint.BT_DEVICE_CONNECTED
])
annotation class ConstraintId

@IntDef(value = [
    Constraint.MODE_AND,
    Constraint.MODE_OR
])
annotation class ConstraintMode

@IntDef(value = [
    Constraint.CATEGORY_APP,
    Constraint.CATEGORY_BLUETOOTH]
)
annotation class ConstraintCategory

data class Constraint(@ConstraintId val id: String, val extraList: List<Extra>) {

    constructor(id: String, extra: Extra) : this(id, listOf(extra))

    companion object {
        const val MODE_OR = 0
        const val MODE_AND = 1
        const val DEFAULT_MODE = MODE_AND

        const val APP_FOREGROUND = "constraint_app_foreground"
        const val BT_DEVICE_CONNECTED = "constraint_bt_device_connected"
        const val BT_DEVICE_DISCONNECTED = "constraint_bt_device_disconnected"

        //Categories
        const val CATEGORY_APP = 0
        const val CATEGORY_BLUETOOTH = 1

        val CATEGORY_LABEL_MAP = mapOf(
            CATEGORY_APP to R.string.constraint_category_app,
            CATEGORY_BLUETOOTH to R.string.constraint_category_bluetooth
        )

        fun appConstraint(packageName: String): Constraint {
            val extra = Extra(Extra.EXTRA_PACKAGE_NAME, packageName)

            return Constraint(APP_FOREGROUND, extra)
        }
    }

    fun getExtraData(extraId: String): Result<String> {
        val extra = extraList.find { it.id == extraId } ?: return ExtraNotFound()

        return Success(extra.data)
    }
}

