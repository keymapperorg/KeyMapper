package io.github.sds100.keymapper.constraints

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.camera.CameraLens
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.getFullMessage
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by sds100 on 21/03/2020.
 */

@HiltViewModel
class ChooseConstraintViewModel @Inject constructor(
    private val useCase: CreateConstraintUseCase,
    resourceProvider: ResourceProvider
) : ViewModel(),
    ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl(),
    NavigationViewModel by NavigationViewModelImpl() {

    companion object {
        private val ALL_CONSTRAINTS_ORDERED: Array<ChooseConstraintType> = arrayOf(
            ChooseConstraintType.APP_IN_FOREGROUND,
            ChooseConstraintType.APP_NOT_IN_FOREGROUND,
            ChooseConstraintType.APP_PLAYING_MEDIA,
            ChooseConstraintType.APP_NOT_PLAYING_MEDIA,
            ChooseConstraintType.MEDIA_PLAYING,
            ChooseConstraintType.MEDIA_NOT_PLAYING,

            ChooseConstraintType.BT_DEVICE_CONNECTED,
            ChooseConstraintType.BT_DEVICE_DISCONNECTED,

            ChooseConstraintType.SCREEN_ON,
            ChooseConstraintType.SCREEN_OFF,

            ChooseConstraintType.ORIENTATION_PORTRAIT,
            ChooseConstraintType.ORIENTATION_LANDSCAPE,
            ChooseConstraintType.ORIENTATION_0,
            ChooseConstraintType.ORIENTATION_90,
            ChooseConstraintType.ORIENTATION_180,
            ChooseConstraintType.ORIENTATION_270,

            ChooseConstraintType.FLASHLIGHT_ON,
            ChooseConstraintType.FLASHLIGHT_OFF,

            ChooseConstraintType.WIFI_ON,
            ChooseConstraintType.WIFI_OFF,
            ChooseConstraintType.WIFI_CONNECTED,
            ChooseConstraintType.WIFI_DISCONNECTED,

            ChooseConstraintType.IME_CHOSEN,
            ChooseConstraintType.IME_NOT_CHOSEN,

            ChooseConstraintType.DEVICE_IS_LOCKED,
            ChooseConstraintType.DEVICE_IS_UNLOCKED,

            ChooseConstraintType.IN_PHONE_CALL,
            ChooseConstraintType.NOT_IN_PHONE_CALL,
            ChooseConstraintType.PHONE_RINGING,

            ChooseConstraintType.CHARGING,
            ChooseConstraintType.DISCHARGING
        )
    }

    private val _listItems = MutableStateFlow<State<List<SimpleListItem>>>(State.Loading)
    val listItems = _listItems.asStateFlow()

    private val _returnResult = MutableSharedFlow<Constraint>()
    val returnResult = _returnResult.asSharedFlow()

    private var supportedConstraints = MutableStateFlow<Array<ChooseConstraintType>>(emptyArray())

    init {
        viewModelScope.launch(Dispatchers.Default) {
            supportedConstraints.collectLatest {
                _listItems.value = State.Data(buildListItems())
            }
        }
    }

    fun setSupportedConstraints(supportedConstraints: Array<ChooseConstraintType>) {
        this.supportedConstraints.value = supportedConstraints
    }

    fun onListItemClick(id: String) {
        viewModelScope.launch {
            val constraintType = ChooseConstraintType.valueOf(id)

            when (constraintType) {
                ChooseConstraintType.APP_IN_FOREGROUND,
                ChooseConstraintType.APP_NOT_IN_FOREGROUND,
                ChooseConstraintType.APP_PLAYING_MEDIA,
                ChooseConstraintType.APP_NOT_PLAYING_MEDIA -> onSelectAppConstraint(constraintType)

                ChooseConstraintType.MEDIA_PLAYING -> _returnResult.emit(Constraint.MediaPlaying)
                ChooseConstraintType.MEDIA_NOT_PLAYING -> _returnResult.emit(Constraint.NoMediaPlaying)

                ChooseConstraintType.BT_DEVICE_CONNECTED,
                ChooseConstraintType.BT_DEVICE_DISCONNECTED -> onSelectBluetoothConstraint(
                    constraintType
                )

                ChooseConstraintType.SCREEN_ON -> onSelectScreenOnConstraint()
                ChooseConstraintType.SCREEN_OFF -> onSelectScreenOffConstraint()

                ChooseConstraintType.ORIENTATION_PORTRAIT ->
                    _returnResult.emit(Constraint.OrientationPortrait)

                ChooseConstraintType.ORIENTATION_LANDSCAPE ->
                    _returnResult.emit(Constraint.OrientationLandscape)

                ChooseConstraintType.ORIENTATION_0 ->
                    _returnResult.emit(Constraint.OrientationCustom(Orientation.ORIENTATION_0))

                ChooseConstraintType.ORIENTATION_90 ->
                    _returnResult.emit(Constraint.OrientationCustom(Orientation.ORIENTATION_90))

                ChooseConstraintType.ORIENTATION_180 ->
                    _returnResult.emit(Constraint.OrientationCustom(Orientation.ORIENTATION_180))

                ChooseConstraintType.ORIENTATION_270 ->
                    _returnResult.emit(Constraint.OrientationCustom(Orientation.ORIENTATION_270))

                ChooseConstraintType.FLASHLIGHT_ON -> {
                    val lens = chooseFlashlightLens() ?: return@launch
                    _returnResult.emit(Constraint.FlashlightOn(lens))
                }

                ChooseConstraintType.FLASHLIGHT_OFF -> {
                    val lens = chooseFlashlightLens() ?: return@launch
                    _returnResult.emit(Constraint.FlashlightOff(lens))
                }

                ChooseConstraintType.WIFI_ON -> _returnResult.emit(Constraint.WifiOn)
                ChooseConstraintType.WIFI_OFF -> _returnResult.emit(Constraint.WifiOff)

                ChooseConstraintType.WIFI_CONNECTED,
                ChooseConstraintType.WIFI_DISCONNECTED -> onSelectWifiConnectedConstraint(
                    constraintType
                )

                ChooseConstraintType.IME_CHOSEN,
                ChooseConstraintType.IME_NOT_CHOSEN -> onSelectImeChosenConstraint(constraintType)

                ChooseConstraintType.DEVICE_IS_LOCKED ->
                    _returnResult.emit(Constraint.DeviceIsLocked)

                ChooseConstraintType.DEVICE_IS_UNLOCKED ->
                    _returnResult.emit(Constraint.DeviceIsUnlocked)

                ChooseConstraintType.IN_PHONE_CALL ->
                    _returnResult.emit(Constraint.InPhoneCall)

                ChooseConstraintType.NOT_IN_PHONE_CALL ->
                    _returnResult.emit(Constraint.NotInPhoneCall)

                ChooseConstraintType.PHONE_RINGING ->
                    _returnResult.emit(Constraint.PhoneRinging)

                ChooseConstraintType.CHARGING ->
                    _returnResult.emit(Constraint.Charging)

                ChooseConstraintType.DISCHARGING ->
                    _returnResult.emit(Constraint.Discharging)
            }
        }
    }

    private suspend fun chooseFlashlightLens(): CameraLens? {
        val items = listOf(
            CameraLens.FRONT to getString(R.string.lens_front),
            CameraLens.BACK to getString(R.string.lens_back)
        )

        val dialog = PopupUi.SingleChoice(title = getString(R.string.dialog_title_choose_camera_flash), items)

        val cameraLens = showPopup("choose_flashlight_lens", dialog)

        return cameraLens
    }

    private fun buildListItems(): List<DefaultSimpleListItem> = sequence {
        ALL_CONSTRAINTS_ORDERED.forEach { type ->
            if (!supportedConstraints.value.contains(type)) return@forEach

            val title: String = when (type) {
                ChooseConstraintType.APP_IN_FOREGROUND -> getString(R.string.constraint_choose_app_foreground)
                ChooseConstraintType.APP_NOT_IN_FOREGROUND -> getString(R.string.constraint_choose_app_not_foreground)
                ChooseConstraintType.APP_PLAYING_MEDIA -> getString(R.string.constraint_choose_app_playing_media)
                ChooseConstraintType.APP_NOT_PLAYING_MEDIA -> getString(R.string.constraint_choose_app_not_playing_media)
                ChooseConstraintType.MEDIA_NOT_PLAYING -> getString(R.string.constraint_choose_media_not_playing)
                ChooseConstraintType.MEDIA_PLAYING -> getString(R.string.constraint_choose_media_playing)
                ChooseConstraintType.BT_DEVICE_CONNECTED -> getString(R.string.constraint_choose_bluetooth_device_connected)
                ChooseConstraintType.BT_DEVICE_DISCONNECTED -> getString(R.string.constraint_choose_bluetooth_device_disconnected)
                ChooseConstraintType.SCREEN_ON -> getString(R.string.constraint_choose_screen_on_description)
                ChooseConstraintType.SCREEN_OFF -> getString(R.string.constraint_choose_screen_off_description)
                ChooseConstraintType.ORIENTATION_PORTRAIT -> getString(R.string.constraint_choose_orientation_portrait)
                ChooseConstraintType.ORIENTATION_LANDSCAPE -> getString(R.string.constraint_choose_orientation_landscape)
                ChooseConstraintType.ORIENTATION_0 -> getString(R.string.constraint_choose_orientation_0)
                ChooseConstraintType.ORIENTATION_90 -> getString(R.string.constraint_choose_orientation_90)
                ChooseConstraintType.ORIENTATION_180 -> getString(R.string.constraint_choose_orientation_180)
                ChooseConstraintType.ORIENTATION_270 -> getString(R.string.constraint_choose_orientation_270)
                ChooseConstraintType.FLASHLIGHT_ON -> getString(R.string.constraint_flashlight_on)
                ChooseConstraintType.FLASHLIGHT_OFF -> getString(R.string.constraint_flashlight_off)
                ChooseConstraintType.WIFI_ON -> getString(R.string.constraint_wifi_on)
                ChooseConstraintType.WIFI_OFF -> getString(R.string.constraint_wifi_off)
                ChooseConstraintType.WIFI_CONNECTED -> getString(R.string.constraint_wifi_connected)
                ChooseConstraintType.WIFI_DISCONNECTED -> getString(R.string.constraint_wifi_disconnected)
                ChooseConstraintType.IME_CHOSEN -> getString(R.string.constraint_ime_chosen)
                ChooseConstraintType.IME_NOT_CHOSEN -> getString(R.string.constraint_ime_not_chosen)
                ChooseConstraintType.DEVICE_IS_LOCKED -> getString(R.string.constraint_device_is_locked)
                ChooseConstraintType.DEVICE_IS_UNLOCKED -> getString(R.string.constraint_device_is_unlocked)
                ChooseConstraintType.IN_PHONE_CALL -> getString(R.string.constraint_in_phone_call)
                ChooseConstraintType.NOT_IN_PHONE_CALL -> getString(R.string.constraint_not_in_phone_call)
                ChooseConstraintType.PHONE_RINGING -> getString(R.string.constraint_phone_ringing)
                ChooseConstraintType.CHARGING -> getString(R.string.constraint_charging)
                ChooseConstraintType.DISCHARGING -> getString(R.string.constraint_discharging)
            }

            val error = useCase.isSupported(type)

            val listItem = DefaultSimpleListItem(
                id = type.toString(),
                title = title,
                isEnabled = error == null,
                subtitle = error?.getFullMessage(this@ChooseConstraintViewModel),
                subtitleTint = TintType.Error,
                icon = null
            )

            yield(listItem)
        }
    }.toList()

    private suspend fun onSelectWifiConnectedConstraint(type: ChooseConstraintType) {
        val knownSSIDs = useCase.getKnownWiFiSSIDs()

        val chosenSSID: String?

        if (knownSSIDs == null) {
            val savedWifiSSIDs = useCase.getSavedWifiSSIDs().first()

            val dialog = PopupUi.Text(
                hint = getString(R.string.hint_wifi_ssid),
                allowEmpty = true,
                message = getString(R.string.constraint_wifi_message_cant_list_networks),
                autoCompleteEntries = savedWifiSSIDs
            )

            val ssidText = showPopup("type_ssid", dialog) ?: return

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
            val dialog = PopupUi.SingleChoice(title = getString(R.string.dialog_title_choose_wifi_network), items)

            val chosenItem = showPopup("choose_ssid", dialog) ?: return

            if (chosenItem == anySSIDItem.first) {
                chosenSSID = null
            } else {
                chosenSSID = items.single { it.first == chosenItem }.second
            }
        }

        when (type) {
            ChooseConstraintType.WIFI_CONNECTED ->
                _returnResult.emit(Constraint.WifiConnected(chosenSSID))

            ChooseConstraintType.WIFI_DISCONNECTED ->
                _returnResult.emit(Constraint.WifiDisconnected(chosenSSID))

            else -> {}
        }
    }

    private suspend fun onSelectImeChosenConstraint(type: ChooseConstraintType) {
        val inputMethods = useCase.getEnabledInputMethods()
        val items = inputMethods.map { it.id to it.label }
        val dialog = PopupUi.SingleChoice(title = getString(R.string.dialog_title_choose_keyboard), items)

        val result = showPopup("choose_input_method", dialog) ?: return

        val imeInfo = inputMethods.single { it.id == result }

        when (type) {
            ChooseConstraintType.IME_CHOSEN ->
                _returnResult.emit(Constraint.ImeChosen(imeInfo.id, imeInfo.label))

            ChooseConstraintType.IME_NOT_CHOSEN ->
                _returnResult.emit(Constraint.ImeNotChosen(imeInfo.id, imeInfo.label))

            else -> {}
        }
    }

    private suspend fun onSelectScreenOnConstraint() {
        val response = showPopup(
            "screen_on_constraint_limitation",
            PopupUi.Ok(getString(R.string.dialog_message_screen_constraints_limitation))
        )

        response ?: return

        _returnResult.emit(Constraint.ScreenOn)
    }

    private suspend fun onSelectScreenOffConstraint() {
        val response = showPopup(
            "screen_on_constraint_limitation",
            PopupUi.Ok(getString(R.string.dialog_message_screen_constraints_limitation))
        )

        response ?: return

        _returnResult.emit(Constraint.ScreenOff)
    }

    private suspend fun onSelectBluetoothConstraint(type: ChooseConstraintType) {
        val response = showPopup(
            "bluetooth_device_constraint_limitation",
            PopupUi.Ok(getString(R.string.dialog_message_bt_constraint_limitation))
        )

        response ?: return

        val device = navigate(
            "choose_bluetooth_device_for_constraint",
            NavDestination.ChooseBluetoothDevice
        ) ?: return

        val constraint = when (type) {
            ChooseConstraintType.BT_DEVICE_CONNECTED -> Constraint.BtDeviceConnected(
                device.address,
                device.name
            )
            ChooseConstraintType.BT_DEVICE_DISCONNECTED -> Constraint.BtDeviceDisconnected(
                device.address,
                device.name
            )
            else -> throw IllegalArgumentException("Don't know how to create $type constraint after choosing app")
        }

        _returnResult.emit(constraint)
    }

    private suspend fun onSelectAppConstraint(type: ChooseConstraintType) {
        val packageName =
            navigate("choose_package_for_constraint", NavDestination.ChooseApp(allowHiddenApps = true))
                ?: return

        val constraint = when (type) {
            ChooseConstraintType.APP_IN_FOREGROUND -> Constraint.AppInForeground(
                packageName
            )
            ChooseConstraintType.APP_NOT_IN_FOREGROUND -> Constraint.AppNotInForeground(
                packageName
            )
            ChooseConstraintType.APP_PLAYING_MEDIA -> Constraint.AppPlayingMedia(
                packageName
            )
            ChooseConstraintType.APP_NOT_PLAYING_MEDIA -> Constraint.AppNotPlayingMedia(
                packageName
            )

            else -> throw IllegalArgumentException("Don't know how to create $type constraint after choosing app")
        }

        _returnResult.emit(constraint)
    }
}