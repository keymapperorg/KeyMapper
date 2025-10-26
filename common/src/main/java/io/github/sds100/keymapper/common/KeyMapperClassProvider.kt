package io.github.sds100.keymapper.common

interface KeyMapperClassProvider {
    fun getMainActivity(): Class<*>

    fun getAccessibilityService(): Class<*>
}
