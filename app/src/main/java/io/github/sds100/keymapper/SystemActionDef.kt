package io.github.sds100.keymapper

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.sds100.keymapper.util.str

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
    val maxApi: Int = Constants.MAX_API,
    @DrawableRes val iconRes: Int? = null,
    @StringRes val descriptionRes: Int,
    @StringRes val messageOnSelection: Int? = null,
    val formattedDescription: (ctx: Context, optionText: String) -> String = { ctx, _ -> ctx.str(descriptionRes) },

    /**
     * A map of any option ids to their label.
     */
    val options: List<String> = listOf()) {

    constructor(id: String,
                category: String,
                permission: String,
                minApi: Int = Constants.MIN_API,
                @DrawableRes iconRes: Int? = null,
                @StringRes descriptionRes: Int,
                @StringRes messageOnSelection: Int? = null,
                formattedDescription: (ctx: Context, optionText: String) -> String = { ctx, _ -> ctx.str(descriptionRes) },
                options: List<String> = listOf()
    ) : this(
        id,
        category,
        arrayOf(permission),
        minApi = minApi,
        iconRes = iconRes,
        descriptionRes = descriptionRes,
        messageOnSelection = messageOnSelection,
        formattedDescription = formattedDescription,
        options = options
    )

    constructor(id: String,
                category: String,
                permission: String,
                feature: String,
                minApi: Int = Constants.MIN_API,
                @DrawableRes iconRes: Int? = null,
                @StringRes descriptionRes: Int,
                @StringRes messageOnSelection: Int? = null,
                formattedDescription: (ctx: Context, optionText: String) -> String = { ctx, _ -> ctx.str(descriptionRes) },
                options: List<String> = listOf()
    ) : this(
        id = id,
        category = category,
        permissions = arrayOf(permission),
        features = arrayOf(feature),
        minApi = minApi,
        iconRes = iconRes,
        descriptionRes = descriptionRes,
        messageOnSelection = messageOnSelection,
        formattedDescription = formattedDescription,
        options = options
    )

    val hasOptions: Boolean
        get() = options.isNotEmpty()
}