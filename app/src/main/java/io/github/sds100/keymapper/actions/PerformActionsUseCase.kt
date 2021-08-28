package io.github.sds100.keymapper.actions

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.shizuku.InputEventInjector
import io.github.sds100.keymapper.system.accessibility.AccessibilityNodeAction
import io.github.sds100.keymapper.system.accessibility.IAccessibilityService
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
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
import io.github.sds100.keymapper.system.permissions.Permission
import io.github.sds100.keymapper.system.permissions.PermissionAdapter
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
    private val shizukuInputEventInjector: InputEventInjector,
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
    private val soundsManager: SoundsManager,
    private val permissionAdapter: PermissionAdapter,
    private val notificationReceiverAdapter: ServiceAdapter
) : PerformActionsUseCase {

    private val openMenuHelper by lazy { OpenMenuHelper(suAdapter, accessibilityService) }

    override fun perform(
        action: ActionData,
        inputEventType: InputEventType,
        keyMetaState: Int,
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
                val deviceId: Int = getDeviceIdForKeyEventAction(action)

                val model = InputKeyModel(
                    keyCode = action.keyCode,
                    inputType = inputEventType,
                    metaState = keyMetaState.withFlag(action.metaState),
                    deviceId = deviceId
                )

                result = when {
                    permissionAdapter.isGranted(Permission.SHIZUKU) -> {
                        shizukuInputEventInjector.inputKeyEvent(model)
                        Success(Unit)
                    }

                    action.useShell -> suAdapter.execute("input keyevent ${model.keyCode}")

                    else -> {
                        keyMapperImeMessenger.inputKeyEvent(model)

                        Success(Unit)
                    }
                }
            }

            is PhoneCallAction -> {
                result = phoneAdapter.startCall(action.number)
            }

            is DndModeAction.Enable -> {
                result = volumeAdapter.enableDndMode(action.dndMode)
            }
            is DndModeAction.Toggle -> {
                result = if (volumeAdapter.isDndEnabled()) {
                    volumeAdapter.disableDndMode()
                } else {
                    volumeAdapter.enableDndMode(action.dndMode)
                }
            }

            is VolumeAction.SetRingerMode -> {
                result = volumeAdapter.setRingerMode(action.ringerMode)
            }

            is ControlMediaForAppAction.FastForward -> {
                result = mediaAdapter.fastForward(action.packageName)
            }
            is ControlMediaForAppAction.NextTrack -> {
                result = mediaAdapter.nextTrack(action.packageName)
            }
            is ControlMediaForAppAction.Pause -> {
                result = mediaAdapter.pause(action.packageName)
            }
            is ControlMediaForAppAction.Play -> {
                result = mediaAdapter.play(action.packageName)
            }
            is ControlMediaForAppAction.PlayPause -> {
                result = mediaAdapter.playPause(action.packageName)
            }
            is ControlMediaForAppAction.PreviousTrack -> {
                result = mediaAdapter.previousTrack(action.packageName)
            }
            is ControlMediaForAppAction.Rewind -> {
                result = mediaAdapter.rewind(action.packageName)
            }

            is RotationAction.CycleRotations -> {
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

            is FlashlightAction.Disable -> {
                result = cameraAdapter.disableFlashlight(action.lens)
            }

            is FlashlightAction.Enable -> {
                result = cameraAdapter.enableFlashlight(action.lens)
            }

            is FlashlightAction.Toggle -> {
                result = cameraAdapter.toggleFlashlight(action.lens)
            }

            is SwitchKeyboardAction -> {
                coroutineScope.launch {
                    inputMethodAdapter
                        .chooseImeWithoutUserInput(action.imeId)
                        .onSuccess {
                            val message = resourceProvider.getString(
                                R.string.toast_chose_keyboard,
                                it.label
                            )
                            popupMessageAdapter.showPopupMessage(message)
                        }
                        .showErrorMessageOnFail()
                }

                result = null
            }

            is VolumeAction.Down -> {
                result = volumeAdapter.lowerVolume(showVolumeUi = action.showVolumeUi)
            }
            is VolumeAction.Up -> {
                result = volumeAdapter.raiseVolume(showVolumeUi = action.showVolumeUi)
            }

            is VolumeAction.Mute -> {
                result = volumeAdapter.muteVolume(showVolumeUi = action.showVolumeUi)
            }

            is VolumeAction.Stream.Decrease -> {
                result = volumeAdapter.lowerVolume(
                    stream = action.volumeStream,
                    showVolumeUi = action.showVolumeUi
                )
            }

            is VolumeAction.Stream.Increase -> {
                result = volumeAdapter.raiseVolume(
                    stream = action.volumeStream,
                    showVolumeUi = action.showVolumeUi
                )
            }

            is VolumeAction.ToggleMute -> {
                result = volumeAdapter.toggleMuteVolume(showVolumeUi = action.showVolumeUi)
            }

            is VolumeAction.UnMute -> {
                result = volumeAdapter.unmuteVolume(showVolumeUi = action.showVolumeUi)
            }

            is TapCoordinateAction -> {
                result = accessibilityService.tapScreen(action.x, action.y, inputEventType)
            }

            is SwipeGestureAction -> {
                result = accessibilityService.swipeScreen(action.path, inputEventType)
            }

            is TextAction -> {
                keyMapperImeMessenger.inputText(action.text)
                result = Success(Unit)
            }

            is UrlAction -> {
                result = openUrlAdapter.openUrl(action.url)
            }

            is SoundAction -> {
                result = soundsManager.getSound(action.soundUid).then { file ->
                    mediaAdapter.playSoundFile(file.uri, VolumeStream.ACCESSIBILITY)
                }
            }

            is WifiAction.Toggle -> {
                result = if (networkAdapter.isWifiEnabled()) {
                    networkAdapter.disableWifi()
                } else {
                    networkAdapter.enableWifi()
                }
            }

            is WifiAction.Enable -> {
                result = networkAdapter.enableWifi()
            }

            is WifiAction.Disable -> {
                result = networkAdapter.disableWifi()
            }

            is BluetoothAction.Toggle -> {
                result = if (bluetoothAdapter.isBluetoothEnabled.firstBlocking()) {
                    bluetoothAdapter.disable()
                } else {
                    bluetoothAdapter.enable()
                }
            }

            is BluetoothAction.Enable -> {
                result = bluetoothAdapter.enable()
            }

            is BluetoothAction.Disable -> {
                result = bluetoothAdapter.disable()
            }

            is MobileDataAction.Toggle -> {
                result = if (networkAdapter.isMobileDataEnabled()) {
                    networkAdapter.disableMobileData()
                } else {
                    networkAdapter.enableMobileData()
                }
            }

            is MobileDataAction.Enable -> {
                result = networkAdapter.enableMobileData()
            }

            is MobileDataAction.Disable -> {
                result = networkAdapter.disableMobileData()
            }

            is BrightnessAction.ToggleAuto -> {
                result = if (displayAdapter.isAutoBrightnessEnabled()) {
                    displayAdapter.disableAutoBrightness()
                } else {
                    displayAdapter.enableAutoBrightness()
                }
            }

            is BrightnessAction.DisableAuto -> {
                result = displayAdapter.disableAutoBrightness()
            }

            is BrightnessAction.EnableAuto -> {
                result = displayAdapter.enableAutoBrightness()
            }
            is BrightnessAction.Increase -> {
                result = displayAdapter.increaseBrightness()
            }
            is BrightnessAction.Decrease -> {
                result = displayAdapter.decreaseBrightness()
            }

            is RotationAction.ToggleAuto -> {
                result = if (displayAdapter.isAutoRotateEnabled()) {
                    displayAdapter.disableAutoRotate()
                } else {
                    displayAdapter.enableAutoRotate()
                }
            }

            is RotationAction.EnableAuto -> {
                result = displayAdapter.enableAutoRotate()
            }

            is RotationAction.DisableAuto -> {
                result = displayAdapter.disableAutoRotate()
            }

            is RotationAction.Portrait -> {
                displayAdapter.disableAutoRotate()
                result = displayAdapter.setOrientation(Orientation.ORIENTATION_0)
            }

            is RotationAction.Landscape -> {
                displayAdapter.disableAutoRotate()
                result = displayAdapter.setOrientation(Orientation.ORIENTATION_90)
            }

            is RotationAction.SwitchOrientation -> {
                if (displayAdapter.orientation == Orientation.ORIENTATION_180
                    || displayAdapter.orientation == Orientation.ORIENTATION_0
                ) {
                    result = displayAdapter.setOrientation(Orientation.ORIENTATION_90)
                } else {
                    result = displayAdapter.setOrientation(Orientation.ORIENTATION_0)
                }
            }

            is VolumeAction.CycleRingerMode -> {
                result = when (volumeAdapter.ringerMode) {
                    RingerMode.NORMAL -> volumeAdapter.setRingerMode(RingerMode.VIBRATE)
                    RingerMode.VIBRATE -> volumeAdapter.setRingerMode(RingerMode.SILENT)
                    RingerMode.SILENT -> volumeAdapter.setRingerMode(RingerMode.NORMAL)
                }
            }

            is VolumeAction.CycleVibrateRing -> {
                result = when (volumeAdapter.ringerMode) {
                    RingerMode.NORMAL -> volumeAdapter.setRingerMode(RingerMode.VIBRATE)
                    RingerMode.VIBRATE -> volumeAdapter.setRingerMode(RingerMode.NORMAL)
                    RingerMode.SILENT -> volumeAdapter.setRingerMode(RingerMode.NORMAL)
                }
            }

            is DndModeAction.Disable -> {
                result = volumeAdapter.disableDndMode()
            }

            is StatusBarAction.ExpandNotifications -> {
                val globalAction = AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS

                result = accessibilityService.doGlobalAction(globalAction).otherwise {
                    shellAdapter.execute("cmd statusbar expand-notifications")
                }
            }

            is StatusBarAction.ToggleNotifications -> {
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

            is StatusBarAction.ExpandQuickSettings -> {
                val globalAction = AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS

                result =
                    accessibilityService.doGlobalAction(globalAction).otherwise {
                        shellAdapter.execute("cmd statusbar expand-settings")
                    }
            }

            is StatusBarAction.ToggleQuickSettings -> {
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

            is StatusBarAction.Collapse -> {
                result = shellAdapter.execute("cmd statusbar collapse")
            }

            is ControlMediaAction.Pause -> {
                result = mediaAdapter.pause()
            }
            is ControlMediaAction.Play -> {
                result = mediaAdapter.play()
            }
            is ControlMediaAction.PlayPause -> {
                result = mediaAdapter.playPause()
            }
            is ControlMediaAction.NextTrack -> {
                result = mediaAdapter.nextTrack()
            }
            is ControlMediaAction.PreviousTrack -> {
                result = mediaAdapter.previousTrack()
            }
            is ControlMediaAction.FastForward -> {
                result = mediaAdapter.fastForward()
            }
            is ControlMediaAction.Rewind -> {
                result = mediaAdapter.rewind()
            }

            is GoBackAction -> {
                result =
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }
            is GoHomeAction -> {
                result =
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }

            is OpenRecentsAction -> {
                result =
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }

            is ToggleSplitScreenAction -> {
                result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                } else {
                    Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.N)
                }
            }

            is GoLastAppAction -> {
                coroutineScope.launch {
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                    delay(100)
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                        .showErrorMessageOnFail()
                }

                result = null
            }

            is OpenMenuAction -> {
                result = openMenuHelper.openMenu()
            }

            is NfcAction.Enable -> {
                result = nfcAdapter.enable()
            }
            is NfcAction.Disable -> {
                result = nfcAdapter.disable()
            }
            is NfcAction.Toggle -> {
                result = if (nfcAdapter.isEnabled()) {
                    nfcAdapter.disable()
                } else {
                    nfcAdapter.enable()
                }
            }

            is MoveCursorToEndAction -> {
                val keyModel = InputKeyModel(
                    keyCode = KeyEvent.KEYCODE_MOVE_END,
                    metaState = KeyEvent.META_CTRL_ON,
                )

                if (permissionAdapter.isGranted(Permission.SHIZUKU)) {
                    shizukuInputEventInjector.inputKeyEvent(keyModel)
                } else {
                    keyMapperImeMessenger.inputKeyEvent(keyModel)
                }

                result = Success(Unit)
            }

            is ToggleKeyboardAction -> {
                val isHidden = accessibilityService.isKeyboardHidden.firstBlocking()
                if (isHidden) {
                    accessibilityService.showKeyboard()
                } else {
                    accessibilityService.hideKeyboard()
                }

                result = Success(Unit)
            }


            is ShowKeyboardAction -> {
                accessibilityService.showKeyboard()
                result = Success(Unit)
            }

            is HideKeyboardAction -> {
                accessibilityService.hideKeyboard()
                result = Success(Unit)
            }

            is ShowKeyboardPickerAction -> {
                result = inputMethodAdapter.showImePicker(fromForeground = false)
            }

            is CutTextAction -> {
                result = accessibilityService.performActionOnNode({ it.isFocused }) {
                    AccessibilityNodeAction(AccessibilityNodeInfo.ACTION_CUT)
                }
            }

            is CopyTextAction -> {
                result = accessibilityService.performActionOnNode({ it.isFocused }) {
                    AccessibilityNodeAction(AccessibilityNodeInfo.ACTION_COPY)
                }
            }
            is PasteTextAction -> {
                result = accessibilityService.performActionOnNode({ it.isFocused }) {
                    AccessibilityNodeAction(AccessibilityNodeInfo.ACTION_PASTE)
                }
            }

            is SelectWordAtCursorAction -> {
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

            is AirplaneModeAction.Toggle -> {
                result = if (airplaneModeAdapter.isEnabled()) {
                    airplaneModeAdapter.disable()
                } else {
                    airplaneModeAdapter.enable()
                }
            }
            is AirplaneModeAction.Enable -> {
                result = airplaneModeAdapter.enable()
            }
            is AirplaneModeAction.Disable -> {
                result = airplaneModeAdapter.disable()
            }

            is ScreenshotAction -> {
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

            is VoiceAssistantAction -> {
                result = packageManagerAdapter.launchVoiceAssistant()
            }
            is DeviceAssistantAction -> {
                result = packageManagerAdapter.launchDeviceAssistant()
            }
            is OpenCameraAction -> {
                result = packageManagerAdapter.launchCameraApp()
            }

            is LockDeviceAction -> {
                result = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    suAdapter.execute("input keyevent ${KeyEvent.KEYCODE_POWER}")
                } else {
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                }
            }

            is ScreenOnOffAction -> {
                result = suAdapter.execute("input keyevent ${KeyEvent.KEYCODE_POWER}")
            }

            is SecureLockAction -> {
                result = lockScreenAdapter.secureLockDevice()
            }

            is ConsumeKeyEventAction -> {
                result = Success(Unit)
            }

            is OpenSettingsAction -> {
                result = packageManagerAdapter.launchSettingsApp()
            }

            is ShowPowerMenuAction -> {
                result =
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            }

            is VolumeAction.ShowDialog -> {
                result = volumeAdapter.showVolumeUi()
            }

            DismissAllNotificationsAction -> {
                coroutineScope.launch {
                    notificationReceiverAdapter.send(Event.DismissAllNotifications)
                }

                result = null
            }

            DismissLastNotificationAction -> {
                coroutineScope.launch {
                    notificationReceiverAdapter.send(Event.DismissLastNotification)
                }

                result = null
            }
        }

        when (result) {
            null, is Success -> Timber.d("Performed action $action, input event type: $inputEventType, key meta state: $keyMetaState")
            is Error -> Timber.d(
                "Failed to perform action $action, reason: ${result.getFullMessage(resourceProvider)}, action: $action, input event type: $inputEventType, key meta state: $keyMetaState"
            )
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