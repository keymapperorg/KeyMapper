package io.github.sds100.keymapper.actions

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.view.InputDevice
import android.view.KeyEvent
import android.view.accessibility.AccessibilityNodeInfo
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.sound.SoundsManager
import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.PreferenceDefaults
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.system.accessibility.AccessibilityNodeAction
import io.github.sds100.keymapper.system.accessibility.AccessibilityNodeModel
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
import io.github.sds100.keymapper.system.inputevents.InputEventInjector
import io.github.sds100.keymapper.system.inputevents.InputEventUtils
import io.github.sds100.keymapper.system.inputmethod.ImeInputEventInjector
import io.github.sds100.keymapper.system.inputmethod.InputKeyModel
import io.github.sds100.keymapper.system.inputmethod.InputMethodAdapter
import io.github.sds100.keymapper.system.intents.IntentAdapter
import io.github.sds100.keymapper.system.intents.IntentTarget
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
import io.github.sds100.keymapper.util.Error
import io.github.sds100.keymapper.util.InputEventType
import io.github.sds100.keymapper.util.Result
import io.github.sds100.keymapper.util.ServiceEvent
import io.github.sds100.keymapper.util.Success
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.firstBlocking
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.getWordBoundaries
import io.github.sds100.keymapper.util.ifIsData
import io.github.sds100.keymapper.util.onFailure
import io.github.sds100.keymapper.util.onSuccess
import io.github.sds100.keymapper.util.otherwise
import io.github.sds100.keymapper.util.success
import io.github.sds100.keymapper.util.then
import io.github.sds100.keymapper.util.ui.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val imeInputEventInjector: ImeInputEventInjector,
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
    private val notificationReceiverAdapter: ServiceAdapter,
) : PerformActionsUseCase {

    private val openMenuHelper by lazy {
        OpenMenuHelper(
            suAdapter,
            accessibilityService,
            shizukuInputEventInjector,
            permissionAdapter,
            coroutineScope,
        )
    }

    /**
     * Cache this so we aren't checking every time a key event must be inputted.
     */
    private val inputKeyEventsWithShizuku: StateFlow<Boolean> =
        permissionAdapter.isGrantedFlow(Permission.SHIZUKU)
            .stateIn(coroutineScope, SharingStarted.Eagerly, false)

    override suspend fun perform(
        action: ActionData,
        inputEventType: InputEventType,
        keyMetaState: Int,
    ) {
        /**
         * Is null if the action is being performed asynchronously
         */
        val result: Result<*>

        when (action) {
            is ActionData.App -> {
                result = packageManagerAdapter.openApp(action.packageName)
            }

            is ActionData.AppShortcut -> {
                result = appShortcutAdapter.launchShortcut(action.uri)
            }

            is ActionData.Intent -> {
                result = intentAdapter.send(action.target, action.uri, action.extras)
            }

            is ActionData.InputKeyEvent -> {
                val deviceId: Int = getDeviceIdForKeyEventAction(action)

                // See issue #1683. Some apps ignore key events which do not have a source.
                val source = when {
                    InputEventUtils.isDpadKeyCode(action.keyCode) -> InputDevice.SOURCE_DPAD
                    InputEventUtils.isGamepadButton(action.keyCode) -> InputDevice.SOURCE_GAMEPAD
                    else -> InputDevice.SOURCE_KEYBOARD
                }

                val model = InputKeyModel(
                    keyCode = action.keyCode,
                    inputType = inputEventType,
                    metaState = keyMetaState.withFlag(action.metaState),
                    deviceId = deviceId,
                    source = source,
                )

                result = when {
                    inputKeyEventsWithShizuku.value -> {
                        shizukuInputEventInjector.inputKeyEvent(model)
                        Success(Unit)
                    }

                    action.useShell -> suAdapter.execute("input keyevent ${model.keyCode}")

                    else -> {
                        imeInputEventInjector.inputKeyEvent(model)

                        Success(Unit)
                    }
                }
            }

            is ActionData.PhoneCall -> {
                result = phoneAdapter.startCall(action.number)
            }

            is ActionData.DoNotDisturb.Enable -> {
                result = volumeAdapter.enableDndMode(action.dndMode)
            }

            is ActionData.DoNotDisturb.Toggle -> {
                result = if (volumeAdapter.isDndEnabled()) {
                    volumeAdapter.disableDndMode()
                } else {
                    volumeAdapter.enableDndMode(action.dndMode)
                }
            }

            is ActionData.Volume.SetRingerMode -> {
                result = volumeAdapter.setRingerMode(action.ringerMode)
            }

            is ActionData.ControlMediaForApp.FastForward -> {
                result = mediaAdapter.fastForward(action.packageName)
            }

            is ActionData.ControlMediaForApp.NextTrack -> {
                result = mediaAdapter.nextTrack(action.packageName)
            }

            is ActionData.ControlMediaForApp.Pause -> {
                result = mediaAdapter.pause(action.packageName)
            }

            is ActionData.ControlMediaForApp.Play -> {
                result = mediaAdapter.play(action.packageName)
            }

            is ActionData.ControlMediaForApp.PlayPause -> {
                result = mediaAdapter.playPause(action.packageName)
            }

            is ActionData.ControlMediaForApp.PreviousTrack -> {
                result = mediaAdapter.previousTrack(action.packageName)
            }

            is ActionData.ControlMediaForApp.Rewind -> {
                result = mediaAdapter.rewind(action.packageName)
            }

            is ActionData.ControlMediaForApp.Stop -> {
                result = mediaAdapter.stop(action.packageName)
            }

            is ActionData.ControlMediaForApp.StepForward -> {
                result = mediaAdapter.stepForward(action.packageName)
            }

            is ActionData.ControlMediaForApp.StepBackward -> {
                result = mediaAdapter.stepBackward(action.packageName)
            }

            is ActionData.Rotation.CycleRotations -> {
                result = displayAdapter.disableAutoRotate().then {
                    val currentOrientation = displayAdapter.cachedOrientation

                    val index = action.orientations.indexOf(currentOrientation)

                    val nextOrientation = if (index == action.orientations.lastIndex) {
                        action.orientations[0]
                    } else {
                        action.orientations[index + 1]
                    }

                    displayAdapter.setOrientation(nextOrientation)
                }
            }

            is ActionData.Flashlight.Disable -> {
                result = cameraAdapter.disableFlashlight(action.lens)
            }

            is ActionData.Flashlight.Enable -> {
                result = cameraAdapter.enableFlashlight(action.lens, action.strengthPercent)
            }

            is ActionData.Flashlight.Toggle -> {
                result = cameraAdapter.toggleFlashlight(action.lens, action.strengthPercent)
            }

            is ActionData.Flashlight.ChangeStrength -> {
                result = cameraAdapter.changeFlashlightStrength(action.lens, action.percent)
            }

            is ActionData.SwitchKeyboard -> {
                result = inputMethodAdapter
                    .chooseImeWithoutUserInput(action.imeId)
                    .onSuccess {
                        val message = resourceProvider.getString(
                            R.string.toast_chose_keyboard,
                            it.label,
                        )
                        popupMessageAdapter.showPopupMessage(message)
                    }
            }

            is ActionData.Volume.Down -> {
                result = volumeAdapter.lowerVolume(showVolumeUi = action.showVolumeUi)
            }

            is ActionData.Volume.Up -> {
                result = volumeAdapter.raiseVolume(showVolumeUi = action.showVolumeUi)
            }

            is ActionData.Volume.Mute -> {
                result = volumeAdapter.muteVolume(showVolumeUi = action.showVolumeUi)
            }

            is ActionData.Volume.Stream.Decrease -> {
                result = volumeAdapter.lowerVolume(
                    stream = action.volumeStream,
                    showVolumeUi = action.showVolumeUi,
                )
            }

            is ActionData.Volume.Stream.Increase -> {
                result = volumeAdapter.raiseVolume(
                    stream = action.volumeStream,
                    showVolumeUi = action.showVolumeUi,
                )
            }

            is ActionData.Volume.ToggleMute -> {
                result = volumeAdapter.toggleMuteVolume(showVolumeUi = action.showVolumeUi)
            }

            is ActionData.Volume.UnMute -> {
                result = volumeAdapter.unmuteVolume(showVolumeUi = action.showVolumeUi)
            }

            is ActionData.TapScreen -> {
                result = accessibilityService.tapScreen(action.x, action.y, inputEventType)
            }

            is ActionData.SwipeScreen -> {
                result = accessibilityService.swipeScreen(
                    action.xStart,
                    action.yStart,
                    action.xEnd,
                    action.yEnd,
                    action.fingerCount,
                    action.duration,
                    inputEventType,
                )
            }

            is ActionData.PinchScreen -> {
                result = accessibilityService.pinchScreen(
                    action.x,
                    action.y,
                    action.distance,
                    action.pinchType,
                    action.fingerCount,
                    action.duration,
                    inputEventType,
                )
            }

            is ActionData.Text -> {
                imeInputEventInjector.inputText(action.text)
                result = Success(Unit)
            }

            is ActionData.Url -> {
                result = openUrlAdapter.openUrl(action.url)
            }

            is ActionData.Sound -> {
                result = soundsManager.getSound(action.soundUid).then { file ->
                    mediaAdapter.playFile(file.uri, VolumeStream.ACCESSIBILITY)
                }
            }

            is ActionData.Wifi.Toggle -> {
                result = if (networkAdapter.isWifiEnabled()) {
                    networkAdapter.disableWifi()
                } else {
                    networkAdapter.enableWifi()
                }
            }

            is ActionData.Wifi.Enable -> {
                result = networkAdapter.enableWifi()
            }

            is ActionData.Wifi.Disable -> {
                result = networkAdapter.disableWifi()
            }

            is ActionData.Bluetooth.Toggle -> {
                result = if (bluetoothAdapter.isBluetoothEnabled.firstBlocking()) {
                    bluetoothAdapter.disable()
                } else {
                    bluetoothAdapter.enable()
                }
            }

            is ActionData.Bluetooth.Enable -> {
                result = bluetoothAdapter.enable()
            }

            is ActionData.Bluetooth.Disable -> {
                result = bluetoothAdapter.disable()
            }

            is ActionData.MobileData.Toggle -> {
                result = if (networkAdapter.isMobileDataEnabled()) {
                    networkAdapter.disableMobileData()
                } else {
                    networkAdapter.enableMobileData()
                }
            }

            is ActionData.MobileData.Enable -> {
                result = networkAdapter.enableMobileData()
            }

            is ActionData.MobileData.Disable -> {
                result = networkAdapter.disableMobileData()
            }

            is ActionData.Brightness.ToggleAuto -> {
                result = if (displayAdapter.isAutoBrightnessEnabled()) {
                    displayAdapter.disableAutoBrightness()
                } else {
                    displayAdapter.enableAutoBrightness()
                }
            }

            is ActionData.Brightness.DisableAuto -> {
                result = displayAdapter.disableAutoBrightness()
            }

            is ActionData.Brightness.EnableAuto -> {
                result = displayAdapter.enableAutoBrightness()
            }

            is ActionData.Brightness.Increase -> {
                result = displayAdapter.increaseBrightness()
            }

            is ActionData.Brightness.Decrease -> {
                result = displayAdapter.decreaseBrightness()
            }

            is ActionData.Rotation.ToggleAuto -> {
                result = if (displayAdapter.isAutoRotateEnabled()) {
                    displayAdapter.disableAutoRotate()
                } else {
                    displayAdapter.enableAutoRotate()
                }
            }

            is ActionData.Rotation.EnableAuto -> {
                result = displayAdapter.enableAutoRotate()
            }

            is ActionData.Rotation.DisableAuto -> {
                result = displayAdapter.disableAutoRotate()
            }

            is ActionData.Rotation.Portrait -> {
                displayAdapter.disableAutoRotate()
                result = displayAdapter.setOrientation(Orientation.ORIENTATION_0)
            }

            is ActionData.Rotation.Landscape -> {
                displayAdapter.disableAutoRotate()
                result = displayAdapter.setOrientation(Orientation.ORIENTATION_90)
            }

            is ActionData.Rotation.SwitchOrientation -> {
                if (displayAdapter.cachedOrientation == Orientation.ORIENTATION_180 ||
                    displayAdapter.cachedOrientation == Orientation.ORIENTATION_0
                ) {
                    result = displayAdapter.setOrientation(Orientation.ORIENTATION_90)
                } else {
                    result = displayAdapter.setOrientation(Orientation.ORIENTATION_0)
                }
            }

            is ActionData.Volume.CycleRingerMode -> {
                result = when (volumeAdapter.ringerMode) {
                    RingerMode.NORMAL -> volumeAdapter.setRingerMode(RingerMode.VIBRATE)
                    RingerMode.VIBRATE -> volumeAdapter.setRingerMode(RingerMode.SILENT)
                    RingerMode.SILENT -> volumeAdapter.setRingerMode(RingerMode.NORMAL)
                }
            }

            is ActionData.Volume.CycleVibrateRing -> {
                result = when (volumeAdapter.ringerMode) {
                    RingerMode.NORMAL -> volumeAdapter.setRingerMode(RingerMode.VIBRATE)
                    RingerMode.VIBRATE -> volumeAdapter.setRingerMode(RingerMode.NORMAL)
                    RingerMode.SILENT -> volumeAdapter.setRingerMode(RingerMode.NORMAL)
                }
            }

            is ActionData.DoNotDisturb.Disable -> {
                result = volumeAdapter.disableDndMode()
            }

            is ActionData.StatusBar.ExpandNotifications -> {
                val globalAction = AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS

                result = accessibilityService.doGlobalAction(globalAction).otherwise {
                    shellAdapter.execute("cmd statusbar expand-notifications")
                }
            }

            is ActionData.StatusBar.ToggleNotifications -> {
                result =
                    if (accessibilityService.rootNode?.packageName == "com.android.systemui") {
                        closeStatusBarShade()
                    } else {
                        val globalAction = AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS

                        accessibilityService.doGlobalAction(globalAction).otherwise {
                            shellAdapter.execute("cmd statusbar expand-notifications")
                        }
                    }
            }

            is ActionData.StatusBar.ExpandQuickSettings -> {
                val globalAction = AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS

                result =
                    accessibilityService.doGlobalAction(globalAction).otherwise {
                        shellAdapter.execute("cmd statusbar expand-settings")
                    }
            }

            is ActionData.StatusBar.ToggleQuickSettings -> {
                result =
                    if (accessibilityService.rootNode?.packageName == "com.android.systemui") {
                        closeStatusBarShade()
                    } else {
                        val globalAction = AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS

                        accessibilityService.doGlobalAction(globalAction).otherwise {
                            shellAdapter.execute("cmd statusbar expand-settings")
                        }
                    }
            }

            is ActionData.StatusBar.Collapse -> {
                result = closeStatusBarShade()
            }

            is ActionData.ControlMedia.Pause -> {
                result = mediaAdapter.pause()
            }

            is ActionData.ControlMedia.Play -> {
                result = mediaAdapter.play()
            }

            is ActionData.ControlMedia.PlayPause -> {
                result = mediaAdapter.playPause()
            }

            is ActionData.ControlMedia.NextTrack -> {
                result = mediaAdapter.nextTrack()
            }

            is ActionData.ControlMedia.PreviousTrack -> {
                result = mediaAdapter.previousTrack()
            }

            is ActionData.ControlMedia.FastForward -> {
                result = mediaAdapter.fastForward()
            }

            is ActionData.ControlMedia.Rewind -> {
                result = mediaAdapter.rewind()
            }

            is ActionData.ControlMedia.Stop -> {
                result = mediaAdapter.stop()
            }

            is ActionData.ControlMedia.StepForward -> {
                result = mediaAdapter.stepForward()
            }

            is ActionData.ControlMedia.StepBackward -> {
                result = mediaAdapter.stepBackward()
            }

            is ActionData.GoBack -> {
                result =
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            }

            is ActionData.GoHome -> {
                result =
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            }

            is ActionData.OpenRecents -> {
                result =
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }

            is ActionData.ToggleSplitScreen -> {
                result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)
                } else {
                    Error.SdkVersionTooLow(minSdk = Build.VERSION_CODES.N)
                }
            }

            is ActionData.GoLastApp -> {
                accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
                delay(100)
                result =
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            }

            is ActionData.OpenMenu -> {
                result = openMenuHelper.openMenu()
            }

            is ActionData.Nfc.Enable -> {
                result = nfcAdapter.enable()
            }

            is ActionData.Nfc.Disable -> {
                result = nfcAdapter.disable()
            }

            is ActionData.Nfc.Toggle -> {
                result = if (nfcAdapter.isEnabled()) {
                    nfcAdapter.disable()
                } else {
                    nfcAdapter.enable()
                }
            }

            is ActionData.MoveCursorToEnd -> {
                val keyModel = InputKeyModel(
                    keyCode = KeyEvent.KEYCODE_MOVE_END,
                    metaState = KeyEvent.META_CTRL_ON,
                )

                if (inputKeyEventsWithShizuku.value) {
                    shizukuInputEventInjector.inputKeyEvent(keyModel)
                } else {
                    imeInputEventInjector.inputKeyEvent(keyModel)
                }

                result = Success(Unit)
            }

            is ActionData.ToggleKeyboard -> {
                val isHidden = accessibilityService.isKeyboardHidden.firstBlocking()
                if (isHidden) {
                    accessibilityService.showKeyboard()
                } else {
                    accessibilityService.hideKeyboard()
                }

                result = Success(Unit)
            }

            is ActionData.ShowKeyboard -> {
                accessibilityService.showKeyboard()
                result = Success(Unit)
            }

            is ActionData.HideKeyboard -> {
                accessibilityService.hideKeyboard()
                result = Success(Unit)
            }

            is ActionData.ShowKeyboardPicker -> {
                result = inputMethodAdapter.showImePicker(fromForeground = false)
            }

            is ActionData.CutText -> {
                result = accessibilityService.performActionOnNode({ it.isFocused }) {
                    AccessibilityNodeAction(AccessibilityNodeInfo.ACTION_CUT)
                }
            }

            is ActionData.CopyText -> {
                result = accessibilityService.performActionOnNode({ it.isFocused }) {
                    AccessibilityNodeAction(AccessibilityNodeInfo.ACTION_COPY)
                }
            }

            is ActionData.PasteText -> {
                result = accessibilityService.performActionOnNode({ it.isFocused }) {
                    AccessibilityNodeAction(AccessibilityNodeInfo.ACTION_PASTE)
                }
            }

            is ActionData.SelectWordAtCursor -> {
                result = accessibilityService.performActionOnNode({ it.isFocused }) { node ->
                    // it is at the cursor position if they both return the same value
                    if (node.textSelectionStart == node.textSelectionEnd) {
                        val cursorPosition = node.textSelectionStart

                        val wordBoundary =
                            node.text.toString().getWordBoundaries(cursorPosition)
                                ?: return@performActionOnNode null

                        val extras = mapOf(
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT to wordBoundary.first,

                            // The index of the cursor is the index of the last char in the word + 1
                            AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT to wordBoundary.second + 1,
                        )

                        AccessibilityNodeAction(
                            AccessibilityNodeInfo.ACTION_SET_SELECTION,
                            extras,
                        )
                    } else {
                        null
                    }
                }
            }

            is ActionData.AirplaneMode.Toggle -> {
                result = if (airplaneModeAdapter.isEnabled()) {
                    airplaneModeAdapter.disable()
                } else {
                    airplaneModeAdapter.enable()
                }
            }

            is ActionData.AirplaneMode.Enable -> {
                result = airplaneModeAdapter.enable()
            }

            is ActionData.AirplaneMode.Disable -> {
                result = airplaneModeAdapter.disable()
            }

            is ActionData.Screenshot -> {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    val picturesFolder = fileAdapter.getPicturesFolder()
                    val screenshotsFolder = "$picturesFolder/Screenshots"
                    val fileDate = FileUtils.createFileDate()

                    result =
                        suAdapter.execute("mkdir -p $screenshotsFolder; screencap -p $screenshotsFolder/Screenshot_$fileDate.png")
                            .onSuccess {
                                // Wait 3 seconds so the message isn't shown in the screenshot.
                                delay(3000)

                                popupMessageAdapter.showPopupMessage(
                                    resourceProvider.getString(
                                        R.string.toast_screenshot_taken,
                                    ),
                                )
                            }
                } else {
                    result =
                        accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
                }
            }

            is ActionData.VoiceAssistant -> {
                result = packageManagerAdapter.launchVoiceAssistant()
            }

            is ActionData.DeviceAssistant -> {
                result = packageManagerAdapter.launchDeviceAssistant()
            }

            is ActionData.OpenCamera -> {
                result = packageManagerAdapter.launchCameraApp()
            }

            is ActionData.LockDevice -> {
                result = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                    suAdapter.execute("input keyevent ${KeyEvent.KEYCODE_POWER}")
                } else {
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
                }
            }

            is ActionData.ScreenOnOff -> {
                result = suAdapter.execute("input keyevent ${KeyEvent.KEYCODE_POWER}")
            }

            is ActionData.SecureLock -> {
                result = lockScreenAdapter.secureLockDevice()
            }

            is ActionData.ConsumeKeyEvent -> {
                result = Success(Unit)
            }

            is ActionData.OpenSettings -> {
                result = packageManagerAdapter.launchSettingsApp()
            }

            is ActionData.ShowPowerMenu -> {
                result =
                    accessibilityService.doGlobalAction(AccessibilityService.GLOBAL_ACTION_POWER_DIALOG)
            }

            is ActionData.Volume.ShowDialog -> {
                result = volumeAdapter.showVolumeUi()
            }

            ActionData.DismissAllNotifications -> {
                result = notificationReceiverAdapter.send(ServiceEvent.DismissAllNotifications)
            }

            ActionData.DismissLastNotification -> {
                result = notificationReceiverAdapter.send(ServiceEvent.DismissLastNotification)
            }

            ActionData.AnswerCall -> {
                phoneAdapter.answerCall()
                result = success()
            }

            ActionData.EndCall -> {
                phoneAdapter.endCall()
                result = success()
            }

            ActionData.DeviceControls -> {
                result = intentAdapter.send(
                    IntentTarget.ACTIVITY,
                    uri = "#Intent;action=android.intent.action.MAIN;package=com.android.systemui;component=com.android.systemui/.controls.ui.ControlsActivity;end",
                    extras = emptyList(),
                )
            }

            is ActionData.HttpRequest -> {
                result = networkAdapter.sendHttpRequest(
                    method = action.method,
                    url = action.url,
                    body = action.body,
                    authorizationHeader = action.authorizationHeader,
                )
            }

            is ActionData.InteractUiElement -> {
                if (accessibilityService.activeWindowPackage.first() != action.packageName) {
                    result = Error.UiElementNotFound
                } else {
                    result = accessibilityService.performActionOnNode(
                        findNode = { node ->
                            matchAccessibilityNode(node, action)
                        },
                        performAction = { AccessibilityNodeAction(action = action.nodeAction.accessibilityActionId) },
                    ).otherwise { Error.UiElementNotFound }
                }
            }
        }

        when (result) {
            is Success -> Timber.d("Performed action $action, input event type: $inputEventType, key meta state: $keyMetaState")
            is Error -> Timber.d(
                "Failed to perform action $action, reason: ${result.getFullMessage(resourceProvider)}, action: $action, input event type: $inputEventType, key meta state: $keyMetaState",
            )
        }

        result.showErrorMessageOnFail()
    }

    override fun getErrorSnapshot(): ActionErrorSnapshot {
        return getActionError.actionErrorSnapshot.firstBlocking()
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

    private fun getDeviceIdForKeyEventAction(action: ActionData.InputKeyEvent): Int {
        if (action.device?.descriptor == null) {
            // automatically select a game controller as the input device for game controller key events

            if (InputEventUtils.isGamepadKeyCode(action.keyCode)) {
                deviceAdapter.connectedInputDevices.value.ifIsData { inputDevices ->
                    val device = inputDevices.find { it.isGameController }

                    if (device != null) {
                        return device.id
                    }
                }
            }

            return 0
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

    private fun closeStatusBarShade(): Result<*> {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return accessibilityService
                .doGlobalAction(AccessibilityService.GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
        } else {
            return shellAdapter.execute("cmd statusbar collapse")
        }
    }

    private fun Result<*>.showErrorMessageOnFail() {
        onFailure {
            popupMessageAdapter.showPopupMessage(it.getFullMessage(resourceProvider))
        }
    }

    private fun matchAccessibilityNode(
        node: AccessibilityNodeModel,
        action: ActionData.InteractUiElement,
    ): Boolean {
        if (!node.actions.contains(action.nodeAction.accessibilityActionId)) {
            return false
        }

        if (compareIfNonNull(node.uniqueId, action.uniqueId)) {
            return true
        }

        if (action.contentDescription == null && action.text == null) {
            if (compareIfNonNull(node.viewResourceId, action.viewResourceId)) {
                return true
            }

            if (compareIfNonNull(node.className, action.className)) {
                return true
            }
        } else {
            if (compareIfNonNull(node.contentDescription, action.contentDescription) ||
                compareIfNonNull(node.text, action.text)
            ) {
                if (action.viewResourceId != null) {
                    return node.viewResourceId == action.viewResourceId
                }

                if (action.className != null) {
                    return node.className == action.className
                }

                return true
            }
        }

        return false
    }

    private fun <T> compareIfNonNull(a: T?, b: T?): Boolean {
        return a != null && b != null && a == b
    }
}

interface PerformActionsUseCase {
    val defaultHoldDownDuration: Flow<Long>
    val defaultRepeatDelay: Flow<Long>
    val defaultRepeatRate: Flow<Long>

    suspend fun perform(
        action: ActionData,
        inputEventType: InputEventType = InputEventType.DOWN_UP,
        keyMetaState: Int = 0,
    )

    fun getErrorSnapshot(): ActionErrorSnapshot
}
