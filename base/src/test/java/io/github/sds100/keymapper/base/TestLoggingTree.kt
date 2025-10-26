package io.github.sds100.keymapper.base

import timber.log.Timber

class TestLoggingTree : Timber.Tree() {
    override fun log(
        priority: Int,
        tag: String?,
        message: String,
        t: Throwable?,
    ) {
        t?.printStackTrace()
        println(message)
    }
}
