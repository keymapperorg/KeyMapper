package io.github.sds100.keymapper.constraints

import androidx.lifecycle.*
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.system.display.Orientation
import io.github.sds100.keymapper.util.ui.ResourceProvider
import io.github.sds100.keymapper.ui.*
import io.github.sds100.keymapper.util.ui.PopupUi
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Created by sds100 on 21/03/2020.
 */

class ChooseConstraintViewModel(
    resourceProvider: ResourceProvider
) : ViewModel(), ResourceProvider by resourceProvider,
    PopupViewModel by PopupViewModelImpl() {

    companion object {
        private val ALL_CONSTRAINTS_ORDERED: Array<ChooseConstraintType> = arrayOf(
            ChooseConstraintType.APP_IN_FOREGROUND,
            ChooseConstraintType.APP_NOT_IN_FOREGROUND,
            ChooseConstraintType.APP_PLAYING_MEDIA,

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
        )
    }

    private val _state =
        MutableStateFlow<ListUiState<ChooseConstraintListItem>>(ListUiState.Loading)
    val state = _state.asStateFlow()

    private val _returnResult = MutableSharedFlow<Constraint>()
    val returnResult = _returnResult.asSharedFlow()

    private val _chooseApp = MutableSharedFlow<Unit>()
    val chooseApp = _chooseApp.asSharedFlow()

    private val _chooseBluetoothDevice = MutableSharedFlow<Unit>()
    val chooseBluetoothDevice = _chooseBluetoothDevice.asSharedFlow()

    private var supportedConstraints = MutableStateFlow<Array<ChooseConstraintType>>(emptyArray())
    private var chosenConstraintType: ChooseConstraintType? = null

    init {
        viewModelScope.launch {
            supportedConstraints.collectLatest {
                _state.value = withContext(Dispatchers.Default) {
                    buildListItems().createListState()
                }
            }
        }
    }

    fun setSupportedConstraints(supportedConstraints: Array<ChooseConstraintType>) {
        this.supportedConstraints.value = supportedConstraints
    }

    fun onChooseApp(packageName: String) {
        chosenConstraintType ?: return
        viewModelScope.launch {
            val constraint = when (chosenConstraintType) {
                ChooseConstraintType.APP_IN_FOREGROUND -> Constraint.AppInForeground(packageName)
                ChooseConstraintType.APP_NOT_IN_FOREGROUND -> Constraint.AppNotInForeground(packageName)
                ChooseConstraintType.APP_PLAYING_MEDIA -> Constraint.AppPlayingMedia(packageName)
                else -> throw Exception("Don't know how to create $chosenConstraintType constraint after choosing app")
            }

            _returnResult.emit(constraint)
        }

        chosenConstraintType = null
    }

    fun onChooseBluetoothDevice(address: String, name: String) {
        chosenConstraintType ?: return

        viewModelScope.launch {
            val constraint = when (chosenConstraintType) {
                ChooseConstraintType.BT_DEVICE_CONNECTED -> Constraint.BtDeviceConnected(
                    address,
                    name
                )
                ChooseConstraintType.BT_DEVICE_DISCONNECTED -> Constraint.BtDeviceDisconnected(
                    address,
                    name
                )
                else -> throw Exception("Don't know how to create $chosenConstraintType constraint after choosing app")
            }

            _returnResult.emit(constraint)
        }

        chosenConstraintType = null
    }

    fun chooseConstraint(constraintType: ChooseConstraintType) {
        viewModelScope.launch {
            when (constraintType) {
                ChooseConstraintType.APP_IN_FOREGROUND,
                ChooseConstraintType.APP_NOT_IN_FOREGROUND,
                ChooseConstraintType.APP_PLAYING_MEDIA -> {
                    chosenConstraintType = constraintType
                    _chooseApp.emit(Unit)
                }

                ChooseConstraintType.BT_DEVICE_CONNECTED,
                ChooseConstraintType.BT_DEVICE_DISCONNECTED -> {
                    val response = showPopup(
                        "bluetooth_device_constraint_limitation",
                        PopupUi.Ok(getString(R.string.dialog_message_bt_constraint_limitation))
                    )

                    response ?: return@launch
                    chosenConstraintType = constraintType

                    _chooseBluetoothDevice.emit(Unit)
                }

                ChooseConstraintType.SCREEN_ON -> {
                    val response = showPopup(
                        "screen_on_constraint_limitation",
                        PopupUi.Ok(getString(R.string.dialog_message_screen_constraints_limitation))
                    )

                    response ?: return@launch

                    _returnResult.emit(Constraint.ScreenOn)
                }

                ChooseConstraintType.SCREEN_OFF -> {
                    val response = showPopup(
                        "screen_off_constraint_limitation",
                        PopupUi.Ok(getString(R.string.dialog_message_screen_constraints_limitation))
                    )

                    response ?: return@launch

                    _returnResult.emit(Constraint.ScreenOff)
                }

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
            }
        }
    }

    private fun buildListItems(): List<ChooseConstraintListItem> = sequence {
        ALL_CONSTRAINTS_ORDERED.forEach { type ->
            if (!supportedConstraints.value.contains(type)) return@forEach

            val listItem = when (type) {
                ChooseConstraintType.APP_IN_FOREGROUND ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_app_foreground)
                    )

                ChooseConstraintType.APP_NOT_IN_FOREGROUND ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_app_not_foreground)
                    )

                ChooseConstraintType.APP_PLAYING_MEDIA ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_app_playing_media)
                    )

                ChooseConstraintType.BT_DEVICE_CONNECTED ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_bluetooth_device_connected)
                    )

                ChooseConstraintType.BT_DEVICE_DISCONNECTED ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_bluetooth_device_disconnected)
                    )

                ChooseConstraintType.SCREEN_ON ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_screen_on_description)
                    )

                ChooseConstraintType.SCREEN_OFF ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_screen_off_description)
                    )

                ChooseConstraintType.ORIENTATION_PORTRAIT ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_orientation_portrait)
                    )

                ChooseConstraintType.ORIENTATION_LANDSCAPE ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_orientation_landscape)
                    )

                ChooseConstraintType.ORIENTATION_0 ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_orientation_0)
                    )

                ChooseConstraintType.ORIENTATION_90 ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_orientation_90)
                    )

                ChooseConstraintType.ORIENTATION_180 ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_orientation_180)
                    )

                ChooseConstraintType.ORIENTATION_270 ->
                    ChooseConstraintListItem(
                        type,
                        getString(R.string.constraint_choose_orientation_270)
                    )
            }

            yield(listItem)
        }
    }.toList()

    @Suppress("UNCHECKED_CAST")
    class Factory(
        private val resourceProvider: ResourceProvider
    ) : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChooseConstraintViewModel(resourceProvider) as T
        }
    }
}