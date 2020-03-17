package io.github.sds100.keymapper.data.model

import androidx.annotation.StringDef

/**
 * Created by sds100 on 17/03/2020.
 */
@StringDef(value = [
    Constraint.CONSTRAINT_APP
])
annotation class ConstraintId

data class Constraint(@ConstraintId val id: String, val extra: List<Extra>) {
    companion object{
        const val CONSTRAINT_APP = "constraint_app"
    }
}

