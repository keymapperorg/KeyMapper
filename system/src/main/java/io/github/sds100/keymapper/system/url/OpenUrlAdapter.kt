package io.github.sds100.keymapper.system.url

import io.github.sds100.keymapper.common.utils.KMResult

interface OpenUrlAdapter {
    fun openUrl(url: String): KMResult<*>
}
