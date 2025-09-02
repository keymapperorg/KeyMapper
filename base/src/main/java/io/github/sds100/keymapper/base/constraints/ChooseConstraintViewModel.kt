package io.github.sds100.keymapper.base.constraints

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
import io.github.sds100.keymapper.base.utils.ui.compose.SimpleListItemModel
import io.github.sds100.keymapper.base.utils.ui.showDialog
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.system.camera.CameraLens
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
import javax.inject.Inject

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
        private val ALL_CONSTRAINTS_ORDERED: Array<ConstraintId> = arrayOf(
            ConstraintId.APP_IN_FOREGROUND,
            ConstraintId.APP_NOT_IN_FOREGROUND,
            ConstraintId.APP_PLAYING_MEDIA,
            ConstraintId.APP_NOT_PLAYING_MEDIA,
            ConstraintId.MEDIA_PLAYING,
            ConstraintId.MEDIA_NOT_PLAYING,

            ConstraintId.BT_DEVICE_CONNECTED,
            ConstraintId.BT_DEVICE_DISCONNECTED,

            ConstraintId.SCREEN_ON,
            ConstraintId.SCREEN_OFF,

            ConstraintId.ORIENTATION_PORTRAIT,
            ConstraintId.ORIENTATION_LANDSCAPE,
            ConstraintId.ORIENTATION_0,
            ConstraintId.ORIENTATION_90,
            ConstraintId.ORIENTATION_180,
            ConstraintId.ORIENTATION_270,

            ConstraintId.FLASHLIGHT_ON,
            ConstraintId.FLASHLIGHT_OFF,

            ConstraintId.WIFI_ON,
            ConstraintId.WIFI_OFF,
            ConstraintId.WIFI_CONNECTED,
            ConstraintId.WIFI_DISCONNECTED,

            ConstraintId.IME_CHOSEN,
            ConstraintId.IME_NOT_CHOSEN,

            ConstraintId.DEVICE_IS_LOCKED,
            ConstraintId.DEVICE_IS_UNLOCKED,
            ConstraintId.LOCK_SCREEN_SHOWING,
            ConstraintId.LOCK_SCREEN_NOT_SHOWING,

            ConstraintId.IN_PHONE_CALL,
            ConstraintId.NOT_IN_PHONE_CALL,
            ConstraintId.PHONE_RINGING,

            ConstraintId.CHARGING,
            ConstraintId.DISCHARGING,

            ConstraintId.TIME,
        )
    }

    private val returnResult = MutableSharedFlow<Constraint>()

    private val allListItems: List<SimpleListItemModel> by lazy { buildListItems() }

    val searchQuery = MutableStateFlow<String?>(null)

    val listItems: StateFlow<State<List<SimpleListItemModel>>> =
        searchQuery.map { query ->
            val filteredItems = allListItems.filter { it.title.containsQuery(query) }
            State.Data(filteredItems)
        }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    var timeConstraintState: Constraint.Time? by mutableStateOf(null)

    init {
        viewModelScope.launch {
            returnResult.collect { constraint ->
                popBackStackWithResult(Json.encodeToString(constraint))
            }
        }
    }

    fun onDoneConfigTimeConstraintClick() {
        timeConstraintState?.let { constraint ->
            viewModelScope.launch {
                returnResult.emit(constraint)
                timeConstraintState = null
            }
        }
    }

    fun onNavigateBack() {
        viewModelScope.launch {
            popBackStack()
        }
    }

    fun onListItemClick(id: String) {
        viewModelScope.launch {
            when (val constraintType = ConstraintId.valueOf(id)) {
                ConstraintId.APP_IN_FOREGROUND,
                ConstraintId.APP_NOT_IN_FOREGROUND,
                ConstraintId.APP_PLAYING_MEDIA,
                ConstraintId.APP_NOT_PLAYING_MEDIA,
                -> onSelectAppConstraint(constraintType)

                ConstraintId.MEDIA_PLAYING -> returnResult.emit(Constraint.MediaPlaying())
                ConstraintId.MEDIA_NOT_PLAYING -> returnResult.emit(Constraint.NoMediaPlaying())

                ConstraintId.BT_DEVICE_CONNECTED,
                ConstraintId.BT_DEVICE_DISCONNECTED,
                -> onSelectBluetoothConstraint(
                    constraintType,
                )

                ConstraintId.SCREEN_ON -> onSelectScreenOnConstraint()
                ConstraintId.SCREEN_OFF -> onSelectScreenOffConstraint()

                ConstraintId.ORIENTATION_PORTRAIT ->
                    returnResult.emit(Constraint.OrientationPortrait())

                ConstraintId.ORIENTATION_LANDSCAPE ->
                    returnResult.emit(Constraint.OrientationLandscape())

                ConstraintId.ORIENTATION_0 ->
                    returnResult.emit(Constraint.OrientationCustom(orientation = Orientation.ORIENTATION_0))

                ConstraintId.ORIENTATION_90 ->
                    returnResult.emit(Constraint.OrientationCustom(orientation = Orientation.ORIENTATION_90))

                ConstraintId.ORIENTATION_180 ->
                    returnResult.emit(Constraint.OrientationCustom(orientation = Orientation.ORIENTATION_180))

                ConstraintId.ORIENTATION_270 ->
                    returnResult.emit(Constraint.OrientationCustom(orientation = Orientation.ORIENTATION_270))

                ConstraintId.FLASHLIGHT_ON -> {
                    val lens = chooseFlashlightLens() ?: return@launch
                    returnResult.emit(Constraint.FlashlightOn(lens = lens))
                }

                ConstraintId.FLASHLIGHT_OFF -> {
                    val lens = chooseFlashlightLens() ?: return@launch
                    returnResult.emit(Constraint.FlashlightOff(lens = lens))
                }

                ConstraintId.WIFI_ON -> returnResult.emit(Constraint.WifiOn())
                ConstraintId.WIFI_OFF -> returnResult.emit(Constraint.WifiOff())

                ConstraintId.WIFI_CONNECTED,
                ConstraintId.WIFI_DISCONNECTED,
                -> onSelectWifiConnectedConstraint(
                    constraintType,
                )

                ConstraintId.IME_CHOSEN,
                ConstraintId.IME_NOT_CHOSEN,
                -> onSelectImeChosenConstraint(constraintType)

                ConstraintId.DEVICE_IS_LOCKED ->
                    returnResult.emit(Constraint.DeviceIsLocked())

                ConstraintId.DEVICE_IS_UNLOCKED ->
                    returnResult.emit(Constraint.DeviceIsUnlocked())

                ConstraintId.IN_PHONE_CALL ->
                    returnResult.emit(Constraint.InPhoneCall())

                ConstraintId.NOT_IN_PHONE_CALL ->
                    returnResult.emit(Constraint.NotInPhoneCall())

                ConstraintId.PHONE_RINGING ->
                    returnResult.emit(Constraint.PhoneRinging())

                ConstraintId.CHARGING ->
                    returnResult.emit(Constraint.Charging())

                ConstraintId.DISCHARGING ->
                    returnResult.emit(Constraint.Discharging())

                ConstraintId.LOCK_SCREEN_SHOWING ->
                    returnResult.emit(Constraint.LockScreenShowing())

                ConstraintId.LOCK_SCREEN_NOT_SHOWING ->
                    returnResult.emit(Constraint.LockScreenNotShowing())

                ConstraintId.TIME -> {
                    timeConstraintState = Constraint.Time(
                        startHour = 0,
                        startMinute = 0,
                        endHour = 0,
                        endMinute = 0,
                    )
                }
            }
        }
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

    private fun buildListItems(): List<SimpleListItemModel> = buildList {
        ALL_CONSTRAINTS_ORDERED.forEach { id ->
            val title = getString(ConstraintUtils.getTitleStringId(id))
            val icon = ConstraintUtils.getIcon(id)
            val error = useCase.isSupported(id)

            val listItem = SimpleListItemModel(
                id = id.toString(),
                title = title,
                icon = icon,
                subtitle = error?.getFullMessage(this@ChooseConstraintViewModel),
                isSubtitleError = true,
                isEnabled = error == null,
            )

            add(listItem)
        }
    }

    private suspend fun onSelectWifiConnectedConstraint(type: ConstraintId) {
        val knownSSIDs = useCase.getKnownWiFiSSIDs()

        val chosenSSID: String?

        if (knownSSIDs == null) {
            val savedWifiSSIDs = useCase.getSavedWifiSSIDs().first()

            val dialog = DialogModel.Text(
                hint = getString(R.string.hint_wifi_ssid),
                allowEmpty = true,
                message = getString(R.string.constraint_wifi_message_cant_list_networks),
                autoCompleteEntries = savedWifiSSIDs,
            )

            val ssidText = showDialog("type_ssid", dialog) ?: return

            if (ssidText.isBlank()) {
                chosenSSID = null
            } else {
                chosenSSID = ssidText

                useCase.saveWifiSSID(chosenSSID)
            }
        } else {
            val anySSIDItem =
                "any" to getString(R.string.constraint_wifi_pick_network_any)

            val ssidItems = knownSSIDs.map { "ssid_$it" to it }

            val items = listOf(anySSIDItem).plus(ssidItems)

            val chosenItem =
                showDialog("choose_ssid", DialogModel.SingleChoice(items)) ?: return

            if (chosenItem == anySSIDItem.first) {
                chosenSSID = null
            } else {
                chosenSSID = items.single { it.first == chosenItem }.second
            }
        }

        when (type) {
            ConstraintId.WIFI_CONNECTED ->
                returnResult.emit(Constraint.WifiConnected(ssid = chosenSSID))

            ConstraintId.WIFI_DISCONNECTED ->
                returnResult.emit(Constraint.WifiDisconnected(ssid = chosenSSID))

            else -> Unit
        }
    }

    private suspend fun onSelectImeChosenConstraint(type: ConstraintId) {
        val inputMethods = useCase.getEnabledInputMethods()
        val items = inputMethods.map { it.id to it.label }
        val dialog = DialogModel.SingleChoice(items = items)

        val result = showDialog("choose_input_method", dialog) ?: return

        val imeInfo = inputMethods.single { it.id == result }

        when (type) {
            ConstraintId.IME_CHOSEN ->
                returnResult.emit(
                    Constraint.ImeChosen(
                        imeId = imeInfo.id,
                        imeLabel = imeInfo.label,
                    ),
                )

            ConstraintId.IME_NOT_CHOSEN ->
                returnResult.emit(
                    Constraint.ImeNotChosen(
                        imeId = imeInfo.id,
                        imeLabel = imeInfo.label,
                    ),
                )

            else -> Unit
        }
    }

    private suspend fun onSelectScreenOnConstraint() {
        val response = showDialog(
            "screen_on_constraint_limitation",
            DialogModel.Ok(getString(R.string.dialog_message_screen_constraints_limitation)),
        )

        response ?: return

        returnResult.emit(Constraint.ScreenOn())
    }

    private suspend fun onSelectScreenOffConstraint() {
        val response = showDialog(
            "screen_on_constraint_limitation",
            DialogModel.Ok(getString(R.string.dialog_message_screen_constraints_limitation)),
        )

        response ?: return

        returnResult.emit(Constraint.ScreenOff())
    }

    private suspend fun onSelectBluetoothConstraint(type: ConstraintId) {
        val response = showDialog(
            "bluetooth_device_constraint_limitation",
            DialogModel.Ok(getString(R.string.dialog_message_bt_constraint_limitation)),
        )

        response ?: return

        val device = navigate(
            "choose_bluetooth_device_for_constraint",
            NavDestination.ChooseBluetoothDevice,
        ) ?: return

        val constraint = when (type) {
            ConstraintId.BT_DEVICE_CONNECTED -> Constraint.BtDeviceConnected(
                bluetoothAddress = device.address,
                deviceName = device.name,
            )

            ConstraintId.BT_DEVICE_DISCONNECTED -> Constraint.BtDeviceDisconnected(
                bluetoothAddress = device.address,
                deviceName = device.name,
            )

            else -> throw IllegalArgumentException("Don't know how to create $type constraint after choosing app")
        }

        returnResult.emit(constraint)
    }

    private suspend fun onSelectAppConstraint(type: ConstraintId) {
        val packageName =
            navigate(
                "choose_package_for_constraint",
                NavDestination.ChooseApp(allowHiddenApps = true),
            )
                ?: return

        val constraint = when (type) {
            ConstraintId.APP_IN_FOREGROUND -> Constraint.AppInForeground(
                packageName = packageName,
            )

            ConstraintId.APP_NOT_IN_FOREGROUND -> Constraint.AppNotInForeground(
                packageName = packageName,
            )

            ConstraintId.APP_PLAYING_MEDIA -> Constraint.AppPlayingMedia(
                packageName = packageName,
            )

            ConstraintId.APP_NOT_PLAYING_MEDIA -> Constraint.AppNotPlayingMedia(
                packageName = packageName,
            )

            else -> throw IllegalArgumentException("Don't know how to create $type constraint after choosing app")
        }

        returnResult.emit(constraint)
    }
}
