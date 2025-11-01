package io.github.sds100.keymapper.base.system.permissions

import android.Manifest
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import timber.log.Timber

@Singleton
class AutoGrantPermissionController @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val permissionAdapter: PermissionAdapter,
    private val shizukuAdapter: ShizukuAdapter,
) {

    fun start() {
        // automatically grant WRITE_SECURE_SETTINGS if Key Mapper has root or shizuku permission
        permissionAdapter.isGrantedFlow(Permission.WRITE_SECURE_SETTINGS)
            .flatMapLatest { isGranted ->
                if (isGranted) {
                    emptyFlow()
                } else {
                    combine(
                        permissionAdapter.isGrantedFlow(Permission.ROOT),
                        permissionAdapter.isGrantedFlow(Permission.SHIZUKU),
                        shizukuAdapter.isStarted,
                    ) { isRootGranted, isShizukuGranted, isShizukuStarted ->

                        if (isRootGranted || (isShizukuGranted && isShizukuStarted)) {
                            Timber.i("Auto-granting WRITE_SECURE_SETTINGS permission")
                            permissionAdapter.grant(Manifest.permission.WRITE_SECURE_SETTINGS)
                        }
                    }
                }
            }.launchIn(coroutineScope)
    }
}
