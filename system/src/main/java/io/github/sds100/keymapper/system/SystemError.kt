package io.github.sds100.keymapper.system

import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.permissions.Permission

sealed class SystemError : Error() {
    data class PermissionDenied(val permission: Permission) : Error() 
    data class ImeDisabled(val ime: ImeInfo) : Error()
}
