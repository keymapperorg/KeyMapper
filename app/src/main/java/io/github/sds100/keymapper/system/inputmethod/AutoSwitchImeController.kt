package io.github.sds100.keymapper.system.inputmethod

import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.mappings.PauseMappingsUseCase
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

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
    private val resourceProvider: ResourceProvider
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
        false
    )

    init {
        pauseMappingsUseCase.isPaused.onEach { isPaused ->

            if (!toggleKeyboardOnToggleKeymaps) return@onEach

            if (isPaused) {
                imeHelper.chooseLastUsedIncompatibleInputMethod().otherwise {
                    inputMethodAdapter.showImePicker(fromForeground = false)
                }
            } else {
                imeHelper.chooseCompatibleInputMethod().otherwise {
                    inputMethodAdapter.showImePicker(fromForeground = false)
                }
            }
        }.launchIn(coroutineScope)

        devicesAdapter.onInputDeviceConnect.onEach { device ->
            if (showImePickerOnBtConnect && devicesThatShowImePicker.contains(device.descriptor)) {
                inputMethodAdapter.showImePicker(fromForeground = false)
            }

            if (changeImeOnDeviceConnect && devicesThatToggleKeyboard.contains(device.descriptor)) {
                imeHelper.chooseCompatibleInputMethod()
                    .onSuccess { ime ->
                        val message =
                            resourceProvider.getString(R.string.toast_chose_keyboard, ime.label)
                        popupMessageAdapter.showPopupMessage(message)
                    }
                    .otherwise {
                        inputMethodAdapter.showImePicker(fromForeground = false)
                    }
                    .onFailure { error ->
                        popupMessageAdapter.showPopupMessage(error.getFullMessage(resourceProvider))
                    }
            }
        }.launchIn(coroutineScope)

        devicesAdapter.onInputDeviceDisconnect.onEach { device ->
            if (showImePickerOnBtConnect && devicesThatShowImePicker.contains(device.descriptor)) {
                inputMethodAdapter.showImePicker(fromForeground = false)
            }

            if (changeImeOnDeviceConnect && devicesThatToggleKeyboard.contains(device.descriptor)) {
                imeHelper.chooseLastUsedIncompatibleInputMethod()
                    .onSuccess { ime ->
                        val message =
                            resourceProvider.getString(R.string.toast_chose_keyboard, ime.label)
                        popupMessageAdapter.showPopupMessage(message)
                    }
                    .otherwise {
                        inputMethodAdapter.showImePicker(fromForeground = false)
                    }
                    .onFailure { error ->
                        popupMessageAdapter.showPopupMessage(error.getFullMessage(resourceProvider))
                    }
            }
        }.launchIn(coroutineScope)
    }
}