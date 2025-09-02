package io.github.sds100.keymapper.base.system.permissions

import android.Manifest
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.popup.ToastAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import javax.inject.Inject

class AutoGrantPermissionController @Inject constructor(
    private val coroutineScope: CoroutineScope,
    private val permissionAdapter: PermissionAdapter,
    private val popupAdapter: ToastAdapter,
    private val resourceProvider: ResourceProvider,
) {

    fun start() {
        // automatically grant WRITE_SECURE_SETTINGS if Key Mapper has root or shizuku permission
        combine(
            permissionAdapter.isGrantedFlow(Permission.ROOT),
            permissionAdapter.isGrantedFlow(Permission.SHIZUKU),
            permissionAdapter.isGrantedFlow(Permission.WRITE_SECURE_SETTINGS),
        ) { isRootGranted, isShizukuGranted, isWriteSecureSettingsGranted ->

            if (!isWriteSecureSettingsGranted && (isRootGranted || isShizukuGranted)) {
                permissionAdapter.grant(Manifest.permission.WRITE_SECURE_SETTINGS).onSuccess {
                    val stringRes = if (isRootGranted) {
                        R.string.toast_granted_itself_write_secure_settings_with_root
                    } else {
                        R.string.toast_granted_itself_write_secure_settings_with_shizuku
                    }

                    popupAdapter.show(resourceProvider.getString(stringRes))
                }
            }
        }.launchIn(coroutineScope)
    }
}
