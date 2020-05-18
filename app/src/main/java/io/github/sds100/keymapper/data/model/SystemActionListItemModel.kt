package io.github.sds100.keymapper.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Created by sds100 on 31/03/2020.
 */
data class SystemActionListItemModel(
    val id: String,
    val categoryId: String,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int?,
    val requiresRoot: Boolean
)