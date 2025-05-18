package io.github.sds100.keymapper.system.popup

import android.content.Context
import android.widget.Toast
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidToastAdapter @Inject constructor(
    @ApplicationContext private val ctx: Context,
) : ToastAdapter {
    override fun show(message: String) {
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
    }
}
