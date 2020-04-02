package io.github.sds100.keymapper.data.model

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 31/03/2020.
 */
data class UnsupportedSystemActionListItemModel(
    val id: String,
    val description: String,
    val icon: Drawable?,
    val reason: Failure
)