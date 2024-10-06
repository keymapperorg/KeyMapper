package io.github.sds100.keymapper.mappings.keymaps.trigger

/**
 * Created by sds100 on 27/03/2020.
 */
data class TriggerKeyListItem(
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
