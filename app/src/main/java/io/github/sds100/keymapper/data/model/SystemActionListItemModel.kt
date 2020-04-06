package io.github.sds100.keymapper.data.model

import android.content.Context
import android.graphics.drawable.Drawable

/**
 * Created by sds100 on 31/03/2020.
 */
data class SystemActionListItemModel(
    val id: String,
    val category: String,
    val description: String,
    val getIcon: (ctx: Context) -> Drawable?,
    val requiresRoot: Boolean
)