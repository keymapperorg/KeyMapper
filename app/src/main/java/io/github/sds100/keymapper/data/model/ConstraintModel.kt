package io.github.sds100.keymapper.data.model

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 17/03/2020.
 */

data class ConstraintModel(
    val id: String,
    val description: String? = null,
    val failure: Failure? = null,
    val errorMessage: String? = null,
    val icon: Drawable? = null
) {
    val hasError: Boolean
        get() = failure != null
}