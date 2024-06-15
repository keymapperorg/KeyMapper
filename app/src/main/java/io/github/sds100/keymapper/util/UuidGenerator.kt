package io.github.sds100.keymapper.util

import java.util.UUID

/**
 * Created by sds100 on 29/06/2021.
 */

class DefaultUuidGenerator : UuidGenerator {
    override fun random(): String = UUID.randomUUID().toString()
}

interface UuidGenerator {
    fun random(): String
}
