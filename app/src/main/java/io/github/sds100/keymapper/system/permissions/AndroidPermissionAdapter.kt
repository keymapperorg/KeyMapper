package io.github.sds100.keymapper.system.permissions

import android.Manifest
import android.app.NotificationManager
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.IPackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.PowerManager
import android.os.Process
import android.permission.IPermissionManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.shizuku.ShizukuUtils
import io.github.sds100.keymapper.system.DeviceAdmin
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.getIdentifier
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.success
import io.github.sds100.keymapper.util.then
import io.github.sds100.keymapper.util.valueIfFailure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber

/**
 * Created by sds100 on 17/03/2021.
 */
class AndroidPermissionAdapter(
    context: Context,
    private val coroutineScope: CoroutineScope,
    private val suAdapter: SuAdapter,
    private val notificationReceiverAdapter: ServiceAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val packageManagerAdapter: PackageManagerAdapter,
) : PermissionAdapter {
    companion object {
        const val REQUEST_CODE_SHIZUKU_PERMISSION = 1
    }

    private val shizukuPackageManager: IPackageManager by lazy {
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        IPackageManager.Stub.asInterface(binder)
    }

    private val shizukuPermissionManager: IPermissionManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/permission",
            )
        }
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("permissionmgr"))
        IPermissionManager.Stub.asInterface(binder)
    }

    private val ctx = context.applicationContext

    override val onPermissionsUpdate: MutableSharedFlow<Unit> = MutableSharedFlow()

    private val _request = MutableSharedFlow<Permission>()
    val request = _request.asSharedFlow()

    /**
     * On some devices the Do Not Disturb settings do not exist even though
     * they are on Marshmallow+. A dialog is shown when they request this
     * permission that it may not exist and they have the option to never show
     * the dialog again. This value is true when they clicked to never
     * show it again.
     */
    private val neverRequestDndPermission: StateFlow<Boolean> =
        preferenceRepository.get(Keys.neverShowDndAccessError)
            .map { it == true }
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    init {
        suAdapter.isGranted
            .drop(1)
            .onEach { onPermissionsChanged() }
            .launchIn(coroutineScope)

        Shizuku.addRequestPermissionResultListener { requestCode, grantResult ->
            coroutineScope.launch {
                if (requestCode == REQUEST_CODE_SHIZUKU_PERMISSION) {
                    onPermissionsChanged()
                }
            }
        }

        notificationReceiverAdapter.state
            .drop(1)
            .onEach { onPermissionsChanged() }
            .launchIn(coroutineScope)

        // whenever the setting to never show dnd permission changes
        // update whether permissions are granted.
        neverRequestDndPermission
            .onEach { onPermissionsChanged() }
            .launchIn(coroutineScope)
    }

    override fun request(permission: Permission) {
        coroutineScope.launch { _request.emit(permission) }
    }

    override fun grant(permissionName: String): Result<*> {
        val result: Result<*>

        if (ContextCompat.checkSelfPermission(
                ctx,
                permissionName,
            ) == PERMISSION_GRANTED
        ) { // if already granted
            return success()
        }

        if (isGranted(Permission.SHIZUKU)) {
            result = try {
                grantPermissionWithShizuku(permissionName)

                // if successfully granted
                if (ContextCompat.checkSelfPermission(ctx, permissionName) == PERMISSION_GRANTED) {
                    success()
                } else {
                    Error.Exception(Exception("Failed to grant permission with Shizuku."))
                }
            } catch (e: Exception) {
                Error.Exception(e)
            }
        } else if (isGranted(Permission.ROOT)) {
            suAdapter.execute(
                "pm grant ${Constants.PACKAGE_NAME} $permissionName",
                block = true,
            )
            if (ContextCompat.checkSelfPermission(ctx, permissionName) == PERMISSION_GRANTED) {
                result = success()
            } else {
                result =
                    Error.Exception(Exception("Failed to grant permission with root. Key Mapper may not actually have root permission."))
            }
        } else {
            result = Error.PermissionDenied(Permission.SHIZUKU)
        }

        result.onSuccess {
            onPermissionsChanged()
        }.onFailure {
            Timber.e("Error granting permission: $it")
        }

        return result
    }

    private fun grantPermissionWithShizuku(permissionName: String) {
        val userId = Process.myUserHandle()!!.getIdentifier()

        try {
            // In revisions of Android 14 the method to grant permissions changed
            // so try them all.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                try {
                    shizukuPermissionManager.grantRuntimePermission(
                        Constants.PACKAGE_NAME,
                        permissionName,
                        ctx.deviceId,
                        userId,
                    )
                } catch (_: NoSuchMethodError) {
                    try {
                        shizukuPermissionManager.grantRuntimePermission(
                            Constants.PACKAGE_NAME,
                            permissionName,
                            "0",
                            userId,
                        )
                    } catch (_: NoSuchMethodError) {
                        shizukuPermissionManager.grantRuntimePermission(
                            Constants.PACKAGE_NAME,
                            permissionName,
                            userId,
                        )
                    }
                }
                // In Android 11 this method was moved from IPackageManager to IPermissionManager.
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                shizukuPermissionManager.grantRuntimePermission(
                    Constants.PACKAGE_NAME,
                    permissionName,
                    userId,
                )
            } else {
                shizukuPackageManager.grantRuntimePermission(
                    Constants.PACKAGE_NAME,
                    permissionName,
                    userId,
                )
            }
            // The API may change in future Android versions so don't crash the whole app
            // just for this shizuku permission feature.
        } catch (_: NoSuchMethodError) {
        }
    }

    override fun isGranted(permission: Permission): Boolean = when (permission) {
        Permission.WRITE_SETTINGS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.System.canWrite(ctx)
            } else {
                true
            }

        Permission.CAMERA ->
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.CAMERA,
            ) == PERMISSION_GRANTED

        Permission.DEVICE_ADMIN -> {
            val devicePolicyManager: DevicePolicyManager = ctx.getSystemService()!!
            devicePolicyManager.isAdminActive(ComponentName(ctx, DeviceAdmin::class.java))
        }

        Permission.READ_PHONE_STATE ->
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.READ_PHONE_STATE,
            ) == PERMISSION_GRANTED

        Permission.ACCESS_NOTIFICATION_POLICY ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !neverRequestDndPermission.value) {
                val notificationManager: NotificationManager = ctx.getSystemService()!!
                notificationManager.isNotificationPolicyAccessGranted
            } else {
                true
            }

        Permission.WRITE_SECURE_SETTINGS -> {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.WRITE_SECURE_SETTINGS,
            ) == PERMISSION_GRANTED
        }

        Permission.NOTIFICATION_LISTENER ->
            NotificationManagerCompat.getEnabledListenerPackages(ctx)
                .contains(Constants.PACKAGE_NAME)

        Permission.CALL_PHONE ->
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.CALL_PHONE,
            ) == PERMISSION_GRANTED

        Permission.ROOT -> suAdapter.isGranted.value

        Permission.IGNORE_BATTERY_OPTIMISATION ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val powerManager = ctx.getSystemService<PowerManager>()

                val ignoringOptimisations =
                    powerManager?.isIgnoringBatteryOptimizations(Constants.PACKAGE_NAME)

                when {
                    powerManager == null -> Timber.i("Power manager is null")
                    ignoringOptimisations == true -> Timber.i("Battery optimisation is disabled")
                    ignoringOptimisations == false -> Timber.e("Battery optimisation is enabled")
                }

                ignoringOptimisations ?: false
            } else {
                true
            }

        // this check is super quick (~0ms) so this doesn't need to be cached.
        Permission.SHIZUKU -> {
            if (ShizukuUtils.isSupportedForSdkVersion() && Shizuku.getBinder() != null) {
                Shizuku.checkSelfPermission() == PERMISSION_GRANTED
            } else {
                false
            }
        }

        Permission.ACCESS_FINE_LOCATION ->
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PERMISSION_GRANTED

        Permission.ANSWER_PHONE_CALL ->
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ANSWER_PHONE_CALLS,
            ) == PERMISSION_GRANTED

        Permission.FIND_NEARBY_DEVICES ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ) == PERMISSION_GRANTED
            } else {
                true
            }

        Permission.POST_NOTIFICATIONS ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    ctx,
                    Manifest.permission.POST_NOTIFICATIONS,
                ) == PERMISSION_GRANTED
            } else {
                true
            }

        Permission.DEVICE_ASSISTANT ->
            packageManagerAdapter.getDeviceAssistantPackage()
                .then { Success(it == Constants.PACKAGE_NAME) }
                .valueIfFailure { false }
    }

    override fun isGrantedFlow(permission: Permission): Flow<Boolean> = callbackFlow {
        send(isGranted(permission))

        onPermissionsUpdate.collect {
            send(isGranted(permission))
        }
    }

    fun onPermissionsChanged() {
        coroutineScope.launch {
            onPermissionsUpdate.emit(Unit)
        }
    }
}
