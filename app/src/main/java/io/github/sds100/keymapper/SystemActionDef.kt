package io.github.sds100.keymapper

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Created by sds100 on 23/11/2018.
 */

class SystemActionDef(
        val id: String,
        val category: String,
        val permissions: Array<String> = arrayOf(),
        val minApi: Int = Constants.MIN_API,
        @DrawableRes val iconRes: Int? = null,
        @StringRes val descriptionRes: Int
) {
    constructor(id: String,
                category: String,
                permission: String,
                minApi: Int = Constants.MIN_API,
                @DrawableRes iconRes: Int? = null,
                @StringRes descriptionRes: Int
    ) : this(id, category, arrayOf(permission), minApi, iconRes, descriptionRes)
}