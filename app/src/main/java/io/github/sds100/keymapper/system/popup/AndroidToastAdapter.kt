package io.github.sds100.keymapper.system.popup

import android.content.Context
import splitties.toast.toast

/**
 * Created by sds100 on 17/04/2021.
 */
class AndroidToastAdapter(context: Context) : PopupMessageAdapter {
    private val ctx: Context = context.applicationContext

    override fun showPopupMessage(message: String) {
        ctx.toast(message)
    }
}
