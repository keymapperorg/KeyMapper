package io.github.sds100.keymapper.util.ui

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

/**
 * Created by sds100 on 08/01/21.
 */

data class TabFragmentModel(
    @StringRes val tabTitle: Int,
    val searchStateKey: String?,
    val fragmentCreator: () -> Fragment,
)
