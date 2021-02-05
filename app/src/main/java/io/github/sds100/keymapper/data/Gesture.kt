package io.github.sds100.keymapper.data

import android.os.Parcelable
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.util.KeyEventAction
import kotlinx.android.parcel.Parcelize

interface Gesture : Parcelable {
    enum class GestureType {
        TAP
    }

    val type: GestureType
    val action: Action
    val keyEventAction: KeyEventAction
}

@Parcelize
class TapGesture(override val action: Action,
                 override val keyEventAction: KeyEventAction,
                 val x: Float, val y: Float) : Gesture {
    override val type = Gesture.GestureType.TAP
}