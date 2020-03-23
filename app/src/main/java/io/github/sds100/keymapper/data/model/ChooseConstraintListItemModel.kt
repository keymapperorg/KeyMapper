package io.github.sds100.keymapper.data.model

import io.github.sds100.keymapper.util.ConstraintUtils
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 22/03/2020.
 */
data class ChooseConstraintListItemModel(
    val id: String,
    @ConstraintCategory val categoryId: Int,
    @ConstraintId val description: String,
    val error: Failure? = ConstraintUtils.isSupported(id)
) {
    val isSupported: Boolean
        get() = error != null
}