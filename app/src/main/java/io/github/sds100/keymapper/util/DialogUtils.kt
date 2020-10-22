package io.github.sds100.keymapper.util

import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.data.model.SeekBarListItemModel
import io.github.sds100.keymapper.databinding.DialogEdittextNumberBinding
import io.github.sds100.keymapper.databinding.DialogEdittextStringBinding
import io.github.sds100.keymapper.databinding.DialogSeekbarListBinding
import io.github.sds100.keymapper.util.result.*
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.okButton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by sds100 on 30/03/2020.
 */

suspend fun FragmentActivity.editTextStringAlertDialog(hint: String, allowEmpty: Boolean = false) = suspendCoroutine<String> {
    alertDialog {
        DialogEdittextStringBinding.inflate(layoutInflater).apply {
            val text = MutableLiveData("")

            setHint(hint)
            setText(text)
            setAllowEmpty(allowEmpty)

            setView(this.root)

            okButton { _ ->
                it.resume(text.value!!)
            }

            cancelButton()

            show().apply {
                text.observe(this@editTextStringAlertDialog, {
                    getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                        if (allowEmpty) {
                            true
                        } else {
                            !it.isNullOrBlank()
                        }
                })
            }
        }
    }
}

suspend fun FragmentActivity.editTextNumberAlertDialog(
    hint: String,
    min: Int? = null,
    max: Int? = null
) = suspendCoroutine<Int> {

    fun isValid(text: String?): Result<Int> {
        if (text.isNullOrBlank()) {
            return InvalidNumber()
        }

        return try {
            val num = text.toInt()

            min?.let {
                if (num < min) {
                    return NumberTooSmall(min)
                }
            }

            max?.let {
                if (num > max) {
                    return NumberTooBig(max)
                }
            }

            Success(num)
        } catch (e: NumberFormatException) {
            InvalidNumber()
        }
    }

    alertDialog {
        DialogEdittextNumberBinding.inflate(layoutInflater).apply {
            val text = MutableLiveData("")

            setHint(hint)
            setText(text)

            setView(this.root)

            okButton { _ ->
                isValid(text.value).onSuccess { num ->
                    it.resume(num)
                }
            }

            cancelButton()

            show().apply {
                text.observe(this@editTextNumberAlertDialog, {
                    val result = isValid(it)

                    getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = result.isSuccess

                    textInputLayout.error = result.failureOrNull()?.getFullMessage(context)
                })
            }
        }
    }
}

suspend fun FragmentActivity.seekBarAlertDialog(seekBarListItemModel: SeekBarListItemModel) = suspendCoroutine<Int> {
    alertDialog {
        DialogSeekbarListBinding.inflate(layoutInflater).apply {

            var result = seekBarListItemModel.initialValue

            model = seekBarListItemModel

            onChangeListener = object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    result = seekBarListItemModel.calculateValue(progress)

                    textViewValue.text = result.toString()
                }

                override fun onStartTrackingTouch(seekBar: SeekBar?) {}

                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }

            okButton { _ ->
                it.resume(result)
            }


            setView(this.root)
        }

        cancelButton()

        show()
    }
}