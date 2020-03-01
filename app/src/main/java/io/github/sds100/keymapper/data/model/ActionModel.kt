package io.github.sds100.keymapper.data.model

import android.graphics.drawable.Drawable

/**
 * Created by sds100 on 26/02/2020.
 */

data class ActionModel(
    val title: String? = null,
    val errorDescription: String? = null,
    val icon: Drawable? = null
) {
    val hasError: Boolean
        get() = errorDescription != null
}