package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import kotlinx.android.parcel.Parcelize

/**
 * Created by sds100 on 26/01/2019.
 */

@Parcelize
data class Extra(
    @SerializedName(NAME_ID)
    val id: String,

    @SerializedName(NAME_DATA)
    val data: String,
) : Parcelable {
    companion object {

        // DON'T CHANGE THESE. Used for JSON serialization and parsing.
        const val NAME_ID = "id"
        const val NAME_DATA = "data"

        val DESERIALIZER = jsonDeserializer {
            val id by it.json.byString(NAME_ID)
            val data by it.json.byString(NAME_DATA)

            Extra(id, data)
        }
    }
}

fun List<Extra>.getData(extraId: String): Result<String> {
    return find { it.id == extraId }.let {
        it ?: return@let Error.ExtraNotFound(extraId)

        Success(it.data)
    }
}
