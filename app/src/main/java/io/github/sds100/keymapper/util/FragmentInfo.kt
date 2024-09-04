package io.github.sds100.keymapper.util

import androidx.annotation.StringRes
import androidx.fragment.app.Fragment

/**
 * Created by sds100 on 17/01/21.
 */

open class FragmentInfo(
    @StringRes val header: Int? = null,
    @StringRes val supportUrl: Int? = null,
    val instantiate: () -> Fragment,
)
