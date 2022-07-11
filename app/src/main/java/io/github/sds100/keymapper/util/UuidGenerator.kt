package io.github.sds100.keymapper.util

import java.util.*
import javax.inject.Inject

/**
 * Created by sds100 on 29/06/2021.
 */

class DefaultUuidGenerator @Inject constructor(): UuidGenerator {
    override fun random(): String {
        return UUID.randomUUID().toString()
    }
}

interface UuidGenerator {
    fun random(): String
}