package io.github.sds100.keymapper.mappings.keymaps.trigger

data class TriggerKeyListItemState(
    val id: String,
    val name: String,

    /**
     * null if should be hidden
     */
    val clickTypeString: String? = null,

    val extraInfo: String?,

    val linkType: TriggerKeyLinkType,

    val isDragDropEnabled: Boolean,
)
