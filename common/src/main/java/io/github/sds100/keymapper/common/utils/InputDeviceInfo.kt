package io.github.sds100.keymapper.common.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class InputDeviceInfo(
    val descriptor: String,
    val name: String,
    val id: Int,
    val isExternal: Boolean,
    val isGameController: Boolean,
    val sources: Int,
) : Parcelable {
    fun supportsSource(source: Int): Boolean = sources and source == source
}
