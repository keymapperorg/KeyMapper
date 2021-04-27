package io.github.sds100.keymapper.system.apps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.ui.ISearchable

/**
 * Created by sds100 on 27/01/2020.
 */
data class AppListItem(
    val packageName: String,
    val appName: String,
    val icon: Drawable?
) : ISearchable {
    override fun getSearchableString() = appName
}