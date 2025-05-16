package io.github.sds100.keymapper.system.popup

import android.content.Context
import splitties.toast.toast

class AndroidToastAdapter(context: Context) : PopupMessageAdapter {
    private val ctx: Context = context.applicationContext

    override fun showPopupMessage(message: String) {
        ctx.toast(message)
    }
}
