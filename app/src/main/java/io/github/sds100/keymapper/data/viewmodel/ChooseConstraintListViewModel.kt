package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.ChooseConstraintListItemModel
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.ConstraintType
import io.github.sds100.keymapper.data.model.NotifyUserModel
import io.github.sds100.keymapper.util.Event

/**
 * Created by sds100 on 21/03/2020.
 */

class ChooseConstraintListViewModel : ViewModel() {

    private val mConstraintList = listOf(
        ChooseConstraintListItemModel(
            Constraint.APP_FOREGROUND,
            Constraint.CATEGORY_APP,
            R.string.constraint_choose_app_foreground),

        ChooseConstraintListItemModel(
            Constraint.APP_NOT_FOREGROUND,
            Constraint.CATEGORY_APP,
            R.string.constraint_choose_app_not_foreground
        ),

        ChooseConstraintListItemModel(
            Constraint.BT_DEVICE_CONNECTED,
            Constraint.CATEGORY_BLUETOOTH,
            R.string.constraint_choose_bluetooth_device_connected
        ),
        ChooseConstraintListItemModel(
            Constraint.BT_DEVICE_DISCONNECTED,
            Constraint.CATEGORY_BLUETOOTH,
            R.string.constraint_choose_bluetooth_device_disconnected
        )
    )

    val constraintsSortedByCategory = sequence {
        for ((id, label) in Constraint.CATEGORY_LABEL_MAP) {
            val constraints = mConstraintList.filter { it.categoryId == id }

            yield(label to constraints)
        }
    }.toMap()

    val choosePackageEvent = MutableLiveData<Event<Unit>>()
    val chooseBluetoothDeviceEvent = MutableLiveData<Event<Unit>>()
    val selectModelEvent = MutableLiveData<Event<Constraint>>()
    val notifyUserEvent = MutableLiveData<Event<NotifyUserModel>>()

    private var mChosenConstraintType: String? = null

    fun chooseConstraint(@ConstraintType constraintType: String) {
        mChosenConstraintType = constraintType

        when (constraintType) {
            Constraint.APP_FOREGROUND, Constraint.APP_NOT_FOREGROUND -> choosePackageEvent.value = Event(Unit)
            Constraint.BT_DEVICE_CONNECTED, Constraint.BT_DEVICE_DISCONNECTED -> {
                notifyUserEvent.value = Event(NotifyUserModel(R.string.dialog_message_bt_constraint_limitation) {
                    chooseBluetoothDeviceEvent.value = Event(Unit)
                })
            }
        }
    }

    fun packageChosen(packageName: String) {
        selectModelEvent.value = Event(Constraint.appConstraint(mChosenConstraintType!!, packageName))
        mChosenConstraintType = null
    }

    fun bluetoothDeviceChosen(address: String, name: String) {
        selectModelEvent.value = Event(Constraint.btConstraint(mChosenConstraintType!!, address, name))
        mChosenConstraintType = null
    }

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChooseConstraintListViewModel() as T
        }
    }
}