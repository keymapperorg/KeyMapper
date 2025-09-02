package io.github.sds100.keymapper.common.utils

import java.util.UUID
import javax.inject.Inject

class DefaultUuidGenerator @Inject constructor() : UuidGenerator {
    override fun random(): String = UUID.randomUUID().toString()
}

interface UuidGenerator {
    fun random(): String
}
