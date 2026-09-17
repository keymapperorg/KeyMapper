package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.github.salomonbrys.kotson.byInt
import com.github.salomonbrys.kotson.byNullableString
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

@Parcelize
data class ConstraintGroupEntity(
    @SerializedName(NAME_UID)
    val uid: String,

    @SerializedName(NAME_NAME)
    val name: String? = null,

    @SerializedName(NAME_MODE)
    val mode: Int = ConstraintEntity.DEFAULT_MODE,
) : Parcelable {
    companion object {
        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_UID = "uid"
        const val NAME_NAME = "name"
        const val NAME_MODE = "mode"

        val DESERIALIZER = jsonDeserializer {
            val uid by it.json.byString(NAME_UID)
            val name by it.json.byNullableString(NAME_NAME)
            val mode by it.json.byInt(NAME_MODE) { ConstraintEntity.DEFAULT_MODE }

            ConstraintGroupEntity(uid = uid, name = name, mode = mode)
        }
    }
}
