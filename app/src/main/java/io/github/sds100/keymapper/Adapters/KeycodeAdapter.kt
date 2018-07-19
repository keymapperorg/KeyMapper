package io.github.sds100.keymapper.Adapters

import android.graphics.drawable.Drawable
import android.view.KeyEvent
import io.github.sds100.keymapper.KeycodeHelper

/**
 * Created by sds100 on 17/07/2018.
 */

class KeycodeAdapter() : SimpleItemAdapter<Int>(KeycodeHelper.getKeyCodes()) {
    override fun getItemText(item: Int): String {
        return KeyEvent.keyCodeToString(item)
    }

    override fun getItemImage(item: Int): Drawable? = null
}