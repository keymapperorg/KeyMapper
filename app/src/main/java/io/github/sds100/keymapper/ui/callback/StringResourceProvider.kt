package io.github.sds100.keymapper.ui.callback

import androidx.annotation.StringRes

/**
 * Created by sds100 on 18/05/2020.
 */
interface StringResourceProvider {
    fun getStringResource(@StringRes resId: Int): String
}