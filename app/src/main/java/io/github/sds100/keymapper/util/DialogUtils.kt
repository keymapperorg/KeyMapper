package io.github.sds100.keymapper.util

import android.content.Context
import android.view.LayoutInflater
import android.widget.SeekBar
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.LifecycleOwner
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

suspend fun Context.editTextStringAlertDialog(lifecycleOwner: LifecycleOwner,
                                              hint: String,
                                              allowEmpty: Boolean = false) = suspendCoroutine<String> {
    alertDialog {
        val inflater = LayoutInflater.from(this@editTextStringAlertDialog)

        DialogEdittextStringBinding.inflate(inflater).apply {
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
                text.observe(lifecycleOwner, {
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

suspend fun Context.editTextNumberAlertDialog(
    lifecycleOwner: LifecycleOwner,
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
        val inflater = LayoutInflater.from(this@editTextNumberAlertDialog)
        DialogEdittextNumberBinding.inflate(inflater).apply {
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
                text.observe(lifecycleOwner, {
                    val result = isValid(it)

                    getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = result.isSuccess

                    textInputLayout.error = result.failureOrNull()?.getFullMessage(context)
                })
            }
        }
    }
}

suspend fun Context.seekBarAlertDialog(
    lifecycleOwner: LifecycleOwner,
    seekBarListItemModel: SeekBarListItemModel) = suspendCoroutine<Int> {
    alertDialog {
        val inflater = LayoutInflater.from(this@seekBarAlertDialog)
        DialogSeekbarListBinding.inflate(inflater).apply {

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