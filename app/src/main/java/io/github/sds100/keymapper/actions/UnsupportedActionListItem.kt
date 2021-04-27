package io.github.sds100.keymapper.actions

import android.graphics.drawable.Drawable

/**
 * Created by sds100 on 31/03/2020.
 */
data class UnsupportedActionListItem(
    val id: String,
    val description: String,
    val icon: Drawable?,
    val reason: String
)