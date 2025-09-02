package io.github.sds100.keymapper.data.entities

import android.os.Parcelable
import com.github.salomonbrys.kotson.byString
import com.github.salomonbrys.kotson.jsonDeserializer
import com.google.gson.annotations.SerializedName
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.data.DataError
import kotlinx.parcelize.Parcelize

@Parcelize
data class EntityExtra(
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

            EntityExtra(id, data)
        }
    }
}

fun List<EntityExtra>.getData(extraId: String): KMResult<String> {
    return find { it.id == extraId }.let {
        it ?: return@let DataError.ExtraNotFound(extraId)

        Success(it.data)
    }
}
