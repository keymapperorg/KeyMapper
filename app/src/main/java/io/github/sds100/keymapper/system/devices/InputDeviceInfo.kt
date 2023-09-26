package io.github.sds100.keymapper.system.devices

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 07/03/2021.
 */

@Parcelize
@Serializable
data class InputDeviceInfo(
    val descriptor: String,
    val name: String,
    val id: Int,
    val isExternal: Boolean,
    val isGameController: Boolean
) : Parcelable