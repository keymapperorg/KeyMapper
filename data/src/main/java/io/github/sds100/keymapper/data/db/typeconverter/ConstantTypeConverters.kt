package io.github.sds100.keymapper.data.db.typeconverter

import io.github.sds100.keymapper.common.utils.Orientation

object ConstantTypeConverters {
    val ORIENTATION_MAP = mapOf(
        Orientation.ORIENTATION_0 to "rotation_0",
        Orientation.ORIENTATION_90 to "rotation_90",
        Orientation.ORIENTATION_180 to "rotation_180",
        Orientation.ORIENTATION_270 to "rotation_270",
    )
}
