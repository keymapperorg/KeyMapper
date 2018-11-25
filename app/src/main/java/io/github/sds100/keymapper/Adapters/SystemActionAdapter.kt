package io.github.sds100.keymapper.Adapters

import android.content.Context
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import io.github.sds100.keymapper.OnItemClickListener
import io.github.sds100.keymapper.SystemActionDef
import io.github.sds100.keymapper.Utils.SystemActionUtils

/**
 * Created by sds100 on 17/07/2018.
 */

class SystemActionAdapter(
        private val ctx: Context,
        systemActionList: List<SystemActionDef> = SystemActionUtils.SYSTEM_ACTION_DEFINITIONS,
        onItemClickListener: OnItemClickListener<SystemActionDef>
) : SimpleItemAdapter<SystemActionDef>(
        systemActionList,
        onItemClickListener
) {
    override fun getItemText(item: SystemActionDef): String {
        return ctx.getString(item.descriptionRes)
    }

    override fun getItemImage(item: SystemActionDef): Drawable? {
        if (item.iconRes == null) return null

        return ContextCompat.getDrawable(ctx, item.iconRes)
    }
}