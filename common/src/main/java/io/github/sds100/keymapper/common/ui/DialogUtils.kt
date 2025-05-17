package io.github.sds100.keymapper.common.ui

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputLayout
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.common.util.result.Error
import io.github.sds100.keymapper.common.util.result.Result
import io.github.sds100.keymapper.common.util.result.Success
import io.github.sds100.keymapper.common.util.result.errorOrNull
import io.github.sds100.keymapper.common.util.result.isSuccess
import io.github.sds100.keymapper.common.util.result.onSuccess
import io.github.sds100.keymapper.databinding.DialogEdittextNumberBinding
import io.github.sds100.keymapper.databinding.DialogEdittextStringBinding
import io.github.sds100.keymapper.base.util.getFullMessage
import io.github.sds100.keymapper.common.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.common.util.resumeIfNotCompleted
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.alertdialog.appcompat.message
import splitties.alertdialog.appcompat.negativeButton
import splitties.alertdialog.appcompat.okButton
import splitties.alertdialog.appcompat.title
import splitties.alertdialog.material.materialAlertDialog
import kotlin.coroutines.resume

suspend fun Context.materialAlertDialog(
    lifecycleOwner: LifecycleOwner,
    model: PopupUi.Dialog,
) = suspendCancellableCoroutine<DialogResponse?> { continuation ->

    materialAlertDialog {
        title = model.title
        setMessage(model.message)

        setPositiveButton(model.positiveButtonText) { _, _ ->
            continuation.resumeIfNotCompleted(DialogResponse.POSITIVE)
        }

        setNeutralButton(model.neutralButtonText) { _, _ ->
            continuation.resumeIfNotCompleted(DialogResponse.NEUTRAL)
        }

        setNegativeButton(model.negativeButtonText) { _, _ ->
            continuation.resumeIfNotCompleted(DialogResponse.NEGATIVE)
        }

        show().apply {
            resumeNullOnDismiss(continuation)
            dismissOnDestroy(lifecycleOwner)

            continuation.invokeOnCancellation {
                dismiss()
            }
        }
    }
}

suspend fun Context.materialAlertDialogCustomView(
    lifecycleOwner: LifecycleOwner,
    title: CharSequence,
    message: CharSequence,
    positiveButtonText: CharSequence? = null,
    neutralButtonText: CharSequence? = null,
    negativeButtonText: CharSequence? = null,
    view: View,
) = suspendCancellableCoroutine<DialogResponse?> { continuation ->

    materialAlertDialog {
        setTitle(title)
        setMessage(message)

        setPositiveButton(positiveButtonText) { _, _ ->
            continuation.resumeIfNotCompleted(DialogResponse.POSITIVE)
        }

        setNeutralButton(neutralButtonText) { _, _ ->
            continuation.resumeIfNotCompleted(DialogResponse.NEUTRAL)
        }

        setNegativeButton(negativeButtonText) { _, _ ->
            continuation.resumeIfNotCompleted(DialogResponse.NEGATIVE)
        }

        setView(view)

        show().apply {
            resumeNullOnDismiss(continuation)
            dismissOnDestroy(lifecycleOwner)

            continuation.invokeOnCancellation {
                dismiss()
            }
        }
    }
}

suspend fun Context.multiChoiceDialog(
    lifecycleOwner: LifecycleOwner,
    items: List<MultiChoiceItem<*>>,
) = suspendCancellableCoroutine<List<*>?> { continuation ->
    materialAlertDialog {
        val checkedItems = items
            .map { it.isChecked }
            .toBooleanArray()

        setMultiChoiceItems(
            items.map { it.label }.toTypedArray(),
            checkedItems,
        ) { _, which, checked ->
            checkedItems[which] = checked
        }

        negativeButton(R.string.neg_cancel) { it.cancel() }

        okButton {
            val checkedItemIds = sequence {
                checkedItems.forEachIndexed { index, checked ->
                    if (checked) {
                        yield(items[index].id)
                    }
                }
            }.toList()

            continuation.resumeIfNotCompleted(checkedItemIds)
        }

        show().apply {
            resumeNullOnDismiss(continuation)
            dismissOnDestroy(lifecycleOwner)
        }
    }
}

suspend fun <ID> Context.singleChoiceDialog(
    lifecycleOwner: LifecycleOwner,
    items: List<Pair<ID, String>>,
) = suspendCancellableCoroutine<ID?> { continuation ->
    materialAlertDialog {
        // message isn't supported
        setItems(
            items.map { it.second }.toTypedArray(),
        ) { _, position ->
            continuation.resumeIfNotCompleted(items[position].first)
        }

        show().apply {
            resumeNullOnDismiss(continuation)
            dismissOnDestroy(lifecycleOwner)
        }
    }
}

suspend fun Context.editTextStringAlertDialog(
    lifecycleOwner: LifecycleOwner,
    hint: String,
    allowEmpty: Boolean = false,
    initialText: String = "",
    inputType: Int? = null,
    message: CharSequence? = null,
    autoCompleteEntries: List<String> = emptyList(),
) = suspendCancellableCoroutine<String?> { continuation ->

    val text = MutableStateFlow(initialText)

    val alertDialog = materialAlertDialog {
        val inflater = LayoutInflater.from(this@editTextStringAlertDialog)

        DialogEdittextStringBinding.inflate(inflater).apply {
            setHint(hint)
            setText(text)
            setAllowEmpty(allowEmpty)

            if (autoCompleteEntries.isEmpty()) {
                textInputLayout.endIconMode = TextInputLayout.END_ICON_NONE
            } else {
                textInputLayout.endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
            }

            if (inputType != null) {
                autoCompleteTextView.inputType = inputType
            }

            val autoCompleteAdapter = ArrayAdapter(
                this@editTextStringAlertDialog,
                R.layout.dropdown_menu_popup_item,
                autoCompleteEntries,
            )

            autoCompleteTextView.setAdapter(autoCompleteAdapter)

            setView(this.root)
        }

        if (message != null) {
            this.message = message
        }

        okButton {
            continuation.resumeIfNotCompleted(text.value)
        }

        negativeButton(R.string.neg_cancel) {
            it.cancel()
        }
    }

    // this prevents window leak
    alertDialog.resumeNullOnDismiss(continuation)
    alertDialog.dismissOnDestroy(lifecycleOwner)

    alertDialog.show()

    lifecycleOwner.lifecycleScope.launchWhenResumed {
        text.collectLatest {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                if (allowEmpty) {
                    true
                } else {
                    it.isNotBlank()
                }
        }
    }
}

suspend fun Context.editTextNumberAlertDialog(
    lifecycleOwner: LifecycleOwner,
    hint: String,
    min: Int? = null,
    max: Int? = null,
) = suspendCancellableCoroutine<Int?> { continuation ->

    fun isValid(text: String?): Result<Int> {
        if (text.isNullOrBlank()) {
            return Error.InvalidNumber
        }

        return try {
            val num = text.toInt()

            min?.let {
                if (num < min) {
                    return Error.NumberTooSmall(min)
                }
            }

            max?.let {
                if (num > max) {
                    return Error.NumberTooBig(max)
                }
            }

            Success(num)
        } catch (e: NumberFormatException) {
            Error.InvalidNumber
        }
    }

    val resourceProvider = ServiceLocator.resourceProvider(this)
    val text = MutableStateFlow("")

    val inflater = LayoutInflater.from(this@editTextNumberAlertDialog)
    val binding = DialogEdittextNumberBinding.inflate(inflater).apply {
        setHint(hint)
        setText(text)
    }

    val alertDialog = materialAlertDialog {
        okButton {
            isValid(text.value).onSuccess { num ->
                continuation.resume(num)
            }
        }

        negativeButton(R.string.neg_cancel) { it.cancel() }

        setView(binding.root)
    }

    alertDialog.show()
    alertDialog.resumeNullOnDismiss(continuation)
    alertDialog.dismissOnDestroy(lifecycleOwner)

    lifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
        text.map { isValid(it) }
            .collectLatest { isValid ->
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                    isValid.isSuccess

                binding.textInputLayout.error =
                    isValid.errorOrNull()?.getFullMessage(resourceProvider)
            }
    }
}

suspend fun Context.okDialog(
    lifecycleOwner: LifecycleOwner,
    message: String,
    title: String? = null,
) = suspendCancellableCoroutine<Unit?> { continuation ->

    val alertDialog = materialAlertDialog {
        setTitle(title)
        setMessage(message)

        okButton {
            continuation.resumeIfNotCompleted(Unit)
        }
    }

    alertDialog.show()

    // this prevents window leak
    alertDialog.resumeNullOnDismiss(continuation)
    alertDialog.dismissOnDestroy(lifecycleOwner)
}

fun <T> Dialog.resumeNullOnDismiss(continuation: CancellableContinuation<T?>) {
    setOnDismissListener {
        if (!continuation.isCompleted) {
            continuation.resume(null)
        }
    }
}

fun Dialog.dismissOnDestroy(lifecycleOwner: LifecycleOwner) {
    lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun onDestroy() {
            this@dismissOnDestroy.dismiss()
            lifecycleOwner.lifecycle.removeObserver(this)
        }
    })
}
