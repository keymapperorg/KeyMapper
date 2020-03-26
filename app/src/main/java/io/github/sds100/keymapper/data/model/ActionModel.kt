package io.github.sds100.keymapper.data.model

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 26/03/2020.
 */

data class ActionModel(
    val id: String,
    val title: String? = null,
    val icon: Drawable? = null,
    val flags: String? = null,
    val failure: Failure? = null
) {
    val hasFlags: Boolean
        get() = flags != null

    val hasError: Boolean
        get() = failure != null
}