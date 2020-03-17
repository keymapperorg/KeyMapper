package io.github.sds100.keymapper.data.model

import androidx.annotation.IntDef
import androidx.annotation.StringDef
import io.github.sds100.keymapper.util.result.ExtraNotFound
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success

/**
 * Created by sds100 on 17/03/2020.
 */
@StringDef(value = [
    Constraint.APP_FOREGROUND
])
annotation class ConstraintId

@IntDef(value = [
    Constraint.AND,
    Constraint.OR
])
annotation class ConstraintMode

data class Constraint(@ConstraintId val id: String, val extraList: List<Extra>) {

    constructor(id: String, extra: Extra) : this(id, listOf(extra))

    companion object {
        const val AND = 1
        const val OR = 0

        const val APP_FOREGROUND = "constraint_app_foreground"

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

