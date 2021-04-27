package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.ui.ISearchable

/**
 * Created by sds100 on 29/03/2020.
 */

data class AppShortcutListItem(
    val shortcutInfo: AppShortcutInfo,
    val label: String,
    val icon: Drawable?
) : ISearchable {
    override fun getSearchableString() = label
}