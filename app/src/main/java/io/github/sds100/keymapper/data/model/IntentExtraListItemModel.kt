package io.github.sds100.keymapper.data.model

/**
 * Created by sds100 on 01/01/21.
 */

// don't factor out properties because otherwise the Epoxyrecyclerview doesn't update when the
// model changes
sealed class IntentExtraListItemModel

data class BoolIntentExtraListItemModel(
    val uid: String,
    val name: String,
    val value: Boolean,
    val isValid: Boolean
) : IntentExtraListItemModel()

data class GenericIntentExtraListItemModel(
    val uid: String,
    val typeString: String,
    val name: String,
    val value: String,
    val isValid: Boolean,
    val exampleString: String,
    val inputType: Int
) : IntentExtraListItemModel()
