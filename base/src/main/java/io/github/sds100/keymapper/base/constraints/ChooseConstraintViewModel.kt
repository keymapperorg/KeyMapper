package io.github.sds100.keymapper.base.constraints

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.StayCurrentPortrait
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.base.utils.containsQuery
import io.github.sds100.keymapper.base.utils.getFullMessage
import io.github.sds100.keymapper.base.utils.navigation.NavDestination
import io.github.sds100.keymapper.base.utils.navigation.NavigationProvider
import io.github.sds100.keymapper.base.utils.navigation.navigate
import io.github.sds100.keymapper.base.utils.ui.DialogModel
import io.github.sds100.keymapper.base.utils.ui.DialogProvider
import io.github.sds100.keymapper.base.utils.ui.ResourceProvider
import io.github.sds100.keymapper.base.utils.ui.compose.ComposeIconInfo
import io.github.sds100.keymapper.base.utils.ui.compose.SimpleListItemGroup
import io.github.sds100.keymapper.base.utils.ui.compose.SimpleListItemModel
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.PhysicalOrientation
import io.github.sds100.keymapper.common.utils.SizeKM
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.volume.RingerMode
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@HiltViewModel
class ChooseConstraintViewModel @Inject constructor(
    private val useCase: CreateConstraintUseCase,
    dialogProvider: DialogProvider,
    navigationProvider: NavigationProvider,
    resourceProvider: ResourceProvider,
) : ViewModel(),
    ResourceProvider by resourceProvider,
    DialogProvider by dialogProvider,
    NavigationProvider by navigationProvider {

    companion object {
        // Synthetic IDs for consolidated orientation list items (not actual ConstraintIds)
        private const val DISPLAY_ORIENTATION_LIST_ITEM_ID = "display_orientation"
        private const val PHYSICAL_ORIENTATION_LIST_ITEM_ID = "physical_orientation"

        private val CATEGORY_ORDER = arrayOf(
            ConstraintCategory.APPS,
            ConstraintCategory.MEDIA,
            ConstraintCategory.BLUETOOTH,
            ConstraintCategory.DISPLAY,
            ConstraintCategory.FLASHLIGHT,
            ConstraintCategory.WIFI,
            ConstraintCategory.KEYBOARD,
            ConstraintCategory.LOCK,
            ConstraintCategory.PHONE,
            ConstraintCategory.POWER,
            ConstraintCategory.DEVICE,
            ConstraintCategory.NOTIFICATIONS,
            ConstraintCategory.TIME,
        )
    }

    private val returnResult = MutableSharedFlow<ConstraintData>()

    private val allGroupedListItems: List<SimpleListItemGroup> by lazy { buildListGroups() }

    val searchQuery = MutableStateFlow<String?>(null)

    val groups: StateFlow<State<List<SimpleListItemGroup>>> =
        searchQuery.map { query ->
            val groups = allGroupedListItems.mapNotNull { group ->

                val filteredItems = group.items.filter { it.title.containsQuery(query) }

                if (filteredItems.isEmpty()) {
                    return@mapNotNull null
                } else {
                    group.copy(items = filteredItems)
                }
            }

            State.Data(groups)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    var timeConstraintState: ConstraintData.Time? by mutableStateOf(null)

    var displayResolutionState: DisplayResolutionSheetState? by mutableStateOf(null)
        private set

    init {
        viewModelScope.launch {
            returnResult.collect { constraintData ->
                popBackStackWithResult(Json.encodeToString(constraintData))
            }
        }
    }

    fun onDoneConfigTimeConstraintClick() {
        timeConstraintState?.let { constraintData ->
            viewModelScope.launch {
                returnResult.emit(constraintData)
                timeConstraintState = null
            }
        }
    }

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

        viewModelScope.launch {
            returnResult.emit(
                ConstraintData.DisplayResolution(
                    width = resolution.width,
                    height = resolution.height,
                ),
            )
            displayResolutionState = null
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            popBackStack()
        }
    }

    fun onListItemClick(id: String) {
        viewModelScope.launch {
            // Handle synthetic list item IDs for consolidated orientation constraints
            when (id) {
                DISPLAY_ORIENTATION_LIST_ITEM_ID -> {
                    onSelectDisplayOrientationConstraint()
                    return@launch
                }

                PHYSICAL_ORIENTATION_LIST_ITEM_ID -> {
                    onSelectPhysicalOrientationConstraint()
                    return@launch
                }
            }

            when (val constraintType = ConstraintId.valueOf(id)) {
                ConstraintId.APP_IN_FOREGROUND,
                ConstraintId.APP_PLAYING_MEDIA,
                    -> onSelectAppConstraint(constraintType)

                ConstraintId.MEDIA_PLAYING -> returnResult.emit(ConstraintData.MediaPlaying)

                ConstraintId.NOTIFICATION_POSTED -> onSelectNotificationConstraint()

                ConstraintId.BT_DEVICE_CONNECTED -> onSelectBluetoothConstraint()

                ConstraintId.SCREEN_ON -> returnResult.emit(ConstraintData.ScreenOn)

                ConstraintId.DISPLAY_RESOLUTION -> {
                    displayResolutionState = buildDisplayResolutionState()
                }

                ConstraintId.DISPLAY_ORIENTATION_PORTRAIT ->
                    returnResult.emit(ConstraintData.OrientationPortrait)

                ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE ->
                    returnResult.emit(ConstraintData.OrientationLandscape)

                ConstraintId.DISPLAY_ORIENTATION_0 ->
                    returnResult.emit(
                        ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_0),
                    )

                ConstraintId.DISPLAY_ORIENTATION_90 ->
                    returnResult.emit(
                        ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_90),
                    )

                ConstraintId.DISPLAY_ORIENTATION_180 ->
                    returnResult.emit(
                        ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_180),
                    )

                ConstraintId.DISPLAY_ORIENTATION_270 ->
                    returnResult.emit(
                        ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_270),
                    )

                ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT ->
                    returnResult.emit(
                        ConstraintData.PhysicalOrientation(
                            physicalOrientation = PhysicalOrientation.PORTRAIT,
                        ),
                    )

                ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE ->
                    returnResult.emit(
                        ConstraintData.PhysicalOrientation(
                            physicalOrientation = PhysicalOrientation.LANDSCAPE,
                        ),
                    )

                ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED ->
                    returnResult.emit(
                        ConstraintData.PhysicalOrientation(
                            physicalOrientation = PhysicalOrientation.PORTRAIT_INVERTED,
                        ),
                    )

                ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED ->
                    returnResult.emit(
                        ConstraintData.PhysicalOrientation(
                            physicalOrientation = PhysicalOrientation.LANDSCAPE_INVERTED,
                        ),
                    )

                ConstraintId.FLASHLIGHT_ON -> {
                    val lens = chooseFlashlightLens() ?: return@launch
                    returnResult.emit(ConstraintData.FlashlightOn(lens = lens))
                }

                ConstraintId.WIFI_ON -> returnResult.emit(ConstraintData.WifiOn)

                ConstraintId.WIFI_CONNECTED -> onSelectWifiConnectedConstraint()

                ConstraintId.IME_CHOSEN -> onSelectImeChosenConstraint()

                ConstraintId.KEYBOARD_SHOWING ->
                    returnResult.emit(ConstraintData.KeyboardShowing)

                ConstraintId.DEVICE_IS_LOCKED ->
                    returnResult.emit(ConstraintData.DeviceIsLocked)

                ConstraintId.IN_PHONE_CALL ->
                    returnResult.emit(ConstraintData.InPhoneCall)

                ConstraintId.NOT_IN_PHONE_CALL ->
                    returnResult.emit(ConstraintData.NotInPhoneCall)

                ConstraintId.PHONE_RINGING ->
                    returnResult.emit(ConstraintData.PhoneRinging)

                ConstraintId.RINGER_MODE_NORMAL ->
                    returnResult.emit(ConstraintData.RingerMode(RingerMode.NORMAL))

                ConstraintId.RINGER_MODE_VIBRATE ->
                    returnResult.emit(ConstraintData.RingerMode(RingerMode.VIBRATE))

                ConstraintId.RINGER_MODE_SILENT ->
                    returnResult.emit(ConstraintData.RingerMode(RingerMode.SILENT))

                ConstraintId.CHARGING ->
                    returnResult.emit(ConstraintData.Charging)

                ConstraintId.HINGE_CLOSED ->
                    returnResult.emit(ConstraintData.HingeClosed)

                ConstraintId.HINGE_OPEN ->
                    returnResult.emit(ConstraintData.HingeOpen)

                ConstraintId.LOCK_SCREEN_SHOWING ->
                    returnResult.emit(ConstraintData.LockScreenShowing)

                ConstraintId.NOTIFICATION_PANEL_SHOWING ->
                    returnResult.emit(ConstraintData.NotificationPanelShowing)

                ConstraintId.TIME -> {
                    timeConstraintState = ConstraintData.Time(
                        startHour = 0,
                        startMinute = 0,
                        endHour = 0,
                        endMinute = 0,
                    )
                }
            }
        }
    }

    private fun buildDisplayResolutionState(): DisplayResolutionSheetState {
        val supportedResolutions = useCase.getSupportedResolutions()
        val currentResolution = useCase.getCurrentResolution()

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

        val cameraLens = showDialog("choose_flashlight_lens", dialog)

        return cameraLens
    }

    private suspend fun onSelectDisplayOrientationConstraint() {
        val items = listOf(
            ConstraintId.DISPLAY_ORIENTATION_PORTRAIT to
                getString(R.string.constraint_choose_orientation_portrait),
            ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE to
                getString(R.string.constraint_choose_orientation_landscape),
            ConstraintId.DISPLAY_ORIENTATION_0 to
                getString(R.string.constraint_choose_orientation_0),
            ConstraintId.DISPLAY_ORIENTATION_90 to
                getString(R.string.constraint_choose_orientation_90),
            ConstraintId.DISPLAY_ORIENTATION_180 to
                getString(R.string.constraint_choose_orientation_180),
            ConstraintId.DISPLAY_ORIENTATION_270 to
                getString(R.string.constraint_choose_orientation_270),
        )

        val dialog = DialogModel.SingleChoice(items)
        val selectedOrientation = showDialog("choose_display_orientation", dialog) ?: return

        val constraintData = when (selectedOrientation) {
            ConstraintId.DISPLAY_ORIENTATION_PORTRAIT -> ConstraintData.OrientationPortrait

            ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE -> ConstraintData.OrientationLandscape

            ConstraintId.DISPLAY_ORIENTATION_0 ->
                ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_0)

            ConstraintId.DISPLAY_ORIENTATION_90 ->
                ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_90)

            ConstraintId.DISPLAY_ORIENTATION_180 ->
                ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_180)

            ConstraintId.DISPLAY_ORIENTATION_270 ->
                ConstraintData.OrientationCustom(orientation = Orientation.ORIENTATION_270)

            else -> return
        }

        returnResult.emit(constraintData)
    }

    private suspend fun onSelectPhysicalOrientationConstraint() {
        val items = listOf(
            PhysicalOrientation.PORTRAIT to
                getString(R.string.constraint_choose_physical_orientation_portrait),
            PhysicalOrientation.LANDSCAPE to
                getString(R.string.constraint_choose_physical_orientation_landscape),
            PhysicalOrientation.PORTRAIT_INVERTED to
                getString(R.string.constraint_choose_physical_orientation_portrait_inverted),
            PhysicalOrientation.LANDSCAPE_INVERTED to
                getString(R.string.constraint_choose_physical_orientation_landscape_inverted),
        )

        val dialog = DialogModel.SingleChoice(items)
        val selectedOrientation = showDialog("choose_physical_orientation", dialog) ?: return

        returnResult.emit(
            ConstraintData.PhysicalOrientation(physicalOrientation = selectedOrientation),
        )
    }

    private fun buildListGroups(): List<SimpleListItemGroup> = buildList {
        // Filter out individual orientation constraints - show only the consolidated ones
        val filteredConstraints = ConstraintId.entries.filter { constraintId ->
            constraintId !in listOf(
                ConstraintId.DISPLAY_ORIENTATION_PORTRAIT,
                ConstraintId.DISPLAY_ORIENTATION_LANDSCAPE,
                ConstraintId.DISPLAY_ORIENTATION_0,
                ConstraintId.DISPLAY_ORIENTATION_90,
                ConstraintId.DISPLAY_ORIENTATION_180,
                ConstraintId.DISPLAY_ORIENTATION_270,
                ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT,
                ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE,
                ConstraintId.PHYSICAL_ORIENTATION_PORTRAIT_INVERTED,
                ConstraintId.PHYSICAL_ORIENTATION_LANDSCAPE_INVERTED,
            )
        }

        val listItems = buildListItems(filteredConstraints)

        // Add synthetic orientation list items
        val displayOrientationItem = SimpleListItemModel(
            id = DISPLAY_ORIENTATION_LIST_ITEM_ID,
            title = getString(R.string.constraint_choose_screen_orientation),
            icon = ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait),
            isEnabled = true,
        )

        val physicalOrientationItem = SimpleListItemModel(
            id = PHYSICAL_ORIENTATION_LIST_ITEM_ID,
            title = getString(R.string.constraint_choose_physical_orientation),
            icon = ComposeIconInfo.Vector(Icons.Outlined.StayCurrentPortrait),
            isEnabled = true,
        )

        for (category in CATEGORY_ORDER) {
            val header = getString(ConstraintUtils.getCategoryLabel(category))

            val categoryItems = listItems.filter { item ->
                item.isEnabled &&
                    try {
                        ConstraintUtils.getCategory(ConstraintId.valueOf(item.id)) == category
                    } catch (e: IllegalArgumentException) {
                        false
                    }
            }.toMutableList()

            // Add synthetic orientation items to DISPLAY category
            if (category == ConstraintCategory.DISPLAY) {
                categoryItems.add(displayOrientationItem)
                categoryItems.add(physicalOrientationItem)
            }

            val group = SimpleListItemGroup(
                header,
                items = categoryItems,
            )

            if (group.items.isNotEmpty()) {
                add(group)
            }
        }

        val unsupportedItems = listItems.filter { !it.isEnabled }
        if (unsupportedItems.isNotEmpty()) {
            val unsupportedGroup = SimpleListItemGroup(
                header = getString(R.string.choose_constraint_group_unsupported),
                items = unsupportedItems,
            )
            add(unsupportedGroup)
        }
    }

    private fun buildListItems(constraintIds: List<ConstraintId>): List<SimpleListItemModel> =
        buildList {
            for (constraintId in constraintIds) {
                val title = getString(ConstraintUtils.getTitleStringId(constraintId))
                val icon = ConstraintUtils.getIcon(constraintId)
                val error = useCase.isSupported(constraintId)

                val listItem = SimpleListItemModel(
                    id = constraintId.toString(),
                    title = title,
                    icon = icon,
                    subtitle = error?.getFullMessage(this@ChooseConstraintViewModel),
                    isSubtitleError = true,
                    isEnabled = error == null,
                )

                add(listItem)
            }
        }

    private suspend fun onSelectWifiConnectedConstraint() {
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
        )

        val ssidText = showDialog("type_ssid", dialog) ?: return

        if (ssidText.isBlank()) {
            chosenSSID = null
        } else {
            chosenSSID = ssidText

            useCase.saveWifiSSID(chosenSSID)
        }

        returnResult.emit(ConstraintData.WifiConnected(ssid = chosenSSID))
    }

    private suspend fun onSelectImeChosenConstraint() {
        val inputMethods = useCase.getEnabledInputMethods()
        val items = inputMethods.map { it.id to it.label }
        val dialog = DialogModel.SingleChoice(items = items)

        val result = showDialog("choose_input_method", dialog) ?: return

        val imeInfo = inputMethods.single { it.id == result }

        returnResult.emit(
            ConstraintData.ImeChosen(
                imeId = imeInfo.id,
                imeLabel = imeInfo.label,
            ),
        )
    }

    private suspend fun onSelectBluetoothConstraint() {
        val response = showDialog(
            "bluetooth_device_constraint_limitation",
            DialogModel.Ok(getString(R.string.dialog_message_bt_constraint_limitation)),
        )

        response ?: return

        val device = navigate(
            "choose_bluetooth_device_for_constraint",
            NavDestination.ChooseBluetoothDevice,
        ) ?: return

        returnResult.emit(
            ConstraintData.BtDeviceConnected(
                bluetoothAddress = device.address,
                deviceName = device.name,
            ),
        )
    }

    private suspend fun onSelectNotificationConstraint() {
        val constraintData = navigate(
            "config_notification_constraint",
            NavDestination.ConfigNotificationConstraint,
        ) ?: return

        returnResult.emit(constraintData)
    }

    private suspend fun onSelectAppConstraint(type: ConstraintId) {
        val result =
            navigate(
                "choose_package_for_constraint",
                NavDestination.ChooseApp(allowHiddenApps = true),
            )
                ?: return

        val constraintData = when (type) {
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

        returnResult.emit(constraintData)
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
