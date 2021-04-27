package io.github.sds100.keymapper.system.keyevents

import io.github.sds100.keymapper.util.ui.ISearchable

/**
 * Created by sds100 on 23/03/2021.
 */
data class KeyCodeListItem(val keyCode: Int, val label: String) : ISearchable {
    override fun getSearchableString() = label
}
