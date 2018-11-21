package io.github.sds100.keymapper.Adapters

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.OnItemClickListener

/**
 * Created by sds100 on 17/07/2018.
 */

/**
 * Display app shortcuts in a RecyclerView
 */
class AppShortcutAdapter(
        onItemClickListener: OnItemClickListener<ResolveInfo>,
        appShortcutList: List<ResolveInfo>,
        private val mPackageManager: PackageManager

) : SimpleItemAdapter<ResolveInfo>(appShortcutList, onItemClickListener) {

    override fun getItemText(item: ResolveInfo): String {
        return item.loadLabel(mPackageManager).toString()
    }

    override fun getItemImage(item: ResolveInfo): Drawable? {
        return item.loadIcon(mPackageManager)
    }
}