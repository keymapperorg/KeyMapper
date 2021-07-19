package io.github.sds100.keymapper.mappings

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.actions.GetActionErrorUseCase
import io.github.sds100.keymapper.constraints.GetConstraintErrorUseCase
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 03/04/2021.
 */

class DisplaySimpleMappingUseCaseImpl(
    private val packageManager: PackageManagerAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val serviceAdapter: ServiceAdapter,
    private val preferenceRepository: PreferenceRepository,
    getActionError: GetActionErrorUseCase,
    getConstraintError: GetConstraintErrorUseCase
) : DisplaySimpleMappingUseCase, GetActionErrorUseCase by getActionError,
    GetConstraintErrorUseCase by getConstraintError {

    override val showDeviceDescriptors: Flow<Boolean> =
        preferenceRepository.get(Keys.showDeviceDescriptors).map { it ?: false }

    private val keyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    override fun getAppName(packageName: String): Result<String> =
        packageManager.getAppName(packageName)

    override fun getAppIcon(packageName: String): Result<Drawable> =
        packageManager.getAppIcon(packageName)

    override fun getInputMethodLabel(imeId: String): Result<String> =
        inputMethodAdapter.getInfoById(imeId).then { Success(it.label) }

    override suspend fun fixError(error: Error) {
        when (error) {
            Error.AccessibilityServiceDisabled -> serviceAdapter.enableService()
            Error.AccessibilityServiceCrashed -> serviceAdapter.restartService()
            is Error.AppDisabled -> packageManager.enableApp(error.packageName)
            is Error.AppNotFound -> packageManager.installApp(error.packageName)
            Error.NoCompatibleImeChosen -> keyMapperImeHelper.chooseCompatibleInputMethod()
                .otherwise {
                    inputMethodAdapter.showImePicker(fromForeground = true)
                }
            Error.NoCompatibleImeEnabled -> keyMapperImeHelper.enableCompatibleInputMethods()
            is Error.ImeDisabled -> inputMethodAdapter.enableIme(error.ime.id)
            is Error.PermissionDenied -> permissionAdapter.request(error.permission)
        }
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
}

interface DisplayConstraintUseCase : GetConstraintErrorUseCase {
    fun getAppName(packageName: String): Result<String>
    fun getAppIcon(packageName: String): Result<Drawable>
    fun getInputMethodLabel(imeId: String): Result<String>
    suspend fun fixError(error: Error)
}