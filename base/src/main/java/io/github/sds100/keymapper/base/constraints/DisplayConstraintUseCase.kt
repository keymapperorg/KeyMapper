package io.github.sds100.keymapper.base.constraints

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.common.util.result.Error
import io.github.sds100.keymapper.common.util.result.Result

interface DisplayConstraintUseCase : GetConstraintErrorUseCase {
    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
    fun getInputMethodLabel(imeId: String): Result<String>
    fun neverShowDndTriggerError()
    suspend fun fixError(error: Error)
}
