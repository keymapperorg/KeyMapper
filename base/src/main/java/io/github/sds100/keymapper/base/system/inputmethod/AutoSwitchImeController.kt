package io.github.sds100.keymapper.base.system.inputmethod

import android.os.Build
import android.view.inputmethod.EditorInfo
import androidx.annotation.RequiresApi
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.keymaps.PauseKeyMapsUseCase
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.common.BuildConfigProvider
import io.github.sds100.keymapper.common.utils.Success
import io.github.sds100.keymapper.common.utils.isSuccess
import io.github.sds100.keymapper.common.utils.onFailure
import io.github.sds100.keymapper.common.utils.onSuccess
import io.github.sds100.keymapper.common.utils.otherwise
import io.github.sds100.keymapper.common.utils.valueOrNull
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.data.utils.PrefDelegate
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.popup.ToastAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn

class AutoSwitchImeController @AssistedInject constructor(
    // Use the accessibility service so the calls are synchronous. This will reduce race conditions
    // checking which input method is chosen when they start/finish.
    @Assisted
    private val switchImeInterface: SwitchImeInterface,
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
            switchImeInterface: SwitchImeInterface,
            coroutineScope: CoroutineScope
        ): AutoSwitchImeController
    }

    private val imeHelper =
        KeyMapperImeHelper(switchImeInterface, inputMethodAdapter, buildConfigProvider.packageName)

    private val devicesThatToggleKeyboard: Set<String> by PrefDelegate(
        Keys.devicesThatChangeIme,
        emptySet()
    )

    private val changeImeOnDeviceConnect: Boolean by PrefDelegate(
        Keys.changeImeOnDeviceConnect,
        false
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

    private val changeImeOnInputFocus: StateFlow<Boolean> =
        preferenceRepository
            .get(Keys.changeImeOnInputFocus)
            .map { it ?: PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS }
            .stateIn(
                coroutineScope,
                SharingStarted.Eagerly,
                PreferenceDefaults.CHANGE_IME_ON_INPUT_FOCUS,
            )

    fun init() {
        pauseKeyMapsUseCase.isPaused.onEach { isPaused ->

            if (!toggleKeyboardOnToggleKeymaps) return@onEach

            if (isPaused) {
                chooseIncompatibleIme(imePickerAllowed = true)
            } else {
                chooseCompatibleIme(imePickerAllowed = true)
            }
        }.launchIn(coroutineScope)

        devicesAdapter.onInputDeviceConnect.onEach { device ->
            if (changeImeOnDeviceConnect && devicesThatToggleKeyboard.contains(device.descriptor)) {
                chooseCompatibleIme(imePickerAllowed = true)
            }
        }.launchIn(coroutineScope)

        devicesAdapter.onInputDeviceDisconnect.onEach { device ->
            if (changeImeOnDeviceConnect && devicesThatToggleKeyboard.contains(device.descriptor)) {
                chooseIncompatibleIme(imePickerAllowed = true)
            }
        }.launchIn(coroutineScope)
    }

    var dropNextFinishEvent = false

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun onStartInput(attribute: EditorInfo, restarting: Boolean) {
        if (!changeImeOnInputFocus.value) {
            return
        }

        // Make sure the input type actually accepts text. Sometimes the input method
        // can be started even when the user isn't typing.
        // One must use the mask because other bits are used for flags.
        val isValidInputStarted =
            (attribute.inputType and EditorInfo.TYPE_MASK_CLASS) != EditorInfo.TYPE_NULL

        val result = if (isValidInputStarted) {
            chooseIncompatibleIme(imePickerAllowed = false)
        } else {
            chooseCompatibleIme(imePickerAllowed = false)
        }

        // Drop the next event if the IME was just changed to prevent an infinite loop.
        if (result) {
            dropNextFinishEvent = true
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun onFinishInput() {
        if (!changeImeOnInputFocus.value) {
            return
        }

        if (dropNextFinishEvent) {
            dropNextFinishEvent = false
            return
        }

        chooseCompatibleIme(imePickerAllowed = false)
    }

    private fun chooseIncompatibleIme(imePickerAllowed: Boolean): Boolean {
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
            .otherwise {
                if (imePickerAllowed) {
                    inputMethodAdapter.showImePicker(fromForeground = false)
                } else {
                    Success(Unit)
                }
            }
            .onFailure { error ->
                toastAdapter.show(error.getFullMessage(resourceProvider))
            }
            .isSuccess
    }

    private fun chooseCompatibleIme(imePickerAllowed: Boolean): Boolean {
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
            .otherwise {
                if (imePickerAllowed) {
                    inputMethodAdapter.showImePicker(fromForeground = false)
                } else {
                    Success(Unit)
                }
            }
            .onFailure { error ->
                toastAdapter.show(error.getFullMessage(resourceProvider))
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
