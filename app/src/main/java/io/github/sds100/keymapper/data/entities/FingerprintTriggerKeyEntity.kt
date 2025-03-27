package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class FingerprintTriggerKeyEntity(
    @SerializedName(NAME_FINGERPRINT_GESTURE_TYPE)
    val type: Int = ID_SWIPE_DOWN,

    @SerializedName(NAME_CLICK_TYPE)
    override val clickType: Int = SHORT_PRESS,

    @SerializedName(NAME_UID)
    override val uid: String = UUID.randomUUID().toString(),
) : TriggerKeyEntity(),
    Parcelable {

    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_FINGERPRINT_GESTURE_TYPE = "fingerprintGestureType"

        // IDS! DON'T CHANGE
        const val ID_SWIPE_DOWN = 0
        const val ID_SWIPE_UP = 1
        const val ID_SWIPE_LEFT = 2
        const val ID_SWIPE_RIGHT = 3
    }
}
