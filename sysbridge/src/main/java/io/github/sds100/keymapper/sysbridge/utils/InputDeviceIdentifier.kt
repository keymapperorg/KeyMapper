package io.github.sds100.keymapper.sysbridge.utils

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
internal data class InputDeviceIdentifier(
    val name: String,
    val bus: Int,
    val vendor: Int,
    val product: Int,
    val descriptor: String,
    val bluetoothAddress: String?
) : Parcelable
