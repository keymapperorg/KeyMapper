package io.github.sds100.keymapper.system.intents

// don't factor out properties because otherwise the Epoxyrecyclerview doesn't update when the
// model changes
sealed class IntentExtraListItem {
    abstract val uid: String
}

data class BoolIntentExtraListItem(
    override val uid: String,
    val name: String,
    val value: Boolean,
    val isValid: Boolean,
) : IntentExtraListItem()

data class GenericIntentExtraListItem(
    override val uid: String,
    val typeString: String,
    val name: String,
    val value: String,
    val isValid: Boolean,
    val exampleString: String,
    val inputType: Int,
) : IntentExtraListItem()
