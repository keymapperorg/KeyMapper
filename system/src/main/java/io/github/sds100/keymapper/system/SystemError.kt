package io.github.sds100.keymapper.system

import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.permissions.Permission

sealed class SystemError : KMError() {
    data class PermissionDenied(val permission: Permission) : KMError()
    data class ImeDisabled(val ime: ImeInfo) : KMError()
}
