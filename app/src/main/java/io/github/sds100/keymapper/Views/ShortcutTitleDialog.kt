package io.github.sds100.keymapper.Views

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog
import io.github.sds100.keymapper.R

/**
 * Created by sds100 on 30/09/2018.
 */

object ShortcutTitleDialog {
    /**
     * Show a dialog which asks the user to input a shortcut title.
     *
     * @param onCreateTitle what to do when the user successfully creates a title
     */
    fun show(ctx: Context, onCreateTitle: (title: String) -> Unit) {
        val builder = AlertDialog.Builder(ctx)

        builder.setTitle(R.string.dialog_title_create_shortcut_title)

        val editText = EditText(ctx)

        val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        )

        editText.layoutParams = layoutParams
        editText.setSingleLine()

        builder.setView(editText)

        builder.setPositiveButton(R.string.pos_done) { dialog, _ ->
            if (editText.text.isNotEmpty()) {
                onCreateTitle(editText.text.toString())
                dialog.dismiss()
            } else {
                Toast.makeText(ctx, R.string.error_must_have_a_title, LENGTH_SHORT).show()
            }
        }

        builder.setNegativeButton(R.string.neg_cancel) { dialog, _ -> dialog.cancel() }

        builder.show()
    }
}