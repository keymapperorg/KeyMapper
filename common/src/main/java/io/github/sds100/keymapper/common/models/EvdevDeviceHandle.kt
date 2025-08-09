package io.github.sds100.keymapper.common.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class EvdevDeviceHandle(
    /**
     * The path to the device. E.g /dev/input/event1
     */
    val path: String,
    val name: String,
    val bus: Int,
    val vendor: Int,
    val product: Int,
) : Parcelable