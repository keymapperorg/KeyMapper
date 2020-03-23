package io.github.sds100.keymapper.data.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.ChooseConstraintListItemModel
import io.github.sds100.keymapper.data.model.Constraint
import splitties.init.appCtx
import splitties.resources.str

/**
 * Created by sds100 on 21/03/2020.
 */

class ChooseConstraintListViewModel : ViewModel() {

    private val mConstraintList = listOf(
        ChooseConstraintListItemModel(
            Constraint.APP_FOREGROUND,
            Constraint.CATEGORY_APP,
            appCtx.str(R.string.constraint_choose_app_foreground)
        ),
        ChooseConstraintListItemModel(
            Constraint.BT_DEVICE_CONNECTED,
            Constraint.CATEGORY_BLUETOOTH,
            appCtx.str(R.string.constraint_choose_bluetooth_device_connected)
        ),
        ChooseConstraintListItemModel(
            Constraint.BT_DEVICE_DISCONNECTED,
            Constraint.CATEGORY_BLUETOOTH,
            appCtx.str(R.string.constraint_choose_bluetooth_device_disconnected)
        )
    )

    val constraintsSortedByCategory = sequence {
        for ((id, label) in Constraint.CATEGORY_LABEL_MAP) {
            val constraints = mConstraintList.filter { it.categoryId == id }

            yield(Pair(appCtx.str(label), constraints))
        }
    }.toList()

    @Suppress("UNCHECKED_CAST")
    class Factory : ViewModelProvider.NewInstanceFactory() {

        override fun <T : ViewModel?> create(modelClass: Class<T>): T {
            return ChooseConstraintListViewModel() as T
        }
    }
}