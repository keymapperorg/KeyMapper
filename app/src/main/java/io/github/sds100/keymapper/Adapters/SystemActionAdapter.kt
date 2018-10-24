package io.github.sds100.keymapper.Adapters

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.OnItemClickListener
import io.github.sds100.keymapper.SystemActionListItem
import io.github.sds100.keymapper.Utils.SystemActionUtils

/**
 * Created by sds100 on 17/07/2018.
 */

class SystemActionAdapter(
        private val ctx: Context,
        onItemClickListener: OnItemClickListener<SystemActionListItem>
) : SimpleItemAdapter<SystemActionListItem>(
        SystemActionUtils.getSystemActionListItems(),
        onItemClickListener
) {
    override fun getItemText(item: SystemActionListItem): String {
        return ctx.getString(item.stringId)
    }

    override fun getItemImage(item: SystemActionListItem): Drawable? {
        if (item.iconId == null) return null

        return ContextCompat.getDrawable(ctx, item.iconId)
    }
}