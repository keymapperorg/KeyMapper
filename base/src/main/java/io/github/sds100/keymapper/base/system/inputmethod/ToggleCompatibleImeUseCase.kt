package io.github.sds100.keymapper.base.system.inputmethod

import android.os.Build
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceState
import io.github.sds100.keymapper.system.inputmethod.ImeInfo
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ToggleCompatibleImeUseCaseImpl @Inject constructor(
    private val inputMethodAdapter: InputMethodAdapter,
    private val buildConfigProvider: BuildConfigProvider,
    private val switchImeInterface: SwitchImeInterface,
    private val serviceAdapter: AccessibilityServiceAdapter,
    private val permissionAdapter: PermissionAdapter,
) : ToggleCompatibleImeUseCase {
    private val keyMapperImeHelper =
        KeyMapperImeHelper(switchImeInterface, inputMethodAdapter, buildConfigProvider.packageName)

    override val sufficientPermissions: Flow<Boolean> = channelFlow {
        suspend fun invalidate() {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                    serviceAdapter.state.first() == AccessibilityServiceState.ENABLED -> send(true)

                permissionAdapter.isGranted(Permission.WRITE_SECURE_SETTINGS) -> send(true)

                else -> send(false)
            }
        }

        invalidate()

        launch {
            permissionAdapter.onPermissionsUpdate.collectLatest {
                invalidate()
            }
        }

        launch {
            serviceAdapter.state.collectLatest {
                invalidate()
            }
        }
    }

    override suspend fun toggle(): KMResult<ImeInfo> =
        keyMapperImeHelper.toggleCompatibleInputMethod().then { inputMethodAdapter.getInfoById(it) }
}

interface ToggleCompatibleImeUseCase {
    val sufficientPermissions: Flow<Boolean>

    suspend fun toggle(): KMResult<ImeInfo>
}
