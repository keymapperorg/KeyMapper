package io.github.sds100.keymapper.data.model

import androidx.annotation.StringRes

/**
 * Created by sds100 on 01/01/21.
 */

data class IntentExtraListItemModel(
    @StringRes val typeString: Int,
    val id: String,
    val value: String,
    val isValid: Boolean,
    @StringRes val exampleString: Int
)