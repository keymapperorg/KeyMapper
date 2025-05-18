package io.github.sds100.keymapper.base.keymaps

import android.graphics.drawable.Drawable
import io.github.sds100.keymapper.base.actions.DisplayActionUseCase
import io.github.sds100.keymapper.base.actions.GetActionErrorUseCase
import io.github.sds100.keymapper.common.utils.Error
import io.github.sds100.keymapper.common.utils.Result
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.otherwise
import io.github.sds100.keymapper.common.utils.then
import io.github.sds100.keymapper.common.utils.valueIfFailure
import io.github.sds100.keymapper.constraints.DisplayConstraintUseCase
import io.github.sds100.keymapper.constraints.GetConstraintErrorUseCase
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.base.purchasing.ProductId
import io.github.sds100.keymapper.base.purchasing.PurchasingError
import io.github.sds100.keymapper.base.purchasing.PurchasingManager
import io.github.sds100.keymapper.system.shizuku.ShizukuUtils
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.system.ringtone.RingtoneAdapter
import io.github.sds100.keymapper.trigger.TriggerError
import io.github.sds100.keymapper.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.common.utils.dataOrNull
import io.github.sds100.keymapper.base.utils.SettingsRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DisplayKeyMapUseCaseImpl @Inject constructor(
    private val permissionAdapter: PermissionAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val packageManagerAdapter: PackageManagerAdapter,
    private val settingsRepository: SettingsRepository,
    private val accessibilityServiceAdapter: io.github.sds100.keymapper.system.accessibility.AccessibilityServiceAdapterImpl,
    private val settingsRepository2: SettingsRepository,
    private val purchasingManager: PurchasingManager,
    private val ringtoneAdapter: RingtoneAdapter,
    private val getActionErrorUseCase: GetActionErrorUseCase,
    private val getConstraintErrorUseCase: GetConstraintErrorUseCase
) : DisplayKeyMapUseCase,
    GetActionErrorUseCase by getActionErrorUseCase,
    GetConstraintErrorUseCase by getConstraintErrorUseCase {
    private val keyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

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
    private val purchasesFlow: Flow<State<Result<Set<ProductId>>>> = callbackFlow {
        try {
            val value = withTimeout(5000L) {
                purchasingManager.purchases.filterIsInstance<State.Data<Result<Set<ProductId>>>>()
                    .first()
            }

            send(value)
        } catch (_: TimeoutCancellationException) {
        }

        purchasingManager.purchases.collect(this::send)
    }

    /**
     * Cache the data required for checking errors to reduce the latency of repeatedly checking
     * the errors.
     */
    override val triggerErrorSnapshot: Flow<TriggerErrorSnapshot> = combine(
        permissionAdapter.onPermissionsUpdate.onStart { emit(Unit) },
        purchasesFlow,
        inputMethodAdapter.chosenIme,
        showDpadImeSetupError,
    ) { _, purchases, _, showDpadImeSetupError ->
        TriggerErrorSnapshot(
            isKeyMapperImeChosen = keyMapperImeHelper.isCompatibleImeChosen(),
            isDndAccessGranted = permissionAdapter.isGranted(Permission.ACCESS_NOTIFICATION_POLICY),
            isRootGranted = permissionAdapter.isGranted(Permission.ROOT),
            purchases = purchases.dataOrNull() ?: Success(emptySet()),
            showDpadImeSetupError = showDpadImeSetupError,
        )
    }

    override val showTriggerKeyboardIconExplanation: Flow<Boolean> =
        settingsRepository.get(Keys.neverShowTriggerKeyboardIconExplanation).map { neverShow ->
            if (neverShow == null) {
                true
            } else {
                !neverShow
            }
        }

    override val showDeviceDescriptors: Flow<Boolean> =
        settingsRepository2.get(Keys.showDeviceDescriptors).map { it == true }

    override fun neverShowTriggerKeyboardIconExplanation() {
        settingsRepository.set(Keys.neverShowTriggerKeyboardIconExplanation, true)
    }

    override fun neverShowDpadImeSetupError() {
        settingsRepository.set(Keys.neverShowDpadImeTriggerError, true)
    }

    override suspend fun isFloatingButtonsPurchased(): Boolean {
        return purchasingManager.isPurchased(ProductId.FLOATING_BUTTONS).valueIfFailure { false }
    }

    override suspend fun fixTriggerError(error: TriggerError) {
        when (error) {
            TriggerError.DND_ACCESS_DENIED -> fixError(SystemError.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY))
            TriggerError.SCREEN_OFF_ROOT_DENIED -> fixError(
                SystemError.PermissionDenied(
                    Permission.ROOT,
                ),
            )
            TriggerError.CANT_DETECT_IN_PHONE_CALL -> fixError(Error.CantDetectKeyEventsInPhoneCall)
            TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED -> fixError(
                PurchasingError.ProductNotPurchased(
                    ProductId.ASSISTANT_TRIGGER,
                ),
            )

            TriggerError.DPAD_IME_NOT_SELECTED -> fixError(Error.DpadTriggerImeNotSelected)
            TriggerError.FLOATING_BUTTON_DELETED -> {}
            TriggerError.FLOATING_BUTTONS_NOT_PURCHASED -> fixError(
                PurchasingError.ProductNotPurchased(
                    ProductId.FLOATING_BUTTONS,
                ),
            )

            TriggerError.PURCHASE_VERIFICATION_FAILED -> purchasingManager.refresh()
        }
    }

    override fun getAppName(packageName: String): Result<String> = packageManagerAdapter.getAppName(packageName)

    override fun getAppIcon(packageName: String): Result<Drawable> = packageManagerAdapter.getAppIcon(packageName)

    override fun getInputMethodLabel(imeId: String): Result<String> = inputMethodAdapter.getInfoById(imeId).then { Success(it.label) }

    override suspend fun fixError(error: Error) {
        when (error) {
            is Error.AppDisabled -> packageManagerAdapter.enableApp(error.packageName)
            is Error.AppNotFound -> packageManagerAdapter.downloadApp(error.packageName)
            Error.NoCompatibleImeChosen ->
                keyMapperImeHelper.chooseCompatibleInputMethod().otherwise {
                    inputMethodAdapter.showImePicker(fromForeground = true)
                }

            Error.NoCompatibleImeEnabled -> keyMapperImeHelper.enableCompatibleInputMethods()
            is SystemError.ImeDisabled -> inputMethodAdapter.enableIme(error.ime.id)
            is SystemError.PermissionDenied -> permissionAdapter.request(error.permission)
            is Error.ShizukuNotStarted -> packageManagerAdapter.openApp(ShizukuUtils.SHIZUKU_PACKAGE)
            is Error.CantDetectKeyEventsInPhoneCall -> {
                if (!keyMapperImeHelper.isCompatibleImeEnabled()) {
                    keyMapperImeHelper.enableCompatibleInputMethods()
                }

                // wait for compatible ime to be enabled then choose it.
                keyMapperImeHelper.isCompatibleImeEnabledFlow.first { it }

                keyMapperImeHelper.chooseCompatibleInputMethod().otherwise {
                    inputMethodAdapter.showImePicker(fromForeground = true)
                }
            }

            else -> Unit
        }
    }

    override fun startAccessibilityService(): Boolean = accessibilityServiceAdapter.start()

    override fun restartAccessibilityService(): Boolean = accessibilityServiceAdapter.restart()

    override fun neverShowDndTriggerError() {
        settingsRepository2.set(Keys.neverShowDndAccessError, true)
    }

    override fun getRingtoneLabel(uri: String): Result<String> {
        return ringtoneAdapter.getLabel(uri)
    }
}

interface DisplayKeyMapUseCase :
    DisplayActionUseCase,
    DisplayConstraintUseCase {

    val triggerErrorSnapshot: Flow<TriggerErrorSnapshot>
    suspend fun isFloatingButtonsPurchased(): Boolean
    suspend fun fixTriggerError(error: TriggerError)
    val showTriggerKeyboardIconExplanation: Flow<Boolean>
    fun neverShowTriggerKeyboardIconExplanation()
    override val showDeviceDescriptors: Flow<Boolean>

    fun neverShowDpadImeSetupError()
}
