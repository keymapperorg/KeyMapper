package io.github.sds100.keymapper.view

import android.content.Context
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog

/**
 * Created by sds100 on 26/01/2019.
 */

/**
 * @param items - first = The label for the item, second = the item, third = whether the device is checked
 *
 */
fun <T> Context.multiChoiceDialog(
        @StringRes titleRes: Int,
        items: MutableList<Triple<String, T, Boolean>>,
        onPosClick: (newItems: List<Triple<String, T, Boolean>>) -> Unit
) {
    val builder = AlertDialog.Builder(this)
    builder.apply {
        setTitle(titleRes)

        val itemLabels = items.map { it.first }.toTypedArray()
        val checkedItems = items.map { it.third }.toBooleanArray()

        setMultiChoiceItems(itemLabels, checkedItems) { _, which, isChecked ->
            items[which] = Triple(items[which].first, items[which].second, isChecked)
        }

        setPositiveButton(android.R.string.ok) { dialog, _ ->
            onPosClick(items)
            dialog.dismiss()
        }

        setNegativeButton(android.R.string.cancel) { dialog, _ ->
            dialog.dismiss()
        }
    }.show()
}