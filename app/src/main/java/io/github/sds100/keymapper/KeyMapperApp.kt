package io.github.sds100.keymapper

import android.annotation.SuppressLint
import dagger.hilt.android.HiltAndroidApp
import io.github.sds100.keymapper.base.BaseKeyMapperApp

@SuppressLint("LogNotTimber")
@HiltAndroidApp
class KeyMapperApp : BaseKeyMapperApp() {
    override fun getMainActivityClass(): Class<*> = MainActivity::class.java
}
