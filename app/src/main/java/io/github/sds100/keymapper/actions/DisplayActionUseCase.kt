package io.github.sds100.keymapper.actions

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import kotlinx.coroutines.flow.Flow

interface DisplayActionUseCase : GetActionErrorUseCase {
    val showDeviceDescriptors: Flow<Boolean>
    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
    fun getInputMethodLabel(imeId: String): Result<String>
    fun getRingtoneLabel(uri: String): Result<String>
    suspend fun fixError(error: Error)
    fun neverShowDndTriggerError()
    fun startAccessibilityService(): Boolean
    fun restartAccessibilityService(): Boolean
}
