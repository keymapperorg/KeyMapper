package io.github.sds100.keymapper.system.permissions

import android.Manifest
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

/**
 * Created by sds100 on 12/09/2021.
 */
class AutoGrantPermissionController(
    private val coroutineScope: CoroutineScope,
    private val permissionAdapter: PermissionAdapter,
    private val popupAdapter: PopupMessageAdapter,
    private val resourceProvider: ResourceProvider
) {

    fun start() {
        //automatically grant WRITE_SECURE_SETTINGS if Key Mapper has root or shizuku permission
        combine(
            permissionAdapter.isGrantedFlow(Permission.ROOT),
            permissionAdapter.isGrantedFlow(Permission.SHIZUKU),
            permissionAdapter.isGrantedFlow(Permission.WRITE_SECURE_SETTINGS)
        ) { isRootGranted, isShizukuGranted, isWriteSecureSettingsGranted ->

            if (!isWriteSecureSettingsGranted && (isRootGranted || isShizukuGranted)) {
                permissionAdapter.grant(Manifest.permission.WRITE_SECURE_SETTINGS).onSuccess {
                    val stringRes = if (isRootGranted) {
                        R.string.toast_granted_itself_write_secure_settings_with_root
                    } else {
                        R.string.toast_granted_itself_write_secure_settings_with_shizuku
                    }

                    popupAdapter.showPopupMessage(resourceProvider.getString(stringRes))
                }
            }
        }.launchIn(coroutineScope)
    }
}