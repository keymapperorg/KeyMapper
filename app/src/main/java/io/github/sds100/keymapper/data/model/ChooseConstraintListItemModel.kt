package io.github.sds100.keymapper.data.model

import androidx.annotation.StringRes

/**
 * Created by sds100 on 22/03/2020.
 */
data class ChooseConstraintListItemModel(
    val id: String,
    @ConstraintCategory val categoryId: Int,
    @StringRes val description: Int
)