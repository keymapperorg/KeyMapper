package io.github.sds100.keymapper.base.actions

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import kotlinx.coroutines.flow.Flow

interface DisplayActionUseCase : GetActionErrorUseCase {
    val showDeviceDescriptors: Flow<Boolean>

    fun getAppName(packageName: String): KMResult<String>

    fun getAppIcon(packageName: String): KMResult<Drawable>

    fun getInputMethodLabel(imeId: String): KMResult<String>

    fun getRingtoneLabel(uri: String): KMResult<String>

    suspend fun fixError(error: KMError)

    fun neverShowDndTriggerError()

    fun startAccessibilityService(): Boolean

    fun restartAccessibilityService(): Boolean
}
