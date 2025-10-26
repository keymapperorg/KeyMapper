package io.github.sds100.keymapper.base.keymaps

import android.graphics.drawable.Drawable
import android.os.Build
import dagger.hilt.android.scopes.ViewModelScoped
import io.github.sds100.keymapper.base.actions.DisplayActionUseCase
import io.github.sds100.keymapper.base.actions.GetActionErrorUseCase
import io.github.sds100.keymapper.base.constraints.DisplayConstraintUseCase
import io.github.sds100.keymapper.base.constraints.GetConstraintErrorUseCase
import io.github.sds100.keymapper.base.input.EvdevHandleCache
import io.github.sds100.keymapper.base.purchasing.ProductId
import io.github.sds100.keymapper.base.purchasing.PurchasingError.ProductNotPurchased
import io.github.sds100.keymapper.base.purchasing.PurchasingManager
import io.github.sds100.keymapper.base.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.base.system.inputmethod.SwitchImeInterface
import io.github.sds100.keymapper.base.trigger.TriggerError
import io.github.sds100.keymapper.base.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.models.EvdevDeviceInfo
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.KMResult
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.common.utils.otherwise
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.common.utils.valueIfFailure
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionManager
import io.github.sds100.keymapper.sysbridge.manager.SystemBridgeConnectionState
import io.github.sds100.keymapper.sysbridge.utils.SystemBridgeError
import io.github.sds100.keymapper.system.SystemError.ImeDisabled
import io.github.sds100.keymapper.system.SystemError.PermissionDenied
import io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.ringtones.RingtoneAdapter
import io.github.sds100.keymapper.system.shizuku.ShizukuUtils
import javax.inject.Inject
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeout

@ViewModelScoped
class DisplayKeyMapUseCaseImpl @Inject constructor(
    private val permissionAdapter: PermissionAdapter,
    private val switchImeInterface: SwitchImeInterface,
    private val inputMethodAdapter: InputMethodAdapter,
    private val packageManagerAdapter: PackageManagerAdapter,
    private val settingsRepository: PreferenceRepository,
    private val accessibilityServiceAdapter: AccessibilityServiceAdapter,
    private val purchasingManager: PurchasingManager,
    private val ringtoneAdapter: RingtoneAdapter,
    private val getActionErrorUseCase: GetActionErrorUseCase,
    private val getConstraintErrorUseCase: GetConstraintErrorUseCase,
    private val buildConfigProvider: BuildConfigProvider,
    private val navigationProvider: NavigationProvider,
    private val systemBridgeConnectionManager: SystemBridgeConnectionManager,
    private val evdevHandleCache: EvdevHandleCache,
) : DisplayKeyMapUseCase,
    GetActionErrorUseCase by getActionErrorUseCase,
    GetConstraintErrorUseCase by getConstraintErrorUseCase {
    private val keyMapperImeHelper =
        KeyMapperImeHelper(switchImeInterface, inputMethodAdapter, buildConfigProvider.packageName)

    private val showDpadImeSetupError: Flow<Boolean> =
        settingsRepository.get(Keys.neverShowDpadImeTriggerError).map { neverShow ->
            if (neverShow == null) {
                true
            } else {
                !neverShow
            }
        }

    /**
     * This waits for the purchases to be processed with a timeout so the UI doesn't
     * say there are no purchases while it is loading.
     */
    private val purchasesFlow: Flow<State<KMResult<Set<ProductId>>>> = callbackFlow {
        try {
            val value = withTimeout(5000L) {
                purchasingManager.purchases.filterIsInstance<State.Data<KMResult<Set<ProductId>>>>()
                    .first()
            }

            send(value)
        } catch (_: TimeoutCancellationException) {
        }

        purchasingManager.purchases.collect(this::send)
    }

    private val systemBridgeConnectionState: Flow<SystemBridgeConnectionState?> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            systemBridgeConnectionManager.connectionState
        } else {
            flowOf(null)
        }

    private val evdevDevices: Flow<List<EvdevDeviceInfo>?> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            evdevHandleCache.devices
        } else {
            flowOf(null)
        }

    /**
     * Cache the data required for checking errors to reduce the latency of repeatedly checking
     * the errors.
     */
    override val triggerErrorSnapshot: Flow<TriggerErrorSnapshot> = combine(
        merge(
            permissionAdapter.onPermissionsUpdate.onStart { emit(Unit) },
            inputMethodAdapter.chosenIme,
        ),
        purchasesFlow,
        showDpadImeSetupError,
        systemBridgeConnectionState,
        evdevDevices,
    ) { _, purchases, showDpadImeSetupError, systemBridgeConnectionState, evdevDevices ->
        TriggerErrorSnapshot(
            isKeyMapperImeChosen = keyMapperImeHelper.isCompatibleImeChosen(),
            isDndAccessGranted = permissionAdapter.isGranted(Permission.ACCESS_NOTIFICATION_POLICY),
            isRootGranted = permissionAdapter.isGranted(Permission.ROOT),
            purchases = purchases.dataOrNull() ?: Success(emptySet()),
            showDpadImeSetupError = showDpadImeSetupError,
            isSystemBridgeConnected = systemBridgeConnectionState is SystemBridgeConnectionState.Connected,
            evdevDevices = evdevDevices,
        )
    }

    override val showDeviceDescriptors: Flow<Boolean> =
        settingsRepository.get(Keys.showDeviceDescriptors).map { it == true }

    override fun neverShowDpadImeSetupError() {
        settingsRepository.set(Keys.neverShowDpadImeTriggerError, true)
    }

    override suspend fun isFloatingButtonsPurchased(): Boolean {
        return purchasingManager.isPurchased(ProductId.FLOATING_BUTTONS).valueIfFailure { false }
    }

    override suspend fun fixTriggerError(error: TriggerError) {
        when (error) {
            TriggerError.DND_ACCESS_DENIED -> fixError(
                PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY),
            )

            TriggerError.CANT_DETECT_IN_PHONE_CALL -> fixError(
                KMError.CantDetectKeyEventsInPhoneCall,
            )
            TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED -> fixError(
                ProductNotPurchased(
                    ProductId.ASSISTANT_TRIGGER,
                ),
            )

            TriggerError.DPAD_IME_NOT_SELECTED -> fixError(KMError.DpadTriggerImeNotSelected)
            TriggerError.FLOATING_BUTTONS_NOT_PURCHASED -> fixError(
                ProductNotPurchased(
                    ProductId.FLOATING_BUTTONS,
                ),
            )

            TriggerError.PURCHASE_VERIFICATION_FAILED -> purchasingManager.refresh()
            TriggerError.SYSTEM_BRIDGE_DISCONNECTED -> fixError(SystemBridgeError.Disconnected)
            TriggerError.EVDEV_DEVICE_NOT_FOUND, TriggerError.FLOATING_BUTTON_DELETED, TriggerError.SYSTEM_BRIDGE_UNSUPPORTED -> {
            }
        }
    }

    override fun getAppName(packageName: String): KMResult<String> =
        packageManagerAdapter.getAppName(packageName)

    override fun getAppIcon(packageName: String): KMResult<Drawable> =
        packageManagerAdapter.getAppIcon(packageName)

    override fun getInputMethodLabel(imeId: String): KMResult<String> =
        inputMethodAdapter.getInfoById(imeId).then { Success(it.label) }

    override suspend fun fixError(error: KMError) {
        when (error) {
            is KMError.AppDisabled -> packageManagerAdapter.enableApp(error.packageName)
            is KMError.AppNotFound -> packageManagerAdapter.downloadApp(error.packageName)
            KMError.NoCompatibleImeChosen ->
                keyMapperImeHelper.chooseCompatibleInputMethod().otherwise {
                    inputMethodAdapter.showImePicker(fromForeground = true)
                }

            KMError.NoCompatibleImeEnabled -> keyMapperImeHelper.enableCompatibleInputMethods()
            is ImeDisabled -> switchImeInterface.enableIme(error.ime.id)
            is PermissionDenied -> permissionAdapter.request(error.permission)
            is KMError.ShizukuNotStarted -> packageManagerAdapter.openApp(
                ShizukuUtils.SHIZUKU_PACKAGE,
            )
            is KMError.CantDetectKeyEventsInPhoneCall -> {
                if (!keyMapperImeHelper.isCompatibleImeEnabled()) {
                    keyMapperImeHelper.enableCompatibleInputMethods()
                }

                // wait for compatible ime to be enabled then choose it.
                keyMapperImeHelper.isCompatibleImeEnabledFlow.first { it }

                keyMapperImeHelper.chooseCompatibleInputMethod().otherwise {
                    inputMethodAdapter.showImePicker(fromForeground = true)
                }
            }

            is SystemBridgeError.Disconnected -> navigationProvider.navigate(
                "fix_system_bridge",
                NavDestination.ProMode,
            )

            is KMError.DpadTriggerImeNotSelected -> {
                if (keyMapperImeHelper.isCompatibleImeEnabled()) {
                    keyMapperImeHelper.chooseCompatibleInputMethod()
                } else {
                    keyMapperImeHelper.enableCompatibleInputMethods()
                }
            }

            else -> Unit
        }
    }

    override fun startAccessibilityService(): Boolean = accessibilityServiceAdapter.start()

    override fun restartAccessibilityService(): Boolean = accessibilityServiceAdapter.restart()

    override fun neverShowDndTriggerError() {
        settingsRepository.set(Keys.neverShowDndAccessError, true)
    }

    override fun getRingtoneLabel(uri: String): KMResult<String> {
        return ringtoneAdapter.getLabel(uri)
    }
}

interface DisplayKeyMapUseCase :
    DisplayActionUseCase,
    DisplayConstraintUseCase {

    val triggerErrorSnapshot: Flow<TriggerErrorSnapshot>
    suspend fun isFloatingButtonsPurchased(): Boolean
    suspend fun fixTriggerError(error: TriggerError)
    override val showDeviceDescriptors: Flow<Boolean>

    fun neverShowDpadImeSetupError()
}
