package io.github.sds100.keymapper.Adapters

import android.graphics.drawable.Drawable
import android.view.KeyEvent
import io.github.sds100.keymapper.Interfaces.OnItemClickListener
import io.github.sds100.keymapper.Utils.KeycodeUtils

/**
 * Created by sds100 on 17/07/2018.
 */

/**
 * Display all keycodes in a RecyclerView
 */
class KeycodeAdapter(
        keyCodeList: List<Int> = KeycodeUtils.getKeyCodes(),
        onItemClickListener: OnItemClickListener<Int>
) : SimpleItemAdapter<Int>(keyCodeList, onItemClickListener) {

    override fun getItemText(item: Int): String {
        return KeyEvent.keyCodeToString(item)
    }

    override fun getItemImage(item: Int): Drawable? = null
}