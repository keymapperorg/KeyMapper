package io.github.sds100.keymapper.util.ui

/**
 * Created by sds100 on 31/03/2021.
 */

sealed class TextListItem : ListItem {
    data class Success(override val id: String, val text: String) : TextListItem()
    data class Error(
        override val id: String,
        val text: String,
        val customButtonText: String? = null,
    ) : TextListItem()
}
