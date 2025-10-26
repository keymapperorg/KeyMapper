package io.github.sds100.keymapper.base.actions

import android.text.InputType
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.actions.pinchscreen.PinchPickCoordinateResult
import io.github.sds100.keymapper.base.actions.swipescreen.SwipePickCoordinateResult
import io.github.sds100.keymapper.base.actions.tapscreen.PickCoordinateResult
import io.github.sds100.keymapper.base.system.intents.ConfigIntentResult
import io.github.sds100.keymapper.base.utils.DndModeStrings
import io.github.sds100.keymapper.base.utils.RingerModeStrings
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.MultiChoiceItem
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.SystemError
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.network.HttpMethod
import io.github.sds100.keymapper.system.volume.DndMode
import io.github.sds100.keymapper.system.volume.RingerMode
import io.github.sds100.keymapper.system.volume.VolumeStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class CreateActionDelegate(
    private val coroutineScope: CoroutineScope,
    private val useCase: CreateActionUseCase,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {
    val actionResult: MutableStateFlow<ActionData?> = MutableStateFlow(null)
    var enableFlashlightActionState: EnableFlashlightActionState? by mutableStateOf(null)
    var changeFlashlightStrengthActionState: ChangeFlashlightStrengthActionState? by mutableStateOf(
        null,
    )

    var httpRequestBottomSheetState: ActionData.HttpRequest? by mutableStateOf(null)
    var smsActionBottomSheetState: SmsActionBottomSheetState? by mutableStateOf(null)
    var volumeActionState: VolumeActionBottomSheetState? by mutableStateOf(null)

    init {
        coroutineScope.launch {
            useCase.isFlashlightEnabled().collectLatest { enabled ->
                enableFlashlightActionState?.also { state ->
                    enableFlashlightActionState = state.copy(isFlashEnabled = enabled)
                }
            }
        }
    }

    fun onDoneConfigEnableFlashlightClick() {
        enableFlashlightActionState?.also { state ->
            val flashInfo = state.lensData[state.selectedLens] ?: return

            val strengthPercent =
                state.flashStrength
                    .takeIf { it != flashInfo.defaultStrength }
                    ?.let { it.toFloat() / flashInfo.maxStrength }
                    ?.coerceIn(0f, 1f)

            val action =
                when (state.actionToCreate) {
                    ActionId.TOGGLE_FLASHLIGHT ->
                        ActionData.Flashlight.Toggle(
                            state.selectedLens,
                            strengthPercent,
                        )

                    ActionId.ENABLE_FLASHLIGHT ->
                        ActionData.Flashlight.Enable(
                            state.selectedLens,
                            strengthPercent,
                        )

                    else -> return
                }

            enableFlashlightActionState = null

            useCase.disableFlashlight()

            actionResult.update { action }
        }
    }

    fun onDoneChangeFlashlightBrightnessClick() {
        changeFlashlightStrengthActionState?.also { state ->
            val flashInfo = state.lensData[state.selectedLens] ?: return

            val strengthPercent =
                (state.flashStrength.toFloat() / flashInfo.maxStrength).coerceIn(-1f, 1f)

            val action =
                ActionData.Flashlight.ChangeStrength(
                    state.selectedLens,
                    strengthPercent,
                )

            changeFlashlightStrengthActionState = null

            actionResult.update { action }
        }
    }

    fun onSelectStrength(strength: Int) {
        enableFlashlightActionState?.also { state ->
            enableFlashlightActionState = state.copy(flashStrength = strength)

            if (state.isFlashEnabled) {
                val lensData = state.lensData[state.selectedLens] ?: return

                useCase.setFlashlightBrightness(
                    state.selectedLens,
                    strength / lensData.maxStrength.toFloat(),
                )
            }
        }
    }

    fun onTestFlashlightConfigClick() {
        enableFlashlightActionState?.also { state ->
            val lensData = state.lensData[state.selectedLens] ?: return

            useCase.toggleFlashlight(
                state.selectedLens,
                state.flashStrength / lensData.maxStrength.toFloat(),
            )
        }
    }

    fun onDoneSmsClick() {
        val state = smsActionBottomSheetState ?: return

        val action =
            when (state) {
                is SmsActionBottomSheetState.ComposeSms ->
                    ActionData.ComposeSms(
                        state.number,
                        state.message,
                    )

                is SmsActionBottomSheetState.SendSms ->
                    ActionData.SendSms(
                        state.number,
                        state.message,
                    )
            }

        smsActionBottomSheetState = null
        actionResult.update { action }
    }

    fun onDoneConfigVolumeClick() {
        volumeActionState?.also { state ->
            val action =
                when (state.actionId) {
                    ActionId.VOLUME_UP ->
                        ActionData.Volume.Up(
                            showVolumeUi = state.showVolumeUi,
                            volumeStream = state.volumeStream,
                        )
                    ActionId.VOLUME_DOWN ->
                        ActionData.Volume.Down(
                            showVolumeUi = state.showVolumeUi,
                            volumeStream = state.volumeStream,
                        )
                    else -> return
                }

            volumeActionState = null
            actionResult.update { action }
        }
    }

    fun onTestSmsClick() {
        coroutineScope.launch {
            (smsActionBottomSheetState as? SmsActionBottomSheetState.SendSms)?.also { state ->
                smsActionBottomSheetState = state.copy(testResult = State.Loading)

                val result = useCase.testSms(state.number, state.message)

                if (result is SystemError.PermissionDenied) {
                    useCase.requestPermission(result.permission)
                    smsActionBottomSheetState = state.copy(testResult = null)
                } else {
                    smsActionBottomSheetState = state.copy(testResult = State.Data(result))
                }
            }
        }
    }

    suspend fun editAction(oldData: ActionData) {
        if (!oldData.isEditable()) {
            throw IllegalArgumentException("This action ${oldData.javaClass.name} can't be edited!")
        }

        configAction(oldData.id, oldData)?.let { action ->
            actionResult.update { action }
        }
    }

    suspend fun createAction(id: ActionId) {
        configAction(id)?.let { action ->
            actionResult.update { action }
        }
    }

    private suspend fun configAction(
        actionId: ActionId,
        oldData: ActionData? = null,
    ): ActionData? {
        when (actionId) {
            ActionId.SWITCH_KEYBOARD -> {
                val inputMethods = useCase.getInputMethods()
                val items =
                    inputMethods.map {
                        it.id to it.label
                    }

                val imeId =
                    showDialog("choose_ime", DialogModel.SingleChoice(items)) ?: return null
                val imeName = inputMethods.single { it.id == imeId }.label

                return ActionData.SwitchKeyboard(imeId, imeName)
            }

            ActionId.PLAY_PAUSE_MEDIA_PACKAGE,
            ActionId.PLAY_MEDIA_PACKAGE,
            ActionId.PAUSE_MEDIA_PACKAGE,
            ActionId.NEXT_TRACK_PACKAGE,
            ActionId.PREVIOUS_TRACK_PACKAGE,
            ActionId.FAST_FORWARD_PACKAGE,
            ActionId.REWIND_PACKAGE,
            ActionId.STOP_MEDIA_PACKAGE,
            ActionId.STEP_FORWARD_PACKAGE,
            ActionId.STEP_BACKWARD_PACKAGE,
            -> {
                val packageName =
                    navigate(
                        "choose_app_for_media_action",
                        NavDestination.ChooseApp(allowHiddenApps = true),
                    )
                        ?: return null

                val action =
                    when (actionId) {
                        ActionId.PAUSE_MEDIA_PACKAGE ->
                            ActionData.ControlMediaForApp.Pause(packageName)

                        ActionId.PLAY_MEDIA_PACKAGE ->
                            ActionData.ControlMediaForApp.Play(packageName)

                        ActionId.PLAY_PAUSE_MEDIA_PACKAGE ->
                            ActionData.ControlMediaForApp.PlayPause(packageName)

                        ActionId.NEXT_TRACK_PACKAGE ->
                            ActionData.ControlMediaForApp.NextTrack(packageName)

                        ActionId.PREVIOUS_TRACK_PACKAGE ->
                            ActionData.ControlMediaForApp.PreviousTrack(packageName)

                        ActionId.FAST_FORWARD_PACKAGE ->
                            ActionData.ControlMediaForApp.FastForward(packageName)

                        ActionId.REWIND_PACKAGE ->
                            ActionData.ControlMediaForApp.Rewind(packageName)

                        ActionId.STOP_MEDIA_PACKAGE ->
                            ActionData.ControlMediaForApp.Stop(packageName)

                        ActionId.STEP_FORWARD_PACKAGE ->
                            ActionData.ControlMediaForApp.StepForward(packageName)

                        ActionId.STEP_BACKWARD_PACKAGE ->
                            ActionData.ControlMediaForApp.StepBackward(packageName)

                        else -> throw Exception("don't know how to create action for $actionId")
                    }

                return action
            }

            ActionId.VOLUME_UP -> {
                val oldVolumeUpData = oldData as? ActionData.Volume.Up
                volumeActionState =
                    VolumeActionBottomSheetState(
                        actionId = ActionId.VOLUME_UP,
                        volumeStream = oldVolumeUpData?.volumeStream,
                        showVolumeUi = oldVolumeUpData?.showVolumeUi ?: false,
                    )
                return null
            }

            ActionId.VOLUME_DOWN -> {
                val oldVolumeDownData = oldData as? ActionData.Volume.Down
                volumeActionState =
                    VolumeActionBottomSheetState(
                        actionId = ActionId.VOLUME_DOWN,
                        volumeStream = oldVolumeDownData?.volumeStream,
                        showVolumeUi = oldVolumeDownData?.showVolumeUi ?: false,
                    )
                return null
            }

            ActionId.VOLUME_MUTE,
            ActionId.VOLUME_UNMUTE,
            ActionId.VOLUME_TOGGLE_MUTE,
            -> {
                val showVolumeUiId = 0
                val isVolumeUiChecked =
                    when (oldData) {
                        is ActionData.Volume.Mute -> oldData.showVolumeUi
                        is ActionData.Volume.UnMute -> oldData.showVolumeUi
                        is ActionData.Volume.ToggleMute -> oldData.showVolumeUi
                        else -> false
                    }

                val dialogItems =
                    listOf(
                        MultiChoiceItem(
                            showVolumeUiId,
                            getString(R.string.flag_show_volume_dialog),
                            isVolumeUiChecked,
                        ),
                    )

                val showVolumeUiDialog = DialogModel.MultiChoice(items = dialogItems)

                val chosenFlags = showDialog("show_volume_ui", showVolumeUiDialog) ?: return null

                val showVolumeUi = chosenFlags.contains(showVolumeUiId)

                val action =
                    when (actionId) {
                        ActionId.VOLUME_MUTE -> ActionData.Volume.Mute(showVolumeUi)
                        ActionId.VOLUME_UNMUTE -> ActionData.Volume.UnMute(showVolumeUi)
                        ActionId.VOLUME_TOGGLE_MUTE ->
                            ActionData.Volume.ToggleMute(
                                showVolumeUi,
                            )

                        else -> throw Exception("don't know how to create action for $actionId")
                    }

                return action
            }

            ActionId.MUTE_MICROPHONE -> {
                return ActionData.Microphone.Mute
            }

            ActionId.UNMUTE_MICROPHONE -> {
                return ActionData.Microphone.Unmute
            }

            ActionId.TOGGLE_MUTE_MICROPHONE -> {
                return ActionData.Microphone.Toggle
            }

            ActionId.VOLUME_INCREASE_STREAM,
            ActionId.VOLUME_DECREASE_STREAM,
            -> {
                // These deprecated actions are now converted to Volume.Up/Down with stream parameter
                // Determine which action ID to use based on the old action
                val newActionId =
                    when (actionId) {
                        ActionId.VOLUME_INCREASE_STREAM -> ActionId.VOLUME_UP
                        ActionId.VOLUME_DECREASE_STREAM -> ActionId.VOLUME_DOWN
                        else -> return null
                    }

                // Get the old stream if this is being edited
                val oldStream =
                    when (oldData) {
                        is ActionData.Volume.Up -> oldData.volumeStream
                        is ActionData.Volume.Down -> oldData.volumeStream
                        else -> null
                    }

                val oldShowVolumeUi =
                    when (oldData) {
                        is ActionData.Volume.Up -> oldData.showVolumeUi
                        is ActionData.Volume.Down -> oldData.showVolumeUi
                        else -> false
                    }

                volumeActionState =
                    VolumeActionBottomSheetState(
                        actionId = newActionId,
                        volumeStream = oldStream ?: VolumeStream.MUSIC, // Default to MUSIC for old stream actions
                        showVolumeUi = oldShowVolumeUi,
                    )
                return null
            }

            ActionId.CHANGE_RINGER_MODE -> {
                val items =
                    RingerMode.entries
                        .map { it to getString(RingerModeStrings.getLabel(it)) }

                val ringerMode =
                    showDialog("pick_ringer_mode", DialogModel.SingleChoice(items))
                        ?: return null

                return ActionData.Volume.SetRingerMode(ringerMode)
            }

            // don't need to show options for disabling do not disturb
            ActionId.TOGGLE_DND_MODE,
            ActionId.ENABLE_DND_MODE,
            -> {
                val items =
                    DndMode.entries
                        .map { it to getString(DndModeStrings.getLabel(it)) }

                val dndMode =
                    showDialog("pick_dnd_mode", DialogModel.SingleChoice(items))
                        ?: return null

                val action =
                    when (actionId) {
                        ActionId.TOGGLE_DND_MODE -> ActionData.DoNotDisturb.Toggle(dndMode)

                        ActionId.ENABLE_DND_MODE -> ActionData.DoNotDisturb.Enable(dndMode)

                        else -> throw Exception("don't know how to create action for $actionId")
                    }

                return action
            }

            ActionId.CYCLE_ROTATIONS -> {
                val items =
                    Orientation.values().map { orientation ->
                        val isChecked =
                            if (oldData is ActionData.Rotation.CycleRotations) {
                                oldData.orientations.contains(orientation)
                            } else {
                                false
                            }

                        val label =
                            when (orientation) {
                                Orientation.ORIENTATION_0 -> R.string.orientation_0
                                Orientation.ORIENTATION_90 -> R.string.orientation_90
                                Orientation.ORIENTATION_180 -> R.string.orientation_180
                                Orientation.ORIENTATION_270 -> R.string.orientation_270
                            }

                        MultiChoiceItem(
                            orientation,
                            getString(label),
                            isChecked,
                        )
                    }

                val orientations =
                    showDialog("pick_orientations", DialogModel.MultiChoice(items)) ?: return null

                return ActionData.Rotation.CycleRotations(orientations)
            }

            ActionId.TOGGLE_FLASHLIGHT, ActionId.ENABLE_FLASHLIGHT -> {
                val lenses = useCase.getFlashlightLenses()
                val selectedLens =
                    if (oldData is ActionData.Flashlight) {
                        oldData.lens
                    } else if (lenses.contains(CameraLens.BACK)) {
                        CameraLens.BACK
                    } else {
                        lenses.first()
                    }

                val lensData = lenses.associateWith { useCase.getFlashInfo(it)!! }
                val lensInfo = lensData[selectedLens] ?: lensData.values.first()

                val strength: Int =
                    when (oldData) {
                        is ActionData.Flashlight.Toggle ->
                            if (oldData.strengthPercent == null) {
                                lensInfo.defaultStrength
                            } else {
                                (oldData.strengthPercent * lensInfo.maxStrength).toInt()
                            }

                        is ActionData.Flashlight.Enable ->
                            if (oldData.strengthPercent == null) {
                                lensInfo.defaultStrength
                            } else {
                                (oldData.strengthPercent * lensInfo.maxStrength).toInt()
                            }

                        else -> lensInfo.defaultStrength
                    }

                enableFlashlightActionState =
                    EnableFlashlightActionState(
                        actionToCreate = actionId,
                        selectedLens = selectedLens,
                        lensData = lensData,
                        flashStrength = strength,
                        isFlashEnabled = useCase.isFlashlightEnabled().first(),
                    )

                return null
            }

            ActionId.CHANGE_FLASHLIGHT_STRENGTH -> {
                val lenses = useCase.getFlashlightLenses()
                val selectedLens =
                    if (oldData is ActionData.Flashlight.ChangeStrength) {
                        oldData.lens
                    } else if (lenses.contains(CameraLens.BACK)) {
                        CameraLens.BACK
                    } else {
                        lenses.first()
                    }

                val lensData = lenses.associateWith { useCase.getFlashInfo(it)!! }
                val lensInfo = lensData[selectedLens] ?: lensData.values.first()

                val strength: Int =
                    when (oldData) {
                        is ActionData.Flashlight.ChangeStrength -> {
                            (oldData.percent * lensInfo.maxStrength).toInt()
                        }

                        else -> (0.1f * lensInfo.maxStrength).toInt()
                    }

                changeFlashlightStrengthActionState =
                    ChangeFlashlightStrengthActionState(
                        selectedLens = selectedLens,
                        lensData = lensData,
                        flashStrength = strength,
                    )

                return null
            }

            ActionId.DISABLE_FLASHLIGHT,
            -> {
                val items =
                    useCase.getFlashlightLenses().map { lens ->
                        when (lens) {
                            CameraLens.FRONT -> lens to getString(R.string.lens_front)
                            CameraLens.BACK -> lens to getString(R.string.lens_back)
                        }
                    }

                if (items.size == 1) {
                    return ActionData.Flashlight.Disable(items.first().first)
                } else {
                    val lens =
                        showDialog("pick_lens", DialogModel.SingleChoice(items))
                            ?: return null

                    return ActionData.Flashlight.Disable(lens)
                }
            }

            ActionId.APP -> {
                val packageName =
                    navigate(
                        "choose_app_for_app_action",
                        NavDestination.ChooseApp(allowHiddenApps = false),
                    )
                        ?: return null

                return ActionData.App(packageName)
            }

            ActionId.APP_SHORTCUT -> {
                val appShortcutResult =
                    navigate("choose_app_shortcut", NavDestination.ChooseAppShortcut)
                        ?: return null

                return ActionData.AppShortcut(
                    appShortcutResult.packageName,
                    appShortcutResult.shortcutName,
                    appShortcutResult.uri,
                )
            }

            ActionId.KEY_CODE -> {
                val keyCode =
                    navigate("choose_key_code", NavDestination.ChooseKeyCode) ?: return null

                return ActionData.InputKeyEvent(keyCode = keyCode)
            }

            ActionId.KEY_EVENT -> {
                val keyEventAction =
                    if (oldData is ActionData.InputKeyEvent) {
                        navigate("config_key_event", NavDestination.ConfigKeyEventAction(oldData))
                    } else {
                        navigate("config_key_event", NavDestination.ConfigKeyEventAction())
                    }

                return keyEventAction
            }

            ActionId.TAP_SCREEN -> {
                val oldResult =
                    if (oldData is ActionData.TapScreen) {
                        PickCoordinateResult(
                            oldData.x,
                            oldData.y,
                            oldData.description ?: "",
                        )
                    } else {
                        null
                    }

                val result =
                    navigate(
                        "pick_display_coordinate_for_action",
                        NavDestination.PickCoordinate(oldResult),
                    ) ?: return null

                val description =
                    if (result.description.isEmpty()) {
                        null
                    } else {
                        result.description
                    }

                return ActionData.TapScreen(
                    result.x,
                    result.y,
                    description,
                )
            }

            ActionId.SWIPE_SCREEN -> {
                val oldResult =
                    if (oldData is ActionData.SwipeScreen) {
                        SwipePickCoordinateResult(
                            oldData.xStart,
                            oldData.yStart,
                            oldData.xEnd,
                            oldData.yEnd,
                            oldData.fingerCount,
                            oldData.duration,
                            oldData.description ?: "",
                        )
                    } else {
                        null
                    }

                val result =
                    navigate(
                        "pick_swipe_coordinate_for_action",
                        NavDestination.PickSwipeCoordinate(oldResult),
                    ) ?: return null

                val description =
                    if (result.description.isEmpty()) {
                        null
                    } else {
                        result.description
                    }

                return ActionData.SwipeScreen(
                    result.xStart,
                    result.yStart,
                    result.xEnd,
                    result.yEnd,
                    result.fingerCount,
                    result.duration,
                    description,
                )
            }

            ActionId.PINCH_SCREEN -> {
                val oldResult =
                    if (oldData is ActionData.PinchScreen) {
                        PinchPickCoordinateResult(
                            oldData.x,
                            oldData.y,
                            oldData.distance,
                            oldData.pinchType,
                            oldData.fingerCount,
                            oldData.duration,
                            oldData.description ?: "",
                        )
                    } else {
                        null
                    }

                val result =
                    navigate(
                        "pick_pinch_coordinate_for_action",
                        NavDestination.PickPinchCoordinate(oldResult),
                    ) ?: return null

                val description =
                    if (result.description.isEmpty()) {
                        null
                    } else {
                        result.description
                    }

                return ActionData.PinchScreen(
                    result.x,
                    result.y,
                    result.distance,
                    result.pinchType,
                    result.fingerCount,
                    result.duration,
                    description,
                )
            }

            ActionId.TEXT -> {
                val oldText =
                    if (oldData is ActionData.Text) {
                        oldData.text
                    } else {
                        ""
                    }

                val text =
                    showDialog(
                        "create_text_action",
                        DialogModel.Text(
                            hint = getString(R.string.hint_create_text_action),
                            allowEmpty = false,
                            text = oldText,
                        ),
                    ) ?: return null

                return ActionData.Text(text)
            }

            ActionId.URL -> {
                val oldUrl =
                    if (oldData is ActionData.Url) {
                        oldData.url
                    } else {
                        ""
                    }

                val text =
                    showDialog(
                        "create_url_action",
                        DialogModel.Text(
                            hint = getString(R.string.hint_create_url_action),
                            allowEmpty = false,
                            inputType = InputType.TYPE_TEXT_VARIATION_URI,
                            text = oldUrl,
                        ),
                    ) ?: return null

                return ActionData.Url(text)
            }

            ActionId.INTENT -> {
                val oldIntent =
                    if (oldData is ActionData.Intent) {
                        ConfigIntentResult(
                            oldData.uri,
                            oldData.target,
                            oldData.description,
                            oldData.extras,
                        )
                    } else {
                        null
                    }

                val result =
                    navigate(
                        "config_intent",
                        NavDestination.ConfigIntent(oldIntent),
                    ) ?: return null

                return ActionData.Intent(
                    description = result.description,
                    target = result.target,
                    uri = result.uri,
                    extras = result.extras,
                )
            }

            ActionId.PHONE_CALL -> {
                val oldText =
                    if (oldData is ActionData.PhoneCall) {
                        oldData.number
                    } else {
                        ""
                    }

                val text =
                    showDialog(
                        "create_phone_call_action",
                        DialogModel.Text(
                            hint = getString(R.string.hint_create_phone_call_action),
                            allowEmpty = false,
                            inputType = InputType.TYPE_CLASS_PHONE,
                            text = oldText,
                        ),
                    ) ?: return null

                return ActionData.PhoneCall(text)
            }

            ActionId.SEND_SMS, ActionId.COMPOSE_SMS -> {
                val number =
                    when (oldData) {
                        is ActionData.SendSms -> oldData.number
                        is ActionData.ComposeSms -> oldData.number
                        else -> ""
                    }

                val message =
                    when (oldData) {
                        is ActionData.SendSms -> oldData.message
                        is ActionData.ComposeSms -> oldData.message
                        else -> ""
                    }

                smsActionBottomSheetState =
                    if (actionId == ActionId.SEND_SMS) {
                        SmsActionBottomSheetState.SendSms(
                            number = number,
                            message = message,
                            testResult = null,
                        )
                    } else {
                        SmsActionBottomSheetState.ComposeSms(
                            number = number,
                            message = message,
                        )
                    }

                return null
            }

            ActionId.SOUND -> {
                return navigate(
                    "choose_sound_file",
                    NavDestination.ChooseSound,
                )
            }

            ActionId.TOGGLE_WIFI -> return ActionData.Wifi.Toggle
            ActionId.ENABLE_WIFI -> return ActionData.Wifi.Enable
            ActionId.DISABLE_WIFI -> return ActionData.Wifi.Disable

            ActionId.TOGGLE_BLUETOOTH -> return ActionData.Bluetooth.Toggle
            ActionId.ENABLE_BLUETOOTH -> return ActionData.Bluetooth.Enable
            ActionId.DISABLE_BLUETOOTH -> return ActionData.Bluetooth.Disable

            ActionId.TOGGLE_MOBILE_DATA -> return ActionData.MobileData.Toggle
            ActionId.ENABLE_MOBILE_DATA -> return ActionData.MobileData.Enable
            ActionId.DISABLE_MOBILE_DATA -> return ActionData.MobileData.Disable

            ActionId.TOGGLE_AUTO_BRIGHTNESS -> return ActionData.Brightness.ToggleAuto
            ActionId.DISABLE_AUTO_BRIGHTNESS -> return ActionData.Brightness.DisableAuto
            ActionId.ENABLE_AUTO_BRIGHTNESS -> return ActionData.Brightness.EnableAuto
            ActionId.INCREASE_BRIGHTNESS -> return ActionData.Brightness.Increase
            ActionId.DECREASE_BRIGHTNESS -> return ActionData.Brightness.Decrease

            ActionId.TOGGLE_AUTO_ROTATE -> return ActionData.Rotation.ToggleAuto
            ActionId.ENABLE_AUTO_ROTATE -> return ActionData.Rotation.EnableAuto
            ActionId.DISABLE_AUTO_ROTATE -> return ActionData.Rotation.DisableAuto
            ActionId.PORTRAIT_MODE -> return ActionData.Rotation.Portrait
            ActionId.LANDSCAPE_MODE -> return ActionData.Rotation.Landscape
            ActionId.SWITCH_ORIENTATION -> return ActionData.Rotation.SwitchOrientation

            ActionId.VOLUME_SHOW_DIALOG -> return ActionData.Volume.ShowDialog
            ActionId.CYCLE_RINGER_MODE -> return ActionData.Volume.CycleRingerMode
            ActionId.CYCLE_VIBRATE_RING -> return ActionData.Volume.CycleVibrateRing

            ActionId.EXPAND_NOTIFICATION_DRAWER -> return ActionData.StatusBar.ExpandNotifications
            ActionId.TOGGLE_NOTIFICATION_DRAWER -> return ActionData.StatusBar.ToggleNotifications
            ActionId.EXPAND_QUICK_SETTINGS -> return ActionData.StatusBar.ExpandQuickSettings
            ActionId.TOGGLE_QUICK_SETTINGS -> return ActionData.StatusBar.ToggleQuickSettings
            ActionId.COLLAPSE_STATUS_BAR -> return ActionData.StatusBar.Collapse

            ActionId.PAUSE_MEDIA -> return ActionData.ControlMedia.Pause
            ActionId.PLAY_MEDIA -> return ActionData.ControlMedia.Play
            ActionId.PLAY_PAUSE_MEDIA -> return ActionData.ControlMedia.PlayPause
            ActionId.NEXT_TRACK -> return ActionData.ControlMedia.NextTrack
            ActionId.PREVIOUS_TRACK -> return ActionData.ControlMedia.PreviousTrack
            ActionId.FAST_FORWARD -> return ActionData.ControlMedia.FastForward
            ActionId.REWIND -> return ActionData.ControlMedia.Rewind
            ActionId.STOP_MEDIA -> return ActionData.ControlMedia.Stop
            ActionId.STEP_FORWARD -> return ActionData.ControlMedia.StepForward
            ActionId.STEP_BACKWARD -> return ActionData.ControlMedia.StepBackward

            ActionId.GO_BACK -> return ActionData.GoBack
            ActionId.GO_HOME -> return ActionData.GoHome
            ActionId.OPEN_RECENTS -> return ActionData.OpenRecents
            ActionId.TOGGLE_SPLIT_SCREEN -> return ActionData.ToggleSplitScreen
            ActionId.GO_LAST_APP -> return ActionData.GoLastApp
            ActionId.OPEN_MENU -> return ActionData.OpenMenu

            ActionId.ENABLE_NFC -> return ActionData.Nfc.Enable
            ActionId.DISABLE_NFC -> return ActionData.Nfc.Disable
            ActionId.TOGGLE_NFC -> return ActionData.Nfc.Toggle

            ActionId.TOGGLE_KEYBOARD -> return ActionData.ToggleKeyboard
            ActionId.SHOW_KEYBOARD -> return ActionData.ShowKeyboard
            ActionId.HIDE_KEYBOARD -> return ActionData.HideKeyboard
            ActionId.SHOW_KEYBOARD_PICKER -> return ActionData.ShowKeyboardPicker
            ActionId.TEXT_CUT -> return ActionData.CutText
            ActionId.TEXT_COPY -> return ActionData.CopyText
            ActionId.TEXT_PASTE -> return ActionData.PasteText
            ActionId.SELECT_WORD_AT_CURSOR -> return ActionData.SelectWordAtCursor

            ActionId.TOGGLE_AIRPLANE_MODE -> return ActionData.AirplaneMode.Toggle
            ActionId.ENABLE_AIRPLANE_MODE -> return ActionData.AirplaneMode.Enable
            ActionId.DISABLE_AIRPLANE_MODE -> return ActionData.AirplaneMode.Disable

            ActionId.SCREENSHOT -> return ActionData.Screenshot
            ActionId.OPEN_VOICE_ASSISTANT -> return ActionData.VoiceAssistant
            ActionId.OPEN_DEVICE_ASSISTANT -> return ActionData.DeviceAssistant

            ActionId.OPEN_CAMERA -> return ActionData.OpenCamera
            ActionId.LOCK_DEVICE -> return ActionData.LockDevice
            ActionId.POWER_ON_OFF_DEVICE -> return ActionData.ScreenOnOff
            ActionId.SECURE_LOCK_DEVICE -> return ActionData.SecureLock
            ActionId.CONSUME_KEY_EVENT -> return ActionData.ConsumeKeyEvent
            ActionId.OPEN_SETTINGS -> return ActionData.OpenSettings
            ActionId.SHOW_POWER_MENU -> return ActionData.ShowPowerMenu
            ActionId.DISABLE_DND_MODE -> return ActionData.DoNotDisturb.Disable
            ActionId.DISMISS_MOST_RECENT_NOTIFICATION -> return ActionData.DismissLastNotification
            ActionId.DISMISS_ALL_NOTIFICATIONS -> return ActionData.DismissAllNotifications
            ActionId.ANSWER_PHONE_CALL -> return ActionData.AnswerCall
            ActionId.END_PHONE_CALL -> return ActionData.EndCall
            ActionId.DEVICE_CONTROLS -> return ActionData.DeviceControls
            ActionId.HTTP_REQUEST -> {
                if (oldData == null) {
                    httpRequestBottomSheetState =
                        ActionData.HttpRequest(
                            description = "",
                            method = HttpMethod.GET,
                            url = "http://",
                            body = "",
                            authorizationHeader = "",
                        )
                } else {
                    httpRequestBottomSheetState = oldData as? ActionData.HttpRequest
                }
                return null
            }

            ActionId.SHELL_COMMAND -> {
                val oldAction = oldData as? ActionData.ShellCommand

                return navigate(
                    "config_shell_command_action",
                    NavDestination.ConfigShellCommand(oldAction?.let { Json.encodeToString(oldAction) }),
                )
            }

            ActionId.INTERACT_UI_ELEMENT -> {
                val oldAction = oldData as? ActionData.InteractUiElement

                return navigate(
                    "config_interact_ui_element_action",
                    NavDestination.InteractUiElement(oldAction?.let { Json.encodeToString(it) }),
                )
            }

            ActionId.MOVE_CURSOR -> return createMoverCursorAction()
            ActionId.FORCE_STOP_APP -> return ActionData.ForceStopApp
            ActionId.CLEAR_RECENT_APP -> return ActionData.ClearRecentApp
        }
    }

    private suspend fun createMoverCursorAction(): ActionData.MoveCursor? {
        val choices =
            listOf(
                ActionData.MoveCursor(
                    ActionData.MoveCursor.Type.CHAR,
                    ActionData.MoveCursor.Direction.START,
                ) to getString(R.string.action_move_cursor_prev_character),
                ActionData.MoveCursor(
                    ActionData.MoveCursor.Type.CHAR,
                    ActionData.MoveCursor.Direction.END,
                ) to getString(R.string.action_move_cursor_next_character),
                ActionData.MoveCursor(
                    ActionData.MoveCursor.Type.WORD,
                    ActionData.MoveCursor.Direction.START,
                ) to getString(R.string.action_move_cursor_start_word),
                ActionData.MoveCursor(
                    ActionData.MoveCursor.Type.WORD,
                    ActionData.MoveCursor.Direction.END,
                ) to getString(R.string.action_move_cursor_end_word),
                ActionData.MoveCursor(
                    ActionData.MoveCursor.Type.LINE,
                    ActionData.MoveCursor.Direction.START,
                ) to getString(R.string.action_move_cursor_start_line),
                ActionData.MoveCursor(
                    ActionData.MoveCursor.Type.LINE,
                    ActionData.MoveCursor.Direction.END,
                ) to getString(R.string.action_move_cursor_end_line),
                ActionData.MoveCursor(
                    ActionData.MoveCursor.Type.PARAGRAPH,
                    ActionData.MoveCursor.Direction.START,
                ) to getString(R.string.action_move_cursor_start_paragraph),
                ActionData.MoveCursor(
                    ActionData.MoveCursor.Type.PARAGRAPH,
                    ActionData.MoveCursor.Direction.END,
                ) to getString(R.string.action_move_cursor_end_paragraph),
                ActionData.MoveCursor(
                    ActionData.MoveCursor.Type.PAGE,
                    ActionData.MoveCursor.Direction.START,
                ) to getString(R.string.action_move_cursor_start_page),
                ActionData.MoveCursor(
                    ActionData.MoveCursor.Type.PAGE,
                    ActionData.MoveCursor.Direction.END,
                ) to getString(R.string.action_move_cursor_end_page),
            )

        val dialog = DialogModel.SingleChoice(choices)
        return showDialog("create_move_cursor_action", dialog)
    }
}
