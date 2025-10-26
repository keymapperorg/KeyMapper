package io.github.sds100.keymapper.data
import io.github.sds100.keymapper.common.utils.KMError

object DataError {
    data class ExtraNotFound(
        val extraId: String,
    ) : KMError()
}
