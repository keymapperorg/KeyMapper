package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 01/01/21.
 */

data class IntentExtraListItemModel(
    val uid: String,
    val typeString: String,
    val name: String,
    val value: String,
    val isValid: Boolean,
    val exampleString: String
)