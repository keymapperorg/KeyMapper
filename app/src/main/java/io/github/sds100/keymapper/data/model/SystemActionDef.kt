package io.github.sds100.keymapper.data.model

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.util.result.OptionsNotRequired
import io.github.sds100.keymapper.util.result.Result
import io.github.sds100.keymapper.util.result.Success

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

    @StringRes val messageOnSelection: Int? = null,

    @StringRes val descriptionRes: Int,
    @StringRes val descriptionFormattedRes: Int? = null,

    val optionType: OptionType = OptionType.SINGLE,

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