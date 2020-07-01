package io.github.sds100.keymapper.data.model

import androidx.annotation.StringRes

/**
 * Created by sds100 on 04/06/20.
 */
data class CheckBoxListItemModel(
    val id: String,
    @StringRes val label: Int,
    val isChecked: Boolean
)