package io.github.sds100.keymapper.base.system.inputmethod

import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.system.accessibility.BaseAccessibilityService
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.KMError
import io.github.sds100.keymapper.common.utils.isSuccess
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.data.utils.PrefDelegate
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.popup.ToastAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

/**
 * This requires Android 11+ because this is when the accessibility service API for switching
 * input methods was introduced. On older versions one would have to use WRITE_SECURE_SETTINGS
 * permission, and it is not worth the effort to build a UI to explain this in the app.
 */
@RequiresApi(Build.VERSION_CODES.R)
class AutoSwitchImeController @AssistedInject constructor(
    // Use the accessibility service so the calls are synchronous. This will reduce race conditions
    // checking which input method is chosen when they start/finish.
    @Assisted
    private val service: BaseAccessibilityService,
    @Assisted
    private val coroutineScope: CoroutineScope,
    private val preferenceRepository: PreferenceRepository,
    private val inputMethodAdapter: InputMethodAdapter,
    private val pauseKeyMapsUseCase: PauseKeyMapsUseCase,
    private val devicesAdapter: DevicesAdapter,
    private val toastAdapter: ToastAdapter,
    private val resourceProvider: ResourceProvider,
    private val buildConfigProvider: BuildConfigProvider,
) : PreferenceRepository by preferenceRepository {

    @AssistedFactory
    interface Factory {
        fun create(
            accessibilityService: BaseAccessibilityService,
            coroutineScope: CoroutineScope,
        ): AutoSwitchImeController
    }

    private val imeHelper: KeyMapperImeHelper = KeyMapperImeHelper(
        service,
        inputMethodAdapter,
        buildConfigProvider.packageName,
    )

    private val devicesThatToggleKeyboard: Set<String> by PrefDelegate(
        Keys.devicesThatChangeIme,
        emptySet(),
    )

    private val changeImeOnDeviceConnect: Boolean by PrefDelegate(
        Keys.changeImeOnDeviceConnect,
        false,
    )

    private val toggleKeyboardOnToggleKeymaps by PrefDelegate(
        Keys.toggleKeyboardOnToggleKeymaps,
        false,
    )

    private var showToast: StateFlow<Boolean> =
        preferenceRepository.get(Keys.showToastWhenAutoChangingIme)
            .map { it ?: PreferenceDefaults.SHOW_TOAST_WHEN_AUTO_CHANGE_IME }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                PreferenceDefaults.SHOW_TOAST_WHEN_AUTO_CHANGE_IME,
            )

    private val changeImeOnInputFocusPreference: Flow<Boolean> =
        preferenceRepository
            .get(Keys.changeImeOnInputFocus)
            .map { it ?: PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS }

    private val changeImeOnToggleKeyMaps: Flow<Boolean> =
        preferenceRepository
            .get(Keys.toggleKeyboardOnToggleKeymaps)
            .map { it ?: false }

    /**
     * Only change the input method when input is started/finished if the user has enabled
     * the setting, and key maps are resumed if the option to switch ime on toggle key maps
     * is also enabled. This prevents the IME immediately changing again when
     * the user pauses their key maps.
     */
    private val changeImeOnStartInput: StateFlow<Boolean> = combine(
        changeImeOnInputFocusPreference,
        changeImeOnToggleKeyMaps,
        pauseKeyMapsUseCase.isPaused,
    ) { changeOnFocus, toggleOnKeyMaps, isPaused ->
        changeOnFocus && (!toggleOnKeyMaps || !isPaused)
    }.stateIn(
        coroutineScope,
        SharingStarted.Eagerly,
        false,
    )

    private var isImeBeingSwitched = false

    fun init() {
        pauseKeyMapsUseCase.isPaused.onEach { isPaused ->
            if (!toggleKeyboardOnToggleKeymaps) return@onEach

            if (isPaused) {
                chooseIncompatibleIme()
            } else {
                chooseCompatibleIme()
            }
        }.launchIn(coroutineScope)

        devicesAdapter.onInputDeviceConnect.onEach { device ->
            if (changeImeOnDeviceConnect && devicesThatToggleKeyboard.contains(device.descriptor)) {
                chooseCompatibleIme()
            }
        }.launchIn(coroutineScope)

        devicesAdapter.onInputDeviceDisconnect.onEach { device ->
            if (changeImeOnDeviceConnect && devicesThatToggleKeyboard.contains(device.descriptor)) {
                chooseIncompatibleIme()
            }
        }.launchIn(coroutineScope)
    }

    fun onAccessibilityEvent(event: AccessibilityEvent) {
        // On SDK 33 and newer, the more reliable accessibility input method API is used.
        // See onStartInput and onFinishInput. On my OxygenOS 11 it can not detect the Key Mapper
        // Basic input method window so onStartInput is also called from the KeyMapperImeService as
        // a fallback.

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU &&
            changeImeOnStartInput.value &&
            event.eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) {
            if (isImeBeingSwitched) {
                isImeBeingSwitched = false
                return
            }

            val isInputStarted = isImeWindowVisible()

            if (isInputStarted) {
                if (chooseIncompatibleIme()) {
                    isImeBeingSwitched = true
                }
            } else {
                if (chooseCompatibleIme()) {
                    isImeBeingSwitched = true
                }
            }
        }
    }

    private fun isImeWindowVisible(): Boolean {
        val imeWindow: AccessibilityWindowInfo? =
            service.windows.find { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }

        return imeWindow != null && imeWindow.root?.isVisibleToUser == true
    }

    fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        if (!changeImeOnStartInput.value) {
            return
        }

        // Make sure the input type actually accepts text because sometimes the input method
        // can be started even when the user isn't typing, such as in Minecraft.
        // One must use the mask because other bits are used for flags.
        // There are cases where the ime is showing but the app reports no TYPE_CLASS for some reason
        // such as in the Reddit search bar so as a fallback check for a label or hint.
        val isValidInputStarted =
            (attribute.inputType and EditorInfo.TYPE_MASK_CLASS) != EditorInfo.TYPE_NULL ||
                attribute.label != null ||
                attribute.hintText != null

        val result = if (isValidInputStarted) {
            chooseIncompatibleIme()
        } else {
            chooseCompatibleIme()
        }

        // Drop the next event if the IME was just changed to prevent an infinite loop.
        if (result) {
            isImeBeingSwitched = true
        }
    }

    fun onFinishInput() {
        if (!changeImeOnStartInput.value) {
            return
        }

        if (isImeBeingSwitched) {
            isImeBeingSwitched = false
            return
        }

        chooseCompatibleIme()
    }

    private fun chooseIncompatibleIme(): Boolean {
        // only choose the keyboard if the correct one isn't already chosen
        if (!imeHelper.isCompatibleImeChosen()) {
            return false
        }

        return imeHelper.chooseLastUsedIncompatibleInputMethod()
            .onSuccess { imeId ->
                if (showToast.value) {
                    showToast(imeId)
                }
            }
            .onFailure { error ->
                toastAdapter.show(error.getFullMessage(resourceProvider))
            }
            .isSuccess
    }

    private fun chooseCompatibleIme(): Boolean {
        // only choose the keyboard if the correct one isn't already chosen
        if (imeHelper.isCompatibleImeChosen()) {
            return false
        }

        return imeHelper.chooseCompatibleInputMethod()
            .onSuccess { imeId ->
                if (showToast.value) {
                    showToast(imeId)
                }
            }
            .onFailure { error ->
                // Do not show an error if no IME is enabled, just let this auto switching
                // feature not work silently. If the user hasn't enabled an IME then they probably
                // aren't using any feature that requires the IME.
                if (error != KMError.NoCompatibleImeEnabled) {
                    toastAdapter.show(error.getFullMessage(resourceProvider))
                }
            }
            .isSuccess
    }

    private fun showToast(imeId: String) {
        val imeLabel = inputMethodAdapter.getInfoById(imeId).valueOrNull()?.label ?: return

        val message =
            resourceProvider.getString(R.string.toast_chose_keyboard, imeLabel)
        toastAdapter.show(message)
    }
}
