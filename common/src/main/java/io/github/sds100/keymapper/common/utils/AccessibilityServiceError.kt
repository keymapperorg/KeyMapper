package io.github.sds100.keymapper.common.utils

sealed class AccessibilityServiceError : KMError() {
    data object Disabled : AccessibilityServiceError()
    data object Crashed : AccessibilityServiceError()
}
