package io.github.sds100.keymapper.data.model

import android.content.pm.ActivityInfo
import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.ISearchable

/**
 * Created by sds100 on 29/03/2020.
 */

data class AppShortcutListItemModel(
    val activityInfo: ActivityInfo,
    val label: String,
    val icon: Drawable?
) : ISearchable {
    override fun getSearchableString() = label
}