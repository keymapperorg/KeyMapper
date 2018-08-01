package io.github.sds100.keymapper.Adapters

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable

/**
 * Created by sds100 on 17/07/2018.
 */
class AppListAdapter(appList: List<ApplicationInfo>,
                     private val packageManager: PackageManager,
                     private val onItemClickListener: OnItemClickListener<ApplicationInfo>
) : SimpleItemAdapter<ApplicationInfo>(appList, onItemClickListener) {

    private val ApplicationInfo.iconDrawable: Drawable
        get() = loadIcon(packageManager)

    private val ApplicationInfo.appName: String
        get() = loadLabel(packageManager).toString()

    override fun getItemText(item: ApplicationInfo) = item.appName

    override fun getItemImage(item: ApplicationInfo) = item.iconDrawable
}