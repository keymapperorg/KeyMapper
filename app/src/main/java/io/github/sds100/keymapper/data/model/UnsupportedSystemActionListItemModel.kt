package io.github.sds100.keymapper.data.model

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 31/03/2020.
 */
data class UnsupportedSystemActionListItemModel(
    val id: String,
    @StringRes val description: Int,
    @DrawableRes val icon: Int?,
    val reason: Failure
)