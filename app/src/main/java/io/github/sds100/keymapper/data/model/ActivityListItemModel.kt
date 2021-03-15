package io.github.sds100.keymapper.data.model

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.ActivityInfo
import io.github.sds100.keymapper.util.ISearchable

/**
 * Created by sds100 on 27/01/2020.
 */
data class ActivityListItemModel(
    val appName: String,
    val activityInfo: ActivityInfo,
    val icon: Drawable?
) : ISearchable {
    val id: String
        get() = "${activityInfo.packageName}${activityInfo.activityName}"

    override fun getSearchableString() = "$appName $activityInfo"
}