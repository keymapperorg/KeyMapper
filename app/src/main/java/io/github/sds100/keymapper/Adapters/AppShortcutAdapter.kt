package io.github.sds100.keymapper.Adapters

import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.AppShortcutHelper

/**
 * Created by sds100 on 17/07/2018.
 */

class AppShortcutAdapter(
        private val packageManager: PackageManager,
        onItemClickListener: OnItemClickListener<ResolveInfo>
) : SimpleItemAdapter<ResolveInfo>(
        AppShortcutHelper.getAppShortcuts(packageManager),
        onItemClickListener) {

    override fun getItemText(item: ResolveInfo): String {
        return item.activityInfo.name
    }

    override fun getItemImage(item: ResolveInfo): Drawable? {
        return item.loadIcon(packageManager)
    }
}