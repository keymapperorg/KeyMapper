package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.util.PrefDelegate
import io.github.sds100.keymapper.util.ServiceEvent
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.otherwise
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Created by sds100 on 20/04/2021.
 */
class AutoSwitchImeController(
    private val coroutineScope: CoroutineScope,
    private val preferenceRepository: PreferenceRepository,
    private val inputMethodAdapter: InputMethodAdapter,
    private val pauseMappingsUseCase: PauseMappingsUseCase,
    private val devicesAdapter: DevicesAdapter,
    private val popupMessageAdapter: PopupMessageAdapter,
    private val resourceProvider: ResourceProvider,
    private val accessibilityServiceAdapter: ServiceAdapter,
) : PreferenceRepository by preferenceRepository {
    private val imeHelper = KeyMapperImeHelper(inputMethodAdapter)

    private val devicesThatToggleKeyboard
        by PrefDelegate(Keys.devicesThatChangeIme, emptySet())

    private val devicesThatShowImePicker
        by PrefDelegate(Keys.devicesThatShowImePicker, emptySet())

    private val changeImeOnDeviceConnect by PrefDelegate(Keys.changeImeOnDeviceConnect, false)
    private val showImePickerOnBtConnect by PrefDelegate(Keys.showImePickerOnDeviceConnect, false)

    private val toggleKeyboardOnToggleKeymaps by PrefDelegate(
        Keys.toggleKeyboardOnToggleKeymaps,
        false,
    )

    private var changeImeOnInputFocus: Boolean = false

    private var showToast: Boolean = PreferenceDefaults.SHOW_TOAST_WHEN_AUTO_CHANGE_IME

    init {
        pauseMappingsUseCase.isPaused.onEach { isPaused ->

            if (!toggleKeyboardOnToggleKeymaps) return@onEach

            if (isPaused) {
                chooseIncompatibleIme(imePickerAllowed = true)
            } else {
                chooseCompatibleIme(imePickerAllowed = true)
            }
        }.launchIn(coroutineScope)

        devicesAdapter.onInputDeviceConnect.onEach { device ->
            if (showImePickerOnBtConnect && devicesThatShowImePicker.contains(device.descriptor)) {
                inputMethodAdapter.showImePicker(fromForeground = false)
            }

            if (changeImeOnDeviceConnect && devicesThatToggleKeyboard.contains(device.descriptor)) {
                chooseCompatibleIme(imePickerAllowed = true)
            }
        }.launchIn(coroutineScope)

        devicesAdapter.onInputDeviceDisconnect.onEach { device ->
            if (showImePickerOnBtConnect && devicesThatShowImePicker.contains(device.descriptor)) {
                inputMethodAdapter.showImePicker(fromForeground = false)
            }

            if (changeImeOnDeviceConnect && devicesThatToggleKeyboard.contains(device.descriptor)) {
                chooseIncompatibleIme(imePickerAllowed = true)
            }
        }.launchIn(coroutineScope)

        preferenceRepository.get(Keys.changeImeOnInputFocus).onEach {
            changeImeOnInputFocus = it ?: PreferenceDefaults.SHOW_TOAST_WHEN_AUTO_CHANGE_IME
        }.launchIn(coroutineScope)

        preferenceRepository.get(Keys.showToastWhenAutoChangingIme).onEach {
            showToast = it ?: false
        }.launchIn(coroutineScope)

        accessibilityServiceAdapter.eventReceiver.onEach { event ->
            when (event) {
                is ServiceEvent.OnInputFocusChange -> {
                    if (!changeImeOnInputFocus) {
                        return@onEach
                    }

                    if (event.isFocussed) {
                        Timber.d("Choose normal keyboard because got input focus")
                        chooseIncompatibleIme(imePickerAllowed = false)
                    } else {
                        Timber.d("Choose key mapper keyboard because lost input focus")
                        chooseCompatibleIme(imePickerAllowed = false)
                    }
                }

                else -> Unit
            }
        }.launchIn(coroutineScope)
    }

    private suspend fun chooseIncompatibleIme(imePickerAllowed: Boolean) {
        // only choose the keyboard if the correct one isn't already chosen
        if (!imeHelper.isCompatibleImeChosen()) {
            return
        }

        imeHelper.chooseLastUsedIncompatibleInputMethod()
            .onSuccess { ime ->
                if (showToast) {
                    val message =
                        resourceProvider.getString(R.string.toast_chose_keyboard, ime.label)
                    popupMessageAdapter.showPopupMessage(message)
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
                popupMessageAdapter.showPopupMessage(error.getFullMessage(resourceProvider))
            }
    }

    private suspend fun chooseCompatibleIme(imePickerAllowed: Boolean) {
        // only choose the keyboard if the correct one isn't already chosen
        if (imeHelper.isCompatibleImeChosen()) {
            return
        }

        imeHelper.chooseCompatibleInputMethod()
            .onSuccess { ime ->
                if (showToast) {
                    val message =
                        resourceProvider.getString(R.string.toast_chose_keyboard, ime.label)
                    popupMessageAdapter.showPopupMessage(message)
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
                popupMessageAdapter.showPopupMessage(error.getFullMessage(resourceProvider))
            }
    }
}
