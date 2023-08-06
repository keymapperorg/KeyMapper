package io.github.sds100.keymapper.mappings

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.actions.GetActionErrorUseCase
import io.github.sds100.keymapper.constraints.GetConstraintErrorUseCase
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.shizuku.ShizukuUtils
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 03/04/2021.
 */

class DisplaySimpleMappingUseCaseImpl(
    private val packageManager: PackageManagerAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val accessibilityServiceAdapter: ServiceAdapter,
    getActionError: GetActionErrorUseCase,
    getConstraintError: GetConstraintErrorUseCase,
) : DisplaySimpleMappingUseCase,
    GetActionErrorUseCase by getActionError,
    GetConstraintErrorUseCase by getConstraintError {

    override val showDeviceDescriptors: Flow<Boolean> =
        preferenceRepository.get(Keys.showDeviceDescriptors).map { it ?: false }

    private val keyMapperImeHelper by lazy { KeyMapperImeHelper(inputMethodAdapter) }

    override fun getAppName(packageName: String): Result<String> =
        packageManager.getAppName(packageName)

    override fun getAppIcon(packageName: String): Result<Drawable> =
        packageManager.getAppIcon(packageName)

    override fun getInputMethodLabel(imeId: String): Result<String> =
        inputMethodAdapter.getInfoById(imeId).then { Success(it.label) }

    override suspend fun fixError(error: Error) {
        when (error) {
            is Error.AppDisabled -> packageManager.enableApp(error.packageName)
            is Error.AppNotFound -> packageManager.downloadApp(error.packageName)
            Error.NoCompatibleImeChosen ->
                keyMapperImeHelper.chooseCompatibleInputMethod().otherwise {
                    inputMethodAdapter.showImePicker(fromForeground = true)
                }

            Error.NoCompatibleImeEnabled -> keyMapperImeHelper.enableCompatibleInputMethods()
            is Error.ImeDisabled -> inputMethodAdapter.enableIme(error.ime.id)
            is Error.PermissionDenied -> permissionAdapter.request(error.permission)
            is Error.ShizukuNotStarted -> packageManager.openApp(ShizukuUtils.SHIZUKU_PACKAGE)
            is Error.CantDetectKeyEventsInPhoneCall -> {
                if (!keyMapperImeHelper.isCompatibleImeEnabled()) {
                    keyMapperImeHelper.enableCompatibleInputMethods()
                }

                //wait for compatible ime to be enabled then choose it.
                keyMapperImeHelper.isCompatibleImeEnabledFlow.first { it }

                keyMapperImeHelper.chooseCompatibleInputMethod().otherwise {
                    inputMethodAdapter.showImePicker(fromForeground = true)
                }
            }
            else -> Unit
        }
    }

    override fun startAccessibilityService(): Boolean {
        return accessibilityServiceAdapter.start()
    }

    override fun restartAccessibilityService(): Boolean {
        return accessibilityServiceAdapter.restart()
    }
}

interface DisplaySimpleMappingUseCase : DisplayActionUseCase, DisplayConstraintUseCase {
    override val showDeviceDescriptors: Flow<Boolean>
}

interface DisplayActionUseCase : GetActionErrorUseCase {
    val showDeviceDescriptors: Flow<Boolean>
    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
    fun getInputMethodLabel(imeId: String): Result<String>
    suspend fun fixError(error: Error)
    fun startAccessibilityService(): Boolean
    fun restartAccessibilityService(): Boolean
}

interface DisplayConstraintUseCase : GetConstraintErrorUseCase {
    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
    fun getInputMethodLabel(imeId: String): Result<String>
    suspend fun fixError(error: Error)
}