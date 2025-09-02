package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class AssistantTriggerKeyEntity(
    /**
     * The type of assistant that triggers this key. The voice assistant
     * is the assistant that handles voice commands and the device assistant
     * is the one selected in the settings as the default for reading on-screen
     * content.
     */
    @SerializedName(NAME_ASSISTANT_TYPE)
    val type: String = ASSISTANT_TYPE_ANY,

    @SerializedName(NAME_CLICK_TYPE)
    override val clickType: Int = SHORT_PRESS,

    @SerializedName(NAME_UID)
    override val uid: String = UUID.randomUUID().toString(),
) : TriggerKeyEntity(),
    Parcelable {

    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_ASSISTANT_TYPE = "assistantType"

        // IDS! DON'T CHANGE
        const val ASSISTANT_TYPE_ANY = "any"
        const val ASSISTANT_TYPE_VOICE = "voice"
        const val ASSISTANT_TYPE_DEVICE = "device"
    }
}
