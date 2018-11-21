package io.github.sds100.keymapper.Adapters

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import io.github.sds100.keymapper.OnItemClickListener

/**
 * Created by sds100 on 17/07/2018.
 */

/**
 * Display apps in a RecyclerView
 */
class AppListAdapter(
        onItemClickListener: OnItemClickListener<ApplicationInfo>,
        appList: List<ApplicationInfo>,
        private val mPackageManager: PackageManager
) : SimpleItemAdapter<ApplicationInfo>(appList, onItemClickListener) {

    override fun getItemText(item: ApplicationInfo) = item.loadLabel(mPackageManager).toString()
    override fun getItemImage(item: ApplicationInfo) = item.loadIcon(mPackageManager)!!
}