package io.github.sds100.keymapper.system

import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider

sealed class SystemError : Error() {
    data class PermissionDenied(val permission: Permission) : Error() 
    data class ImeDisabled(val ime: ImeInfo) : Error()
}
