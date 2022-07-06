package io.github.sds100.keymapper.system.popup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import splitties.toast.toast
import javax.inject.Inject

/**
 * Created by sds100 on 17/04/2021.
 */
class AndroidToastAdapter @Inject constructor(@ApplicationContext context: Context) : ToastAdapter {
    private val ctx: Context = context.applicationContext

    override fun showPopupMessage(message: String) {
        ctx.toast(message)
    }
}