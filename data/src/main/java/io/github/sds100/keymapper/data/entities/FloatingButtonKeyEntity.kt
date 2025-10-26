package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class FloatingButtonKeyEntity(
    @SerializedName(NAME_BUTTON_UID)
    val buttonUid: String,
    @SerializedName(NAME_CLICK_TYPE)
    override val clickType: Int = SHORT_PRESS,
    @SerializedName(NAME_UID)
    override val uid: String = UUID.randomUUID().toString(),
) : TriggerKeyEntity(),
    Parcelable {
    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_BUTTON_UID = "button_uid"
    }
}
