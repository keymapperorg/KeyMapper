package io.github.sds100.keymapper.actions

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.actions.system.SystemActionId
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.accessibility.AccessibilityNodeAction
import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.airplanemode.AirplaneModeAdapter
import io.github.sds100.keymapper.system.apps.AppShortcutAdapter
import io.github.sds100.keymapper.system.apps.PackageManagerAdapter
import io.github.sds100.keymapper.system.bluetooth.BluetoothAdapter
import io.github.sds100.keymapper.system.camera.CameraAdapter
import io.github.sds100.keymapper.system.devices.DevicesAdapter
import io.github.sds100.keymapper.system.display.DisplayAdapter
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.system.files.FileAdapter
import io.github.sds100.keymapper.system.files.FileUtils
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.inputmethod.KeyMapperImeMessenger
import io.github.sds100.keymapper.system.intents.IntentAdapter
import io.github.sds100.keymapper.system.lock.LockScreenAdapter
import io.github.sds100.keymapper.system.media.MediaAdapter
import io.github.sds100.keymapper.system.navigation.OpenMenuHelper
import io.github.sds100.keymapper.system.network.NetworkAdapter
import io.github.sds100.keymapper.system.nfc.NfcAdapter
import io.github.sds100.keymapper.system.phone.PhoneAdapter
import io.github.sds100.keymapper.system.popup.PopupMessageAdapter
import io.github.sds100.keymapper.system.root.SuAdapter
import io.github.sds100.keymapper.system.shell.ShellAdapter
import io.github.sds100.keymapper.system.url.OpenUrlAdapter
import io.github.sds100.keymapper.system.volume.RingerMode
import io.github.sds100.keymapper.system.volume.VolumeAdapter
import io.github.sds100.keymapper.system.volume.VolumeStream
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import splitties.bitflags.withFlag
import timber.log.Timber

/**
 * Created by sds100 on 14/02/21.
 */

class PerformActionsUseCaseImpl(
    private val coroutineScope: CoroutineScope,
    private val accessibilityService: IAccessibilityService,
    private val inputMethodAdapter: InputMethodAdapter,
    private val fileAdapter: FileAdapter,
    private val suAdapter: SuAdapter,
    private val shellAdapter: ShellAdapter,
    private val intentAdapter: IntentAdapter,
    private val getActionError: GetActionErrorUseCase,
    private val keyMapperImeMessenger: KeyMapperImeMessenger,
    private val packageManagerAdapter: PackageManagerAdapter,
    private val appShortcutAdapter: AppShortcutAdapter,
    private val popupMessageAdapter: PopupMessageAdapter,
    private val deviceAdapter: DevicesAdapter,
    private val phoneAdapter: PhoneAdapter,
    private val volumeAdapter: VolumeAdapter,
    private val cameraAdapter: CameraAdapter,
    private val displayAdapter: DisplayAdapter,
    private val lockScreenAdapter: LockScreenAdapter,
    private val mediaAdapter: MediaAdapter,
    private val airplaneModeAdapter: AirplaneModeAdapter,
    private val networkAdapter: NetworkAdapter,
    private val bluetoothAdapter: BluetoothAdapter,
    private val nfcAdapter: NfcAdapter,
    private val openUrlAdapter: OpenUrlAdapter,
    private val resourceProvider: ResourceProvider,
    private val preferenceRepository: PreferenceRepository,
    private val soundsManager: SoundsManager
) : PerformActionsUseCase {

    private val openMenuHelper by lazy { OpenMenuHelper(suAdapter, accessibilityService) }

    override fun perform(
        action: ActionData,
        inputEventType: InputEventType,
        keyMetaState: Int
    ) {
        /**
         * Is null if the action is being performed asynchronously
         */
        val result: Result<*>?

        when (action) {
            is OpenAppAction -> {
                result = packageManagerAdapter.openApp(action.packageName)
            }
            is OpenAppShortcutAction -> {
                result = appShortcutAdapter.launchShortcut(action.uri)
            }
            is IntentAction -> {
                result = intentAdapter.send(action.target, action.uri)
            }

            is KeyEventAction -> {

                if (action.useShell) {
                    result = suAdapter.execute("input keyevent ${action.keyCode}")
                } else {
                    val deviceId: Int = getDeviceIdForKeyEventAction(action)

                    keyMapperImeMessenger.inputKeyEvent(
                        InputKeyModel(
                            keyCode = action.keyCode,
                            inputType = inputEventType,
                            metaState = keyMetaState.withFlag(action.metaState),
                            deviceId = deviceId
                        )
                    )

                    result = Success(Unit)
                }
            }

            is PhoneCallAction -> {
                result = phoneAdapter.startCall(action.number)
            }

            is EnableDndMode -> {
                result = volumeAdapter.enableDndMode(action.dndMode)
            }
            is ToggleDndMode -> {
                result = if (volumeAdapter.isDndEnabled()) {
                    volumeAdapter.disableDndMode()
                } else {
                    volumeAdapter.enableDndMode(action.dndMode)
                }
            }
            is ChangeRingerModeSystemAction -> {
                result = volumeAdapter.setRingerMode(action.ringerMode)
            }

            is ControlMediaForAppSystemAction.FastForward -> {
                result = mediaAdapter.fastForward(action.packageName)
            }
            is ControlMediaForAppSystemAction.NextTrack -> {
                result = mediaAdapter.nextTrack(action.packageName)
            }
            is ControlMediaForAppSystemAction.Pause -> {
                result = mediaAdapter.pause(action.packageName)
            }
            is ControlMediaForAppSystemAction.Play -> {
                result = mediaAdapter.play(action.packageName)
            }
            is ControlMediaForAppSystemAction.PlayPause -> {
                result = mediaAdapter.playPause(action.packageName)
            }
            is ControlMediaForAppSystemAction.PreviousTrack -> {
                result = mediaAdapter.previousTrack(action.packageName)
            }
            is ControlMediaForAppSystemAction.Rewind -> {
                result = mediaAdapter.rewind(action.packageName)
            }

            is CycleRotationsSystemAction -> {
                result = displayAdapter.disableAutoRotate().then {
                    val currentOrientation = displayAdapter.orientation

                    val index = action.orientations.indexOf(currentOrientation)

                    val nextOrientation = if (index == action.orientations.lastIndex) {
                        action.orientations[0]
                    } else {
                        action.orientations[index + 1]
                    }

                    displayAdapter.setOrientation(nextOrientation)
                }
            }

            is FlashlightSystemAction.Disable -> {
                result = cameraAdapter.disableFlashlight(action.lens)
            }

            is FlashlightSystemAction.Enable -> {
                result = cameraAdapter.enableFlashlight(action.lens)
            }

            is FlashlightSystemAction.Toggle -> {
                result = cameraAdapter.toggleFlashlight(action.lens)
            }

            is SwitchKeyboardSystemAction -> {
                coroutineScope.launch {
                    inputMethodAdapter.chooseIme(
                        action.imeId,
                        fromForeground = false
                    ).onSuccess {
                        val message = resourceProvider.getString(
                            R.string.toast_chose_keyboard,
                            it.label
                        )
                        popupMessageAdapter.showPopupMessage(message)
                    }.showErrorMessageOnFail()
                }

                result = null
            }

            is VolumeSystemAction.Down -> {
                result = volumeAdapter.lowerVolume(showVolumeUi = action.showVolumeUi)
            }
            is VolumeSystemAction.Up -> {
                result = volumeAdapter.raiseVolume(showVolumeUi = action.showVolumeUi)
            }

            is VolumeSystemAction.Mute -> {
                result = volumeAdapter.muteVolume(showVolumeUi = action.showVolumeUi)
            }

            is VolumeSystemAction.Stream.Decrease -> {
                result = volumeAdapter.lowerVolume(
                    stream = action.volumeStream,
                    showVolumeUi = action.showVolumeUi
                )
            }

            is VolumeSystemAction.Stream.Increase -> {
                result = volumeAdapter.raiseVolume(
                    stream = action.volumeStream,
                    showVolumeUi = action.showVolumeUi
                )
            }

            is VolumeSystemAction.ToggleMute -> {
                result = volumeAdapter.toggleMuteVolume(showVolumeUi = action.showVolumeUi)
            }

            is VolumeSystemAction.UnMute -> {
                result = volumeAdapter.unmuteVolume(showVolumeUi = action.showVolumeUi)
            }

            is TapCoordinateAction -> {
                result = accessibilityService.tapScreen(action.x, action.y, inputEventType)
            }

            is TextAction -> {
                keyMapperImeMessenger.inputText(action.text)
                result = Success(Unit)
            }

            is SimpleSystemAction -> {
                when (action.id) {
                    SystemActionId.TOGGLE_WIFI -> {
                        result = if (networkAdapter.isWifiEnabled()) {
                            networkAdapter.disableWifi()
                        } else {
                            networkAdapter.enableWifi()
                        }
                    }
                    SystemActionId.ENABLE_WIFI -> {
                        result = networkAdapter.enableWifi()
                    }
                    SystemActionId.DISABLE_WIFI -> {
                        result = networkAdapter.disableWifi()
                    }

                    SystemActionId.TOGGLE_BLUETOOTH -> {
                        result = if (bluetoothAdapter.isBluetoothEnabled.firstBlocking()) {
                            bluetoothAdapter.disable()
                        } else {
                            bluetoothAdapter.enable()
                        }
                    }
                    SystemActionId.ENABLE_BLUETOOTH -> {
                        result = bluetoothAdapter.enable()
                    }

                    SystemActionId.DISABLE_BLUETOOTH -> {
                        result = bluetoothAdapter.disable()
                    }

                    SystemActionId.TOGGLE_MOBILE_DATA -> {
                        result = if (networkAdapter.isMobileDataEnabled()) {
                            networkAdapter.disableMobileData()
                        } else {
                            networkAdapter.enableMobileData()
                        }
                    }
                    SystemActionId.ENABLE_MOBILE_DATA -> {
                        result = networkAdapter.enableMobileData()
                    }
                    SystemActionId.DISABLE_MOBILE_DATA -> {
                        result = networkAdapter.disableMobileData()
                    }

                    SystemActionId.TOGGLE_AUTO_BRIGHTNESS -> {
                        result = if (displayAdapter.isAutoBrightnessEnabled()) {
                            displayAdapter.disableAutoBrightness()
                        } else {
                            displayAdapter.enableAutoBrightness()
                        }
                    }
                    SystemActionId.DISABLE_AUTO_BRIGHTNESS -> {
                        result = displayAdapter.disableAutoBrightness()
                    }
                    SystemActionId.ENABLE_AUTO_BRIGHTNESS -> {
                        result = displayAdapter.enableAutoBrightness()
                    }
                    SystemActionId.INCREASE_BRIGHTNESS -> {
                        result = displayAdapter.increaseBrightness()
                    }
                    SystemActionId.DECREASE_BRIGHTNESS -> {
                        result = displayAdapter.decreaseBrightness()
                    }

                    SystemActionId.TOGGLE_AUTO_ROTATE -> {
                        result = if (displayAdapter.isAutoRotateEnabled()) {
                            displayAdapter.disableAutoRotate()
                        } else {
                            displayAdapter.enableAutoRotate()
                        }
                    }

                    SystemActionId.ENABLE_AUTO_ROTATE -> {
                        result = displayAdapter.enableAutoRotate()
                    }

                    SystemActionId.DISABLE_AUTO_ROTATE -> {
                        result = displayAdapter.disableAutoRotate()
                    }

                    SystemActionId.PORTRAIT_MODE -> {
                        displayAdapter.disableAutoRotate()
                        result = displayAdapter.setOrientation(Orientation.ORIENTATION_0)
                    }

                    SystemActionId.LANDSCAPE_MODE -> {
                        displayAdapter.disableAutoRotate()
                        result = displayAdapter.setOrientation(Orientation.ORIENTATION_90)
                    }

                    SystemActionId.SWITCH_ORIENTATION -> {
                        if (displayAdapter.orientation == Orientation.ORIENTATION_180
                            || displayAdapter.orientation == Orientation.ORIENTATION_0
                        ) {
                            result = displayAdapter.setOrientation(Orientation.ORIENTATION_90)
                        } else {
                            result = displayAdapter.setOrientation(Orientation.ORIENTATION_0)
                        }
                    }

                    SystemActionId.CYCLE_RINGER_MODE -> {
                        result = when (volumeAdapter.ringerMode) {
                            RingerMode.NORMAL -> volumeAdapter.setRingerMode(RingerMode.VIBRATE)
                            RingerMode.VIBRATE -> volumeAdapter.setRingerMode(RingerMode.SILENT)
                            RingerMode.SILENT -> volumeAdapter.setRingerMode(RingerMode.NORMAL)
                        }
                    }

                    SystemActionId.CYCLE_VIBRATE_RING -> {
                        result = when (volumeAdapter.ringerMode) {
                            RingerMode.NORMAL -> volumeAdapter.setRingerMode(RingerMode.VIBRATE)
                            RingerMode.VIBRATE -> volumeAdapter.setRingerMode(RingerMode.NORMAL)
                            RingerMode.SILENT -> volumeAdapter.setRingerMode(RingerMode.NORMAL)
                        }
                    }

                    SystemActionId.DISABLE_DND_MODE -> {
                        result = volumeAdapter.disableDndMode()
                    }

                    SystemActionId.EXPAND_NOTIFICATION_DRAWER -> {
                        val globalAction = AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS

                        result = accessibilityService.doGlobalAction(globalAction).otherwise {
                            shellAdapter.execute("cmd statusbar expand-notifications")
                        }
                    }

                    SystemActionId.TOGGLE_NOTIFICATION_DRAWER -> {
                        result =
                            if (accessibilityService.rootNode?.packageName == "com.android.systemui") {
                                shellAdapter.execute("cmd statusbar collapse")
                            } else {
                                val globalAction = AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS

                                accessibilityService.doGlobalAction(globalAction).otherwise {
                                    shellAdapter.execute("cmd statusbar expand-notifications")
                                }
                            }
                    }

                    SystemActionId.EXPAND_QUICK_SETTINGS -> {
                        val globalAction = AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS

                        result =
                            accessibilityService.doGlobalAction(globalAction).otherwise {
                                shellAdapter.execute("cmd statusbar expand-settings")
                            }
                    }

                    SystemActionId.TOGGLE_QUICK_SETTINGS -> {
                        result =
                            if (accessibilityService.rootNode?.packageName == "com.android.systemui") {
                                shellAdapter.execute("cmd statusbar collapse")
                            } else {
                                val globalAction = AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS

                                accessibilityService.doGlobalAction(globalAction).otherwise {
                                    shellAdapter.execute("cmd statusbar expand-settings")
                                }
                            }
                    }

                    SystemActionId.COLLAPSE_STATUS_BAR -> {
                        result = shellAdapter.execute("cmd statusbar collapse")
                    }

                    SystemActionId.PAUSE_MEDIA -> {
                        result = mediaAdapter.pause()
                    }
                    SystemActionId.PLAY_MEDIA -> {
                        result = mediaAdapter.play()
                    }
                    SystemActionId.PLAY_PAUSE_MEDIA -> {
                        result = mediaAdapter.playPause()
                    }
                    SystemActionId.NEXT_TRACK -> {
                        result = mediaAdapter.nextTrack()
                    }
                    SystemActionId.PREVIOUS_TRACK -> {
                        result = mediaAdapter.previousTrack()
                    }
                    SystemActionId.FAST_FORWARD -> {
                        result = mediaAdapter.fastForward()
                    }
                    SystemActionId.REWIND -> {
                        result = mediaAdapter.rewind()
                    }

                    SystemActionId.GO_BACK -> {
                        result =
                            accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
                    }
                    SystemActionId.GO_HOME -> {
                        result =
                            accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
                    }

                    SystemActionId.OPEN_RECENTS -> {
                        result =
                            accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                    }

                    SystemActionId.TOGGLE_SPLIT_SCREEN -> {
                        result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                            accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                        } else {
                            Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.N)
                        }
                    }

                    SystemActionId.GO_LAST_APP -> {
                        coroutineScope.launch {
                            accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                            delay(100)
                            accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                                .showErrorMessageOnFail()
                        }

                        result = null
                    }

                    SystemActionId.OPEN_MENU -> {
                        result = openMenuHelper.openMenu()
                    }

                    SystemActionId.ENABLE_NFC -> {
                        result = nfcAdapter.enable()
                    }
                    SystemActionId.DISABLE_NFC -> {
                        result = nfcAdapter.disable()
                    }
                    SystemActionId.TOGGLE_NFC -> {
                        result = if (nfcAdapter.isEnabled()) {
                            nfcAdapter.disable()
                        } else {
                            nfcAdapter.enable()
                        }
                    }

                    SystemActionId.MOVE_CURSOR_TO_END -> {
                        keyMapperImeMessenger.inputKeyEvent(
                            InputKeyModel(
                                keyCode = KeyEvent.KEYCODE_MOVE_END,
                                metaState = KeyEvent.META_CTRL_ON,
                            )
                        )

                        result = Success(Unit)
                    }

                    SystemActionId.TOGGLE_KEYBOARD -> {
                        val isHidden = accessibilityService.isKeyboardHidden.firstBlocking()
                        if (isHidden) {
                            accessibilityService.showKeyboard()
                        } else {
                            accessibilityService.hideKeyboard()
                        }

                        result = Success(Unit)
                    }


                    SystemActionId.SHOW_KEYBOARD -> {
                        accessibilityService.showKeyboard()
                        result = Success(Unit)
                    }

                    SystemActionId.HIDE_KEYBOARD -> {
                        accessibilityService.hideKeyboard()
                        result = Success(Unit)
                    }

                    SystemActionId.SHOW_KEYBOARD_PICKER -> {
                        result = inputMethodAdapter.showImePicker(fromForeground = false)
                    }

                    SystemActionId.TEXT_CUT -> {
                        result = accessibilityService.performActionOnNode({ it.isFocused }) {
                            AccessibilityNodeAction(AccessibilityNodeInfo.ACTION_CUT)
                        }
                    }

                    SystemActionId.TEXT_COPY -> {
                        result = accessibilityService.performActionOnNode({ it.isFocused }) {
                            AccessibilityNodeAction(AccessibilityNodeInfo.ACTION_COPY)
                        }
                    }
                    SystemActionId.TEXT_PASTE -> {
                        result = accessibilityService.performActionOnNode({ it.isFocused }) {
                            AccessibilityNodeAction(AccessibilityNodeInfo.ACTION_PASTE)
                        }
                    }

                    SystemActionId.SELECT_WORD_AT_CURSOR -> {
                        result = accessibilityService.performActionOnNode({ it.isFocused }) {
                            //it is at the cursor position if they both return the same value
                            if (it.textSelectionStart == it.textSelectionEnd) {
                                val cursorPosition = it.textSelectionStart

                                val wordBoundary =
                                    it.text.toString().getWordBoundaries(cursorPosition)
                                        ?: return@performActionOnNode null

                                val extras = mapOf(
                                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT to wordBoundary.first,

                                    //The index of the cursor is the index of the last char in the word + 1
                                    AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT to wordBoundary.second + 1
                                )

                                AccessibilityNodeAction(
                                    AccessibilityNodeInfo.ACTION_SET_SELECTION,
                                    extras
                                )
                            } else {
                                null
                            }

                        }
                    }

                    SystemActionId.TOGGLE_AIRPLANE_MODE -> {
                        result = if (airplaneModeAdapter.isEnabled()) {
                            airplaneModeAdapter.disable()
                        } else {
                            airplaneModeAdapter.enable()
                        }
                    }
                    SystemActionId.ENABLE_AIRPLANE_MODE -> {
                        result = airplaneModeAdapter.enable()
                    }
                    SystemActionId.DISABLE_AIRPLANE_MODE -> {
                        result = airplaneModeAdapter.disable()
                    }

                    SystemActionId.SCREENSHOT -> {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                            coroutineScope.launch {
                                val picturesFolder = fileAdapter.getPicturesFolder()
                                val screenshotsFolder = "$picturesFolder/Screenshots"
                                val fileDate = FileUtils.createFileDate()

                                suAdapter.execute("mkdir -p $screenshotsFolder; screencap -p $screenshotsFolder/Screenshot_$fileDate.png")
                                    .onSuccess {
                                        popupMessageAdapter.showPopupMessage(
                                            resourceProvider.getString(
                                                R.string.toast_screenshot_taken
                                            )
                                        )
                                    }.showErrorMessageOnFail()
                            }
                            result = null

                        } else {
                            result =
                                accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                        }
                    }

                    SystemActionId.OPEN_VOICE_ASSISTANT -> {
                        result = packageManagerAdapter.launchVoiceAssistant()
                    }
                    SystemActionId.OPEN_DEVICE_ASSISTANT -> {
                        result = packageManagerAdapter.launchDeviceAssistant()
                    }
                    SystemActionId.OPEN_CAMERA -> {
                        result = packageManagerAdapter.launchCameraApp()
                    }

                    SystemActionId.LOCK_DEVICE -> {
                        result = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                            suAdapter.execute("input keyevent ${KeyEvent.KEYCODE_POWER}")
                        } else {
                            accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                        }
                    }

                    SystemActionId.POWER_ON_OFF_DEVICE -> {
                        result = suAdapter.execute("input keyevent ${KeyEvent.KEYCODE_POWER}")
                    }

                    SystemActionId.SECURE_LOCK_DEVICE -> {
                        result = lockScreenAdapter.secureLockDevice()
                    }

                    SystemActionId.CONSUME_KEY_EVENT -> {
                        result = Success(Unit)
                    }

                    SystemActionId.OPEN_SETTINGS -> {
                        result = packageManagerAdapter.launchSettingsApp()
                    }

                    SystemActionId.SHOW_POWER_MENU -> {
                        result =
                            accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
                    }

                    SystemActionId.VOLUME_SHOW_DIALOG -> {
                        result = volumeAdapter.showVolumeUi()
                    }

                    else -> throw Exception("Don't know how to perform this action ${action.id}")
                }
            }

            is UrlAction -> {
                result = openUrlAdapter.openUrl(action.url)
            }

            is SoundAction -> {
                result = soundsManager.getSoundUri(action.soundUid).then { uri ->
                    mediaAdapter.playSoundFile(uri, VolumeStream.ACCESSIBILITY)
                }
            }

            CorruptAction -> {
                result = Error.CorruptActionError
            }
        }

        when (result) {
            null, is Success -> Timber.d("Performed action $action, input event type: $inputEventType, key meta state: $keyMetaState")
            is Error -> Timber.d("Failed to perform action $action, reason: ${result.getFullMessage(resourceProvider)}, action: $action, input event type: $inputEventType, key meta state: $keyMetaState")
        }

        result?.showErrorMessageOnFail()
    }

    override fun getError(action: ActionData): Error? {
        return getActionError.getError(action)
    }

    override val defaultRepeatDelay: Flow<Long> =
        preferenceRepository.get(Keys.defaultRepeatDelay)
            .map { it ?: PreferenceDefaults.REPEAT_DELAY }
            .map { it.toLong() }

    override val defaultRepeatRate: Flow<Long> =
        preferenceRepository.get(Keys.defaultRepeatRate)
            .map { it ?: PreferenceDefaults.REPEAT_RATE }
            .map { it.toLong() }

    override val defaultHoldDownDuration: Flow<Long> =
        preferenceRepository.get(Keys.defaultHoldDownDuration)
            .map { it ?: PreferenceDefaults.HOLD_DOWN_DURATION }
            .map { it.toLong() }

    private fun getDeviceIdForKeyEventAction(action: KeyEventAction): Int {
        if (action.device?.descriptor == null) {
            return -1
        }

        val inputDevices = deviceAdapter.connectedInputDevices.value

        val devicesWithSameDescriptor =
            inputDevices.dataOrNull()
                ?.filter { it.descriptor == action.device.descriptor }
                ?: emptyList()

        if (devicesWithSameDescriptor.isEmpty()) {
            return -1
        }

        if (devicesWithSameDescriptor.size == 1) {
            return devicesWithSameDescriptor[0].id
        }

        /*
        if there are multiple devices use the device that supports the key
        code. if none do then use the first one
         */
        val deviceThatHasKey = devicesWithSameDescriptor.singleOrNull {
            deviceAdapter.deviceHasKey(it.id, action.keyCode)
        }

        val device = deviceThatHasKey
            ?: devicesWithSameDescriptor.singleOrNull { it.name == action.device.name }
            ?: devicesWithSameDescriptor[0]

        return device.id
    }

    private fun Result<*>.showErrorMessageOnFail() {
        onFailure {
            popupMessageAdapter.showPopupMessage(it.getFullMessage(resourceProvider))
        }
    }
}

interface PerformActionsUseCase {
    val defaultHoldDownDuration: Flow<Long>
    val defaultRepeatDelay: Flow<Long>
    val defaultRepeatRate: Flow<Long>

    fun perform(
        action: ActionData,
        inputEventType: InputEventType = InputEventType.DOWN_UP,
        keyMetaState: Int = 0
    )

    fun getError(action: ActionData): Error?
}