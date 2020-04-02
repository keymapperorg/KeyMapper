package io.github.sds100.keymapper.data.model

import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.util.result.Failure
import io.github.sds100.keymapper.util.result.OptionsNotRequired
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success
import splitties.resources.appDrawable
import splitties.resources.appStr

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
    val getIcon: () -> Drawable? = {
        if (iconRes != null) {
            appDrawable(iconRes)
        } else {
            null
        }
    },

    @StringRes messageOnSelection: Int? = null,
    val getMessageOnSelection: () -> String? = {
        if (messageOnSelection != null) {
            appStr(messageOnSelection)
        } else {
            null
        }
    },

    @StringRes descriptionRes: Int,
    val getDescription: () -> String = { appStr(descriptionRes) },

    val getDescriptionWithOption: (optionLabel: String) -> String? = { _ -> null },

    options: List<String>? = null,

    val getOptions: () -> Result<List<String>> = {
        if (options == null) {
            OptionsNotRequired()
        } else {
            Success(options)
        }
    }) {

    val hasOptions: Boolean
        get() = getOptions() !is OptionsNotRequired
}