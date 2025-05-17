package io.github.sds100.keymapper.system.url

import io.github.sds100.keymapper.common.util.result.Result


interface OpenUrlAdapter {
    fun openUrl(url: String): Result<*>
}
