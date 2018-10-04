package io.github.sds100.keymapper.Adapters

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.OnItemClickListener
import io.github.sds100.keymapper.Utils.AppShortcutUtils

/**
 * Created by sds100 on 17/07/2018.
 */

/**
 * Display app shortcuts in a RecyclerView
 */
class AppShortcutAdapter(
        private val packageManager: PackageManager,
        onItemClickListener: OnItemClickListener<ResolveInfo>
) : SimpleItemAdapter<ResolveInfo>(
        AppShortcutUtils.getAppShortcuts(packageManager),
        onItemClickListener) {

    override fun getItemText(item: ResolveInfo): String {
        return item.loadLabel(packageManager).toString()
    }

    override fun getItemImage(item: ResolveInfo): Drawable? {
        return item.loadIcon(packageManager)
    }
}