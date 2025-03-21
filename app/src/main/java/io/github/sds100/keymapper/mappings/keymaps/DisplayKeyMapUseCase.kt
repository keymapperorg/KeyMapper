package io.github.sds100.keymapper.mappings.keymaps

import android.graphics.drawable.Drawable
import android.view.KeyEvent
import io.github.sds100.keymapper.actions.GetActionErrorUseCase
import io.github.sds100.keymapper.constraints.GetConstraintErrorUseCase
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.DisplayActionUseCase
import io.github.sds100.keymapper.mappings.DisplayConstraintUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerErrorSnapshot
import io.github.sds100.keymapper.purchasing.ProductId
import io.github.sds100.keymapper.purchasing.PurchasingManager
import io.github.sds100.keymapper.shizuku.ShizukuUtils
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.otherwise
import io.github.sds100.keymapper.util.then
import io.github.sds100.keymapper.util.valueIfFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeout

/**
 * Created by sds100 on 04/04/2021.
 */

class DisplayKeyMapUseCaseImpl(
    private val permissionAdapter: PermissionAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    private val packageManager: PackageManagerAdapter,
    private val preferenceRepository: PreferenceRepository,
    private val accessibilityServiceAdapter: ServiceAdapter,
    private val preferences: PreferenceRepository,
    private val purchasingManager: PurchasingManager,
    getActionError: GetActionErrorUseCase,
    getConstraintError: GetConstraintErrorUseCase,
) : DisplayKeyMapUseCase,
    GetActionErrorUseCase by getActionError,
    GetConstraintErrorUseCase by getConstraintError {
    private companion object {
        val keysThatRequireDndAccess = arrayOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
        )
    }

    private val keyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    private val showDpadImeSetupError: Flow<Boolean> =
        preferences.get(Keys.neverShowDpadImeTriggerError).map { neverShow ->
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
    private val purchasesFlow: Flow<State<Set<ProductId>>> = callbackFlow {
        withTimeout(3000L) {
            val value =
                purchasingManager.purchases.filterIsInstance<State.Data<Set<ProductId>>>().first()
            send(value)
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
            purchases = purchases.dataOrNull() ?: emptySet(),
            showDpadImeSetupError = showDpadImeSetupError,
        )
    }

    override val showTriggerKeyboardIconExplanation: Flow<Boolean> =
        preferences.get(Keys.neverShowTriggerKeyboardIconExplanation).map { neverShow ->
            if (neverShow == null) {
                true
            } else {
                !neverShow
            }
        }

    override val showDeviceDescriptors: Flow<Boolean> =
        preferenceRepository.get(Keys.showDeviceDescriptors).map { it == true }

    override fun neverShowTriggerKeyboardIconExplanation() {
        preferences.set(Keys.neverShowTriggerKeyboardIconExplanation, true)
    }

    override fun neverShowDpadImeSetupError() {
        preferences.set(Keys.neverShowDpadImeTriggerError, true)
    }

    override suspend fun isFloatingButtonsPurchased(): Boolean {
        return purchasingManager.isPurchased(ProductId.FLOATING_BUTTONS).valueIfFailure { false }
    }

    override suspend fun fixTriggerError(error: TriggerError) {
        when (error) {
            TriggerError.DND_ACCESS_DENIED -> fixError(Error.PermissionDenied(Permission.ACCESS_NOTIFICATION_POLICY))
            TriggerError.SCREEN_OFF_ROOT_DENIED -> fixError(Error.PermissionDenied(Permission.ROOT))
            TriggerError.CANT_DETECT_IN_PHONE_CALL -> fixError(Error.CantDetectKeyEventsInPhoneCall)
            TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED -> fixError(
                Error.ProductNotPurchased(
                    ProductId.ASSISTANT_TRIGGER,
                ),
            )

            TriggerError.DPAD_IME_NOT_SELECTED -> fixError(Error.DpadTriggerImeNotSelected)
            TriggerError.FLOATING_BUTTON_DELETED -> {}
            TriggerError.FLOATING_BUTTONS_NOT_PURCHASED -> fixError(
                Error.ProductNotPurchased(
                    ProductId.FLOATING_BUTTONS,
                ),
            )
        }
    }

    override fun getAppName(packageName: String): Result<String> = packageManager.getAppName(packageName)

    override fun getAppIcon(packageName: String): Result<Drawable> = packageManager.getAppIcon(packageName)

    override fun getInputMethodLabel(imeId: String): Result<String> = inputMethodAdapter.getInfoById(imeId).then { Success(it.label) }

    override suspend fun fixError(error: Error) {
        when (error) {
            is Error.AppDisabled -> packageManager.enableApp(error.packageName)
            is Error.AppNotFound -> packageManager.downloadApp(error.packageName)
            Error.NoCompatibleImeChosen ->
                keyMapperImeHelper.chooseCompatibleInputMethod().otherwise {
                    inputMethodAdapter.showImePicker(fromForeground = true)
                }

            Error.NoCompatibleImeEnabled -> keyMapperImeHelper.enableCompatibleInputMethods()
            is Error.ImeDisabled -> inputMethodAdapter.enableIme(error.ime.id)
            is Error.PermissionDenied -> permissionAdapter.request(error.permission)
            is Error.ShizukuNotStarted -> packageManager.openApp(ShizukuUtils.SHIZUKU_PACKAGE)
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
        preferenceRepository.set(Keys.neverShowDndAccessError, true)
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
