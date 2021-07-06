package io.github.sds100.keymapper.actions.system

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.ui.ISearchable
import io.github.sds100.keymapper.util.ui.ListItem

/**
 * Created by sds100 on 31/03/2020.
 */
data class SystemActionListItem(
    val systemActionId: SystemActionId,
    val title: String,
    val icon: Drawable?,
    val showRequiresRootMessage: Boolean
) : ListItem, ISearchable {
    override val id: String = systemActionId.toString()
    override fun getSearchableString() = title
}