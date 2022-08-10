package io.github.sds100.keymapper.actions.tapscreen

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

/**
 * Created by sds100 on 25/03/2021.
 */
@Serializable
@Parcelize
data class PickCoordinateResult(val x: Int, val y: Int, val description: String) : Parcelable
