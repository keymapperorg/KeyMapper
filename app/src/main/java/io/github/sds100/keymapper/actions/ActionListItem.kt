package io.github.sds100.keymapper.actions

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.ui.TintType

/**
 * Created by sds100 on 26/03/2020.
 */

data class ActionListItem(
    val id: String,
    val tintType: TintType,
    val title: String?,
    val icon: Drawable? = null,
    val extraInfo: String? = null,
    val errorMessage: String? = null,
    val dragAndDrop: Boolean,
)
