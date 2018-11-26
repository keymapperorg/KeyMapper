package io.github.sds100.keymapper

import androidx.annotation.IntDef
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_ACTION_IS_NULL
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_APP_DISABLED
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_APP_UNINSTALLED
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_NO_ACTION_DATA
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_PERMISSION_DENIED
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_SHORTCUT_NOT_FOUND

/**
 * Created by sds100 on 25/11/2018.
 */
class ErrorCodeResult(
        @ErrorCode val errorCode: Int,
        /**
         * Any extra information relating to the error code. E.g the cause
         *
         * [ERROR_CODE_PERMISSION_DENIED]: the id of the denied permission.
         *
         * [ERROR_CODE_APP_DISABLED], [ERROR_CODE_APP_UNINSTALLED], [ERROR_CODE_SHORTCUT_NOT_FOUND]:
         * the package name of the app
         */
        val data: String? = null
) {
    companion object {
        @IntDef(value = [
            ERROR_CODE_NO_ACTION_DATA,
            ERROR_CODE_ACTION_IS_NULL,
            ERROR_CODE_APP_DISABLED,
            ERROR_CODE_APP_UNINSTALLED,
            ERROR_CODE_PERMISSION_DENIED,
            ERROR_CODE_SHORTCUT_NOT_FOUND]
        )
        @Retention(AnnotationRetention.SOURCE)
        private annotation class ErrorCode
    }
}