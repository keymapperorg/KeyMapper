package io.github.sds100.keymapper

import io.github.sds100.keymapper.Utils.ErrorCode
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_APP_DISABLED
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_APP_UNINSTALLED
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_FEATURE_NOT_AVAILABLE
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_PERMISSION_DENIED
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_SDK_VERSION_TOO_LOW
import io.github.sds100.keymapper.Utils.ErrorCodeUtils.ERROR_CODE_SHORTCUT_NOT_FOUND

/**
 * Created by sds100 on 25/11/2018.
 */
class ErrorResult(
        @ErrorCode val errorCode: Int,
        /**
         * Any extra information relating to the error code. E.g the cause
         *
         * [ERROR_CODE_PERMISSION_DENIED]: the id of the denied permission.
         *
         * [ERROR_CODE_APP_DISABLED], [ERROR_CODE_APP_UNINSTALLED], [ERROR_CODE_SHORTCUT_NOT_FOUND]:
         * the package name of the app
         *
         * [ERROR_CODE_SDK_VERSION_TOO_LOW]: The version code.
         *
         * [ERROR_CODE_FEATURE_NOT_AVAILABLE]: The feature id.
         */
        val data: String? = null
)