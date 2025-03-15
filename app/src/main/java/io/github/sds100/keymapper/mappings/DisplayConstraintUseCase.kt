package io.github.sds100.keymapper.mappings

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.constraints.GetConstraintErrorUseCase
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result

interface DisplayConstraintUseCase : GetConstraintErrorUseCase {
    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
    fun getInputMethodLabel(imeId: String): Result<String>
    fun neverShowDndTriggerError()
    suspend fun fixError(error: Error)
}
