package io.github.sds100.keymapper.mappings.keymaps

import android.os.Build
import android.view.KeyEvent
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.DisplaySimpleMappingUseCase
import io.github.sds100.keymapper.mappings.keymaps.trigger.AssistantTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyCodeTriggerKey
import io.github.sds100.keymapper.mappings.keymaps.trigger.KeyEventDetectionSource
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerError
import io.github.sds100.keymapper.purchasing.ProductId
import io.github.sds100.keymapper.purchasing.PurchasingManager
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeHelper
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.valueIfFailure
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Created by sds100 on 04/04/2021.
 */

class DisplayKeyMapUseCaseImpl(
    private val permissionAdapter: PermissionAdapter,
    private val inputMethodAdapter: InputMethodAdapter,
    displaySimpleMappingUseCase: DisplaySimpleMappingUseCase,
    private val preferences: PreferenceRepository,
    private val purchasingManager: PurchasingManager,
) : DisplayKeyMapUseCase,
    DisplaySimpleMappingUseCase by displaySimpleMappingUseCase {
    private companion object {
        val keysThatRequireDndAccess = arrayOf(
            KeyEvent.KEYCODE_VOLUME_DOWN,
            KeyEvent.KEYCODE_VOLUME_UP,
        )
    }

    private val keyMapperImeHelper: KeyMapperImeHelper = KeyMapperImeHelper(inputMethodAdapter)

    private val showDpadImeSetupError: Flow<Boolean> =
        preferences.get(Keys.neverShowDpadImeTriggerError).map { neverShow ->
            if (neverShow == null) {
                true
            } else {
                !neverShow
            }
        }

    /**
     * Cache the data required for checking errors to reduce the latency of repeatedly checking
     * the errors.
     */
    override val triggerErrorSnapshot: Flow<TriggerErrorSnapshot> = combine(
        permissionAdapter.onPermissionsUpdate.onStart { emit(Unit) },
        purchasingManager.purchases,
        inputMethodAdapter.chosenIme,
        showDpadImeSetupError,
    ) { _, purchases, _, showDpadImeSetupError ->
        TriggerErrorSnapshot(
            isKeyMapperImeChosen = keyMapperImeHelper.isCompatibleImeChosen(),
            isDndAccessGranted = permissionAdapter.isGranted(Permission.ACCESS_NOTIFICATION_POLICY),
            isRootGranted = permissionAdapter.isGranted(Permission.ROOT),
            isAssistantTriggerPurchased = purchases.dataOrNull()
                ?.contains(ProductId.ASSISTANT_TRIGGER) ?: false,
            isFloatingButtonsPurchased = purchases.dataOrNull()
                ?.contains(ProductId.FLOATING_BUTTONS) ?: false,
            showDpadImeSetupError = showDpadImeSetupError,
        )
    }

    override val invalidateTriggerErrors: Flow<Unit> = triggerErrorSnapshot.map { }

    override val showTriggerKeyboardIconExplanation: Flow<Boolean> =
        preferences.get(Keys.neverShowTriggerKeyboardIconExplanation).map { neverShow ->
            if (neverShow == null) {
                true
            } else {
                !neverShow
            }
        }

    override fun neverShowTriggerKeyboardIconExplanation() {
        preferences.set(Keys.neverShowTriggerKeyboardIconExplanation, true)
    }

    override fun neverShowDpadImeSetupError() {
        preferences.set(Keys.neverShowDpadImeTriggerError, true)
    }

    // TODO Delete
    override suspend fun getTriggerErrors(keyMap: KeyMap): List<TriggerError> {
        val trigger = keyMap.trigger
        val errors = mutableListOf<TriggerError>()
        val isKeyMapperImeChosen = keyMapperImeHelper.isCompatibleImeChosen()

        // can only detect volume button presses during a phone call with an input method service
        if (!isKeyMapperImeChosen && keyMap.requiresImeKeyEventForwarding()) {
            errors.add(TriggerError.CANT_DETECT_IN_PHONE_CALL)
        }

        val requiresDndAccess = trigger.keys
            .mapNotNull { it as? KeyCodeTriggerKey }
            .any { it.keyCode in keysThatRequireDndAccess }

        if (requiresDndAccess) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !permissionAdapter.isGranted(Permission.ACCESS_NOTIFICATION_POLICY)
            ) {
                errors.add(TriggerError.DND_ACCESS_DENIED)
            }
        }

        if (trigger.screenOffTrigger &&
            !permissionAdapter.isGranted(Permission.ROOT) &&
            trigger.isDetectingWhenScreenOffAllowed()
        ) {
            errors.add(TriggerError.SCREEN_OFF_ROOT_DENIED)
        }

        val containsAssistantTrigger = trigger.keys.any { it is AssistantTriggerKey }
        val containsDeviceAssistantTrigger =
            trigger.keys.any { it is AssistantTriggerKey && it.requiresDeviceAssistant() }

        val isAssistantTriggerPurchased =
            purchasingManager.isPurchased(ProductId.ASSISTANT_TRIGGER).valueIfFailure { false }

        if (containsAssistantTrigger && !isAssistantTriggerPurchased) {
            errors.add(TriggerError.ASSISTANT_TRIGGER_NOT_PURCHASED)
        }

        val containsDpadKey = trigger.keys
            .mapNotNull { it as? KeyCodeTriggerKey }
            .any { InputEventUtils.isDpadKeyCode(it.keyCode) && it.detectionSource == KeyEventDetectionSource.INPUT_METHOD }

        if (showDpadImeSetupError.first() && !isKeyMapperImeChosen && containsDpadKey) {
            errors.add(TriggerError.DPAD_IME_NOT_SELECTED)
        }

        return errors
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
}

interface DisplayKeyMapUseCase : DisplaySimpleMappingUseCase {
    val invalidateTriggerErrors: Flow<Unit>
    val triggerErrorSnapshot: Flow<TriggerErrorSnapshot>
    suspend fun getTriggerErrors(keyMap: KeyMap): List<TriggerError>
    suspend fun fixTriggerError(error: TriggerError)
    val showTriggerKeyboardIconExplanation: Flow<Boolean>
    fun neverShowTriggerKeyboardIconExplanation()

    fun neverShowDpadImeSetupError()
}
