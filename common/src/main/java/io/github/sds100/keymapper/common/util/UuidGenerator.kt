package io.github.sds100.keymapper.common.util

import java.util.UUID



class DefaultUuidGenerator : UuidGenerator {
    override fun random(): String = UUID.randomUUID().toString()
}

interface UuidGenerator {
    fun random(): String
}
