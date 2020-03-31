package io.github.sds100.keymapper.util

import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import io.github.sds100.keymapper.databinding.DialogEdittextBinding
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.okButton
import splitties.experimental.ExperimentalSplittiesApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by sds100 on 30/03/2020.
 */

@ExperimentalSplittiesApi
suspend fun FragmentActivity.editTextAlertDialog(hint: String) = suspendCoroutine<String> {

    alertDialog {
        DialogEdittextBinding.inflate(layoutInflater).apply {
            val text = MutableLiveData("")

            setHint(hint)
            setText(text)

            setView(this.root)

            okButton { _ ->
                it.resume(text.value!!)
            }

            cancelButton()

            show().apply {
                text.observe(this@editTextAlertDialog) {
                    getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = !it.isNullOrBlank()
                }
            }
        }
    }
}