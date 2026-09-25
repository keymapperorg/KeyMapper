package io.github.sds100.keymapper.base.constraints

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.PhysicalOrientation
import io.github.sds100.keymapper.common.utils.SizeKM
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.volume.RingerMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.serialization.json.Json

/**
 * Holds all of the logic to create or edit a [ConstraintData], analogous to
 * [io.github.sds100.keymapper.base.actions.CreateActionDelegate] for actions.
 */
class CreateConstraintDelegate(
    private val coroutineScope: CoroutineScope,
    private val useCase: CreateConstraintUseCase,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
    resourceProvider: ResourceProvider,
) : ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    val constraintResult: MutableStateFlow<ConstraintData?> = MutableStateFlow(null)

    var timeConstraintState: ConstraintData.Time? by mutableStateOf(null)

    var displayResolutionState: DisplayResolutionSheetState? by mutableStateOf(null)
        private set

    fun onSelectDisplayResolution(resolution: SizeKM) {
        displayResolutionState = displayResolutionState?.copy(
            isCustom = false,
            selectedResolution = resolution,
        )
    }

    fun onSelectCustomDisplayResolution() {
        displayResolutionState = displayResolutionState?.copy(isCustom = true)
    }

    fun onDisplayResolutionWidthChange(width: String) {
        displayResolutionState = displayResolutionState?.copy(
            widthText = width.filter(Char::isDigit),
        )
    }

    fun onDisplayResolutionHeightChange(height: String) {
        displayResolutionState = displayResolutionState?.copy(
            heightText = height.filter(Char::isDigit),
        )
    }

    fun onDismissDisplayResolution() {
        displayResolutionState = null
    }

    fun onDoneConfigDisplayResolutionClick() {
        val state = displayResolutionState ?: return
        val resolution = state.resolvedResolution ?: return

        displayResolutionState = null
        constraintResult.update {
            ConstraintData.DisplayResolution(width = resolution.width, height = resolution.height)
        }
    }

    fun onDoneConfigTimeConstraintClick() {
        timeConstraintState?.let { constraintData ->
            timeConstraintState = null
            constraintResult.update { constraintData }
        }
    }

    /**
     * @return the newly created [ConstraintData], or null if the user cancelled.
     */
    suspend fun createConstraint(id: ConstraintId) {
        configConstraint(id)?.let { data -> constraintResult.update { data } }
    }

    /**
     * @param oldData the constraint being edited. Its [ConstraintId] determines which picker is
     * shown, pre-filled with the values from [oldData].
     */
    suspend fun editConstraint(oldData: ConstraintData) {
        if (!oldData.isEditable()) {
            throw IllegalArgumentException(
                "This constraint ${oldData.javaClass.name} can't be edited!",
            )
        }

        configConstraint(oldData.id, oldData)?.let { data -> constraintResult.update { data } }
    }

    private suspend fun configConstraint(
        constraintId: ConstraintId,
        oldData: ConstraintData? = null,
    ): ConstraintData? {
        when (constraintId) {
            ConstraintId.APP_IN_FOREGROUND,
            ConstraintId.APP_PLAYING_MEDIA,
                -> return onSelectAppConstraint(constraintId)

            ConstraintId.MEDIA_PLAYING -> return ConstraintData.MediaPlaying

            ConstraintId.NOTIFICATION_POSTED ->
                return onSelectNotificationConstraint(oldData as? ConstraintData.NotificationPosted)

            ConstraintId.BT_DEVICE_CONNECTED -> return onSelectBluetoothConstraint()

            ConstraintId.SCREEN_ON -> return ConstraintData.ScreenOn

            ConstraintId.DISPLAY_RESOLUTION -> {
                val oldResolution = oldData as? ConstraintData.DisplayResolution
                displayResolutionState = buildDisplayResolutionState(oldResolution)
                return null
            }

            ConstraintId.DISPLAY_ORIENTATION_PORTRAIT -> return ConstraintData.OrientationPortrait

            ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE -> return ConstraintData.OrientationLandscape

            ConstraintId.DISPLAY_ORIENTATION_0 ->
                return ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_0)

            ConstraintId.DISPLAY_ORIENTATION_90 ->
                return ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_90)

            ConstraintId.DISPLAY_ORIENTATION_180 ->
                return ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_180)

            ConstraintId.DISPLAY_ORIENTATION_270 ->
                return ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_270)

            ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT ->
                return ConstraintData.PhysicalOrientation(
                    physicalOrientation = PhysicalOrientation.PORTRAIT,
                )

            ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE ->
                return ConstraintData.PhysicalOrientation(
                    physicalOrientation = PhysicalOrientation.LANDSCAPE,
                )

            ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED ->
                return ConstraintData.PhysicalOrientation(
                    physicalOrientation = PhysicalOrientation.PORTRAIT_INVERTED,
                )

            ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED ->
                return ConstraintData.PhysicalOrientation(
                    physicalOrientation = PhysicalOrientation.LANDSCAPE_INVERTED,
                )

            ConstraintId.FLASHLIGHT_ON -> {
                val lens = chooseFlashlightLens() ?: return null
                return ConstraintData.FlashlightOn(lens = lens)
            }

            ConstraintId.WIFI_ON -> return ConstraintData.WifiOn

            ConstraintId.WIFI_CONNECTED ->
                return onSelectWifiConnectedConstraint(oldData as? ConstraintData.WifiConnected)

            ConstraintId.IME_CHOSEN -> return onSelectImeChosenConstraint()

            ConstraintId.KEYBOARD_SHOWING -> return ConstraintData.KeyboardShowing

            ConstraintId.DEVICE_IS_LOCKED -> return ConstraintData.DeviceIsLocked

            ConstraintId.IN_PHONE_CALL -> return ConstraintData.InPhoneCall

            ConstraintId.NOT_IN_PHONE_CALL -> return ConstraintData.NotInPhoneCall

            ConstraintId.PHONE_RINGING -> return ConstraintData.PhoneRinging

            ConstraintId.RINGER_MODE_NORMAL -> return ConstraintData.RingerMode(RingerMode.NORMAL)

            ConstraintId.RINGER_MODE_VIBRATE -> return ConstraintData.RingerMode(RingerMode.VIBRATE)

            ConstraintId.RINGER_MODE_SILENT -> return ConstraintData.RingerMode(RingerMode.SILENT)

            ConstraintId.CHARGING -> return ConstraintData.Charging

            ConstraintId.HINGE_CLOSED -> return ConstraintData.HingeClosed

            ConstraintId.HINGE_OPEN -> return ConstraintData.HingeOpen

            ConstraintId.LOCK_SCREEN_SHOWING -> return ConstraintData.LockScreenShowing

            ConstraintId.NOTIFICATION_PANEL_SHOWING ->
                return ConstraintData.NotificationPanelShowing

            ConstraintId.TIME -> {
                timeConstraintState = oldData as? ConstraintData.Time ?: ConstraintData.Time(
                    startHour = 0,
                    startMinute = 0,
                    endHour = 0,
                    endMinute = 0,
                )
                return null
            }
        }
    }

    private fun buildDisplayResolutionState(
        oldResolution: ConstraintData.DisplayResolution?,
    ): DisplayResolutionSheetState {
        val supportedResolutions = useCase.getSupportedResolutions()

        val currentResolution = if (oldResolution != null) {
            SizeKM(width = oldResolution.width, height = oldResolution.height)
        } else {
            useCase.getCurrentResolution()
        }

        val matchingResolution = supportedResolutions.firstOrNull {
            it.matchesIgnoringOrientation(currentResolution)
        }

        return DisplayResolutionSheetState(
            supportedResolutions = supportedResolutions.sortedBy { it.width },
            // Show the text fields immediately when there is nothing meaningful to pick
            // from or when the current resolution isn't one of the supported modes.
            isCustom = supportedResolutions.size <= 1 || matchingResolution == null,
            selectedResolution = matchingResolution ?: supportedResolutions.firstOrNull(),
            widthText = currentResolution.width.toString(),
            heightText = currentResolution.height.toString(),
        )
    }

    private suspend fun chooseFlashlightLens(): CameraLens? {
        val items = useCase.getFlashlightLenses().map { lens ->
            val label = when (lens) {
                CameraLens.FRONT -> R.string.lens_front
                CameraLens.BACK -> R.string.lens_back
            }
            lens to getString(label)
        }

        if (items.size == 1) {
            return items.first().first
        }

        val dialog = DialogModel.SingleChoice(items)

        return showDialog("choose_flashlight_lens", dialog)
    }

    private suspend fun onSelectWifiConnectedConstraint(
        oldData: ConstraintData.WifiConnected?,
    ): ConstraintData.WifiConnected? {
        val knownSSIDs: List<String> = useCase.getKnownWiFiSSIDs()

        val chosenSSID: String?

        val savedWifiSSIDs: List<String> = useCase.getSavedWifiSSIDs().first()

        val ssidEntries = buildList {
            addAll(savedWifiSSIDs)
            addAll(knownSSIDs)
        }.distinct()

        val dialog = DialogModel.Text(
            hint = getString(R.string.hint_wifi_ssid),
            allowEmpty = true,
            message = getString(R.string.constraint_wifi_message_cant_list_networks),
            autoCompleteEntries = ssidEntries,
            text = oldData?.ssid.orEmpty(),
        )

        val ssidText = showDialog("type_ssid", dialog) ?: return null

        if (ssidText.isBlank()) {
            chosenSSID = null
        } else {
            chosenSSID = ssidText
            useCase.saveWifiSSID(chosenSSID)
        }

        return ConstraintData.WifiConnected(ssid = chosenSSID)
    }

    private suspend fun onSelectImeChosenConstraint(): ConstraintData.ImeChosen? {
        val inputMethods = useCase.getEnabledInputMethods()
        val items = inputMethods.map { it.id to it.label }
        val dialog = DialogModel.SingleChoice(items = items)

        val result = showDialog("choose_input_method", dialog) ?: return null

        val imeInfo = inputMethods.single { it.id == result }

        return ConstraintData.ImeChosen(imeId = imeInfo.id, imeLabel = imeInfo.label)
    }

    private suspend fun onSelectBluetoothConstraint(): ConstraintData.BtDeviceConnected? {
        val response = showDialog(
            "bluetooth_device_constraint_limitation",
            DialogModel.Ok(getString(R.string.dialog_message_bt_constraint_limitation)),
        )

        response ?: return null

        val device = navigate(
            "choose_bluetooth_device_for_constraint",
            NavDestination.ChooseBluetoothDevice,
        ) ?: return null

        return ConstraintData.BtDeviceConnected(
            bluetoothAddress = device.address,
            deviceName = device.name,
        )
    }

    private suspend fun onSelectNotificationConstraint(
        oldData: ConstraintData.NotificationPosted?,
    ): ConstraintData.NotificationPosted? = navigate(
        "config_notification_constraint",
        NavDestination.ConfigNotificationConstraint(oldData?.let { Json.encodeToString(it) }),
    )

    private suspend fun onSelectAppConstraint(type: ConstraintId): ConstraintData? {
        val result =
            navigate(
                "choose_package_for_constraint",
                NavDestination.ChooseApp(allowHiddenApps = true),
            )
                ?: return null

        return when (type) {
            ConstraintId.APP_IN_FOREGROUND -> ConstraintData.AppInForeground(
                packageName = result.packageName,
                appName = result.appName,
            )

            ConstraintId.APP_PLAYING_MEDIA -> ConstraintData.AppPlayingMedia(
                packageName = result.packageName,
                appName = result.appName,
            )

            else -> throw IllegalArgumentException(
                "Don't know how to create $type constraint after choosing app",
            )
        }
    }
}

/**
 * State for the display resolution bottom sheet.
 *
 * @param supportedResolutions the resolutions the display reports as supported.
 * @param isCustom whether the user is entering a custom resolution instead of picking a chip.
 * @param selectedResolution the currently selected supported resolution, if any.
 * @param widthText the custom width input.
 * @param heightText the custom height input.
 */
data class DisplayResolutionSheetState(
    val supportedResolutions: List<SizeKM>,
    val isCustom: Boolean,
    val selectedResolution: SizeKM?,
    val widthText: String,
    val heightText: String,
) {
    private val customWidth: Int? get() = widthText.toIntOrNull()
    private val customHeight: Int? get() = heightText.toIntOrNull()

    /**
     * The resolution that will be saved, or null when the current input is not valid.
     */
    val resolvedResolution: SizeKM?
        get() = if (isCustom) {
            val width = customWidth
            val height = customHeight

            if (width != null && width > 0 && height != null && height > 0) {
                SizeKM(width, height)
            } else {
                null
            }
        } else {
            selectedResolution
        }

    val isValid: Boolean get() = resolvedResolution != null
}

/**
 * Compares two resolutions ignoring orientation so that e.g. 1080x1920 and 1920x1080
 * are treated as the same resolution.
 */
private fun SizeKM.matchesIgnoringOrientation(other: SizeKM): Boolean {
    return (width == other.width && height == other.height) ||
        (width == other.height && height == other.width)
}
