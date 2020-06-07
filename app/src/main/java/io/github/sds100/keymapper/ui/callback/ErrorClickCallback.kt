package io.github.sds100.keymapper.ui.callback

import io.github.sds100.keymapper.util.result.Failure

/**
 * Created by sds100 on 07/03/2020.
 */
interface ErrorClickCallback {
    fun onErrorClick(failure: Failure)
}