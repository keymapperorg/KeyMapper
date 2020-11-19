package io.github.sds100.keymapper.data.model

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 26/03/2020.
 */

data class ActionModel(
    val id: String,
    val type: ActionType,
    val title: String? = null,
    val icon: Drawable? = null,
    val extraInfo: String? = null,
    val failure: Failure? = null,
    val briefErrorMessage: String? = null
) {
    val hasFlags: Boolean
        get() = extraInfo != null

    val hasError: Boolean
        get() = failure != null
}