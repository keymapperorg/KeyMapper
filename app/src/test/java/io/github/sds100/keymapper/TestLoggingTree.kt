package io.github.sds100.keymapper

import timber.log.Timber

/**
 * Created by sds100 on 25/06/2021.
 */
class TestLoggingTree : Timber.Tree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        t?.printStackTrace()
        println(message)
    }
}
