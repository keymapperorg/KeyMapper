package io.github.sds100.keymapper

import android.content.Context
import io.github.sds100.keymapper.util.ErrorCode
import io.github.sds100.keymapper.util.ErrorCodeUtils
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_APP_DISABLED
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_APP_UNINSTALLED
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_FEATURE_NOT_AVAILABLE
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_FLAG_NOT_FOUND
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_NULL
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_PERMISSION_DENIED
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_SDK_VERSION_TOO_LOW
import io.github.sds100.keymapper.util.ErrorCodeUtils.ERROR_CODE_SHORTCUT_NOT_FOUND

/**
 * Created by sds100 on 25/11/2018.
 */
class ErrorResult(
        @ErrorCode val errorCode: Int = ERROR_CODE_NULL,
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
         *
         * [ERROR_CODE_FLAG_NOT_FOUND]: The flag id.
         */
        val data: String? = null
)

/**
 * @return The [ErrorResult] isn't null and it can be fixed.
 */
val ErrorResult?.isFixable: Boolean
    get() = this != null && ErrorCodeUtils.isErrorFixable(errorCode)

/**
 * @see ErrorCodeUtils.fixError
 */
fun ErrorResult.fix(ctx: Context) = ErrorCodeUtils.fixError(ctx, this)