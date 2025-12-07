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
import android.permission.PermissionManagerApis
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.Constants
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.firstBlocking
import io.github.sds100.keymapper.common.utils.getIdentifier
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.success
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.isConnected
import io.github.sds100.keymapper.sysbridge.utils.SystemBridgeError
import io.github.sds100.keymapper.system.DeviceAdmin
import io.github.sds100.keymapper.system.notifications.NotificationReceiverAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuAdapter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper
import timber.log.Timber

@Singleton
class AndroidPermissionAdapter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val suAdapter: SuAdapter,
    private val notificationReceiverAdapter: NotificationReceiverAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val buildConfigProvider: BuildConfigProvider,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val shizukuAdapter: ShizukuAdapter,
) : PermissionAdapter {
    companion object {
        const val REQUEST_CODE_SHIZUKU_PERMISSION = 1
    }

    private val shizukuPermissionManager: IPermissionManager? by lazy {
        // Use IPackageManager instead on older versions
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return@lazy null
        }

        HiddenApiBypass.addHiddenApiExemptions(
            "Landroid/permission",
        )
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("permissionmgr"))
        IPermissionManager.Stub.asInterface(binder)
    }

    private val shizukuPackageManager: IPackageManager? by lazy {
        val binder = ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        IPackageManager.Stub.asInterface(binder)
    }

    private val ctx = context.applicationContext

    private val powerManager: PowerManager? = ctx.getSystemService<PowerManager>()

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
        suAdapter.isRootGranted
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

        notificationReceiverAdapter.isEnabled
            .drop(1)
            .onEach { onPermissionsChanged() }
            .launchIn(coroutineScope)

        // whenever the setting to never show dnd permission changes
        // update whether permissions are granted.
        neverRequestDndPermission
            .drop(1)
            .onEach { onPermissionsChanged() }
            .launchIn(coroutineScope)
    }

    override fun request(permission: Permission) {
        coroutineScope.launch { _request.emit(permission) }
    }

    override fun grant(permissionName: String): KMResult<*> {
        val result: KMResult<*>

        if (ContextCompat.checkSelfPermission(
                ctx,
                permissionName,
            ) == PERMISSION_GRANTED
        ) { // if already granted
            return success()
        }

        val deviceId: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ctx.deviceId
        } else {
            -1
        }

        val isSystemBridgeConnected =
            Build.VERSION.SDK_INT >= Constants.SYSTEM_BRIDGE_MIN_API &&
                systemBridgeConnectionManager.isConnected()

        if (isSystemBridgeConnected) {
            result = systemBridgeConnectionManager.run { bridge ->
                bridge.grantPermission(permissionName, deviceId)
            }.then {
                if (ContextCompat.checkSelfPermission(ctx, permissionName) == PERMISSION_GRANTED) {
                    success()
                } else {
                    KMError.Exception(Exception("Failed to grant permission with system bridge"))
                }
            }
        } else if (shizukuAdapter.isStarted.value && isGranted(Permission.SHIZUKU)) {
            val userId = Process.myUserHandle()!!.getIdentifier()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                PermissionManagerApis.grantPermission(
                    shizukuPermissionManager!!,
                    buildConfigProvider.packageName,
                    permissionName,
                    deviceId,
                    userId,
                )
            } else {
                PermissionManagerApis.grantPermission(
                    shizukuPackageManager!!,
                    buildConfigProvider.packageName,
                    permissionName,
                    userId,
                )
            }

            if (ContextCompat.checkSelfPermission(ctx, permissionName) == PERMISSION_GRANTED) {
                result = success()
            } else {
                result =
                    KMError.Exception(Exception("Failed to grant permission with Shizuku."))
            }
        } else if (isGranted(Permission.ROOT)) {
            runBlocking {
                suAdapter.execute(
                    "pm grant ${buildConfigProvider.packageName} $permissionName",
                )
            }

            if (ContextCompat.checkSelfPermission(ctx, permissionName) == PERMISSION_GRANTED) {
                result = success()
            } else {
                result =
                    KMError.Exception(Exception("Failed to grant permission with root."))
            }
        } else {
            // The system bridge should be the default way to grant permissions.
            result = SystemBridgeError.Disconnected
        }

        result.onSuccess {
            onPermissionsChanged()
        }.onFailure {
            Timber.e("Error granting permission: $it")
        }

        return result
    }

    override fun isGranted(permission: Permission): Boolean = when (permission) {
        Permission.WRITE_SETTINGS -> Settings.System.canWrite(ctx)

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
            if (neverRequestDndPermission.value) {
                true
            } else {
                val notificationManager: NotificationManager = ctx.getSystemService()!!
                notificationManager.isNotificationPolicyAccessGranted
            }

        Permission.WRITE_SECURE_SETTINGS -> {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.WRITE_SECURE_SETTINGS,
            ) == PERMISSION_GRANTED
        }

        Permission.NOTIFICATION_LISTENER ->
            NotificationManagerCompat.getEnabledListenerPackages(ctx)
                .contains(buildConfigProvider.packageName)

        Permission.CALL_PHONE ->
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.CALL_PHONE,
            ) == PERMISSION_GRANTED

        Permission.SEND_SMS ->
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.SEND_SMS,
            ) == PERMISSION_GRANTED

        Permission.ROOT -> suAdapter.isRootGranted.firstBlocking()

        Permission.IGNORE_BATTERY_OPTIMISATION ->
            powerManager?.isIgnoringBatteryOptimizations(buildConfigProvider.packageName) ?: false

        // this check is super quick (~0ms) so this doesn't need to be cached.
        Permission.SHIZUKU -> {
            if (Shizuku.getBinder() != null) {
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

        Permission.READ_LOGS -> {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.READ_LOGS,
            ) == PERMISSION_GRANTED
        }
    }

    override fun isGrantedFlow(permission: Permission): Flow<Boolean> = channelFlow {
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
