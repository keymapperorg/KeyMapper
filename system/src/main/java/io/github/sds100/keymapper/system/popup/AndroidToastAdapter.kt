package io.github.sds100.keymapper.system.popup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import splitties.toast.toast

@Singleton
class AndroidToastAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : PopupMessageAdapter {
    private val ctx: Context = context.applicationContext

    override fun showPopupMessage(message: String) {
        ctx.toast(message)
    }
}
