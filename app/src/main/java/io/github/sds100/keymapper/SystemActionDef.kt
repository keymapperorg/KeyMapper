package io.github.sds100.keymapper

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes

/**
 * Created by sds100 on 23/11/2018.
 */

/**
 * @param messageOnSelection A message to show when the user selects this action
 */
class SystemActionDef(
        val id: String,
        val category: String,
        val permissions: Array<String> = arrayOf(),
        val features: Array<String> = arrayOf(),
        val minApi: Int = Constants.MIN_API,
        @DrawableRes val iconRes: Int? = null,
        @StringRes val descriptionRes: Int,
        @StringRes val messageOnSelection: Int? = null
) {
    constructor(id: String,
                category: String,
                permission: String,
                minApi: Int = Constants.MIN_API,
                @DrawableRes iconRes: Int? = null,
                @StringRes descriptionRes: Int,
                @StringRes messageOnSelection: Int? = null
    ) : this(
            id,
            category,
            arrayOf(permission),
            minApi = minApi,
            iconRes = iconRes,
            descriptionRes = descriptionRes,
            messageOnSelection = messageOnSelection
    )

    constructor(id: String,
                category: String,
                permission: String,
                feature: String,
                minApi: Int = Constants.MIN_API,
                @DrawableRes iconRes: Int? = null,
                @StringRes descriptionRes: Int,
                @StringRes messageOnSelection: Int? = null
    ) : this(
            id,
            category,
            arrayOf(permission),
            arrayOf(feature),
            minApi,
            iconRes,
            descriptionRes,
            messageOnSelection
    )
}