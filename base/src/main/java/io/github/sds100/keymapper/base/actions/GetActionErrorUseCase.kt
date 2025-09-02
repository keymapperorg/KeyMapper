package io.github.sds100.keymapper.base.actions

import io.github.sds100.keymapper.base.actions.sound.SoundsManager
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import io.github.sds100.keymapper.system.ringtones.RingtoneAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetActionErrorUseCaseImpl @Inject constructor(
    private val packageManagerAdapter: PackageManagerAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val systemFeatureAdapter: SystemFeatureAdapter,
    private val cameraAdapter: CameraAdapter,
    private val soundsManager: SoundsManager,
    private val shizukuAdapter: ShizukuAdapter,
    private val ringtoneAdapter: RingtoneAdapter,
    private val buildConfigProvider: BuildConfigProvider,
) : GetActionErrorUseCase {

    private val invalidateActionErrors = merge(
        inputMethodAdapter.chosenIme.drop(1).map { },
        // invalidate when the input methods change
        inputMethodAdapter.inputMethods.drop(1).map { },
        permissionAdapter.onPermissionsUpdate,
        soundsManager.soundFiles.drop(1).map { },
        shizukuAdapter.isStarted.drop(1).map { },
        shizukuAdapter.isInstalled.drop(1).map { },
        packageManagerAdapter.onPackagesChanged,
    )

    override val actionErrorSnapshot: Flow<ActionErrorSnapshot> = channelFlow {
        send(createSnapshot())

        invalidateActionErrors.collectLatest {
            send(createSnapshot())
        }
    }

    private fun createSnapshot(): ActionErrorSnapshot = LazyActionErrorSnapshot(
        packageManagerAdapter,
        inputMethodAdapter,
        permissionAdapter,
        systemFeatureAdapter,
        cameraAdapter,
        soundsManager,
        shizukuAdapter,
        ringtoneAdapter,
        buildConfigProvider,
    )
}

interface GetActionErrorUseCase {
    val actionErrorSnapshot: Flow<ActionErrorSnapshot>
}
