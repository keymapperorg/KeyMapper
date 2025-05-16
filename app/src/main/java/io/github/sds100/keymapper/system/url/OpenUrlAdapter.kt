package io.github.sds100.keymapper.system.url

import io.github.sds100.keymapper.common.result.Result

/**
 * Created by sds100 on 24/04/2021.
 */
interface OpenUrlAdapter {
    fun openUrl(url: String): Result<*>
}
