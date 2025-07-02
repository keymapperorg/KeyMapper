package io.github.sds100.keymapper.base.constraints

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult

interface DisplayConstraintUseCase : GetConstraintErrorUseCase {
    fun getAppName(packageName: String): KMResult<String>
    fun getAppIcon(packageName: String): KMResult<Drawable>
    fun getInputMethodLabel(imeId: String): KMResult<String>
    fun neverShowDndTriggerError()
    suspend fun fixError(error: KMError)
}
