package io.github.sds100.keymapper.view

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import io.github.sds100.keymapper.R
import org.jetbrains.anko.toast

/**
 * Created by sds100 on 30/09/2018.
 */

/**
 * Show a dialog which asks the user to input a shortcut title.
 *
 * @param onSuccess what to do when the user successfully enters some text.
 * @param allowEmpty whether the EditText can be blank when the user clicks on the positive button.
 */
fun Context.editTextDialog(
        @StringRes titleRes: Int,
        @StringRes posButtonRes: Int = R.string.pos_done,
        allowEmpty: Boolean = false,
        onSuccess: (text: String) -> Unit) {
    val builder = AlertDialog.Builder(this)

    builder.setTitle(titleRes)

    val editText = EditText(this)

    val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
    )

    editText.layoutParams = layoutParams
    editText.setSingleLine()

    builder.setView(editText)

    builder.setPositiveButton(posButtonRes) { dialog, _ ->
        if (allowEmpty || editText.text.isNotEmpty()) {
            onSuccess(editText.text.toString())
            dialog.dismiss()
        } else {
            toast(R.string.error_must_have_a_title)
        }
    }

    builder.setNegativeButton(R.string.neg_cancel) { dialog, _ -> dialog.cancel() }

    builder.show()
}