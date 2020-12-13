package io.github.sds100.keymapper

import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.ConstraintMode

/**
 * Created by sds100 on 13/12/20.
 */
interface IConstraintDelegate {
    fun Array<Constraint>.constraintsSatisfied(@ConstraintMode mode: Int): Boolean
}