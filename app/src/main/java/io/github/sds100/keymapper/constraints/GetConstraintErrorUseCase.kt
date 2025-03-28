package io.github.sds100.keymapper.constraints

import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.permissions.SystemFeatureAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

class GetConstraintErrorUseCaseImpl(
    private val packageManager: PackageManagerAdapter,
    private val permissionAdapter: PermissionAdapter,
    private val systemFeatureAdapter: SystemFeatureAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val cameraAdapter: CameraAdapter,
) : GetConstraintErrorUseCase {
    private val invalidateConstraintErrors = merge(
        permissionAdapter.onPermissionsUpdate,
        inputMethodAdapter.inputMethods.drop(1).map { },
        packageManager.onPackagesChanged,
    )

    override val constraintErrorSnapshot: Flow<ConstraintErrorSnapshot> = channelFlow {
        send(createSnapshot())

        invalidateConstraintErrors.collectLatest {
            send(createSnapshot())
        }
    }

    private fun createSnapshot(): ConstraintErrorSnapshot = LazyConstraintErrorSnapshot(
        packageManager,
        permissionAdapter,
        systemFeatureAdapter,
        inputMethodAdapter,
        cameraAdapter,
    )
}

interface GetConstraintErrorUseCase {
    val constraintErrorSnapshot: Flow<ConstraintErrorSnapshot>
}
