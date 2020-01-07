package io.github.sds100.keymapper

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.sds100.keymapper.util.ErrorCodeUtils
import io.github.sds100.keymapper.util.drawable
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

    @DrawableRes iconRes: Int? = null,
    val getIcon: (ctx: Context) -> Drawable? = {
        if (iconRes != null) {
            it.drawable(iconRes)
        } else {
            null
        }
    },

    @StringRes messageOnSelection: Int? = null,
    val getMessageOnSelection: (ctx: Context) -> String? = {
        if (messageOnSelection != null) {
            it.str(messageOnSelection)
        } else {
            null
        }
    },

    @StringRes descriptionRes: Int,
    val getDescription: (ctx: Context) -> String = { ctx -> ctx.str(descriptionRes) },

    val getDescriptionWithOption: (ctx: Context, optionLabel: String) -> String? = { _, _ -> null },

    options: List<String>? = null,
    val getOptions: (ctx: Context) -> Result<List<String>> = {
        @Suppress("IfThenToElvis")
        if (options == null) {
            null.result(ErrorCodeUtils.ERROR_CODE_OPTIONS_NOT_REQUIRED)
        } else {
            options.result()
        }
    })