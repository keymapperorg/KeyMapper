package io.github.sds100.keymapper

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog

/**
 * Created by sds100 on 30/09/2018.
 */

object ShortcutTitleDialog {
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