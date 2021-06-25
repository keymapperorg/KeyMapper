package io.github.sds100.keymapper.util.ui

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.*
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.databinding.DialogChooseAppStoreBinding
import io.github.sds100.keymapper.databinding.DialogEdittextNumberBinding
import io.github.sds100.keymapper.databinding.DialogEdittextStringBinding
import io.github.sds100.keymapper.home.ChooseAppStoreModel
import io.github.sds100.keymapper.system.leanback.LeanbackUtils
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.alertdialog.appcompat.*
import splitties.alertdialog.material.materialAlertDialog
import kotlin.coroutines.resume

/**
 * Created by sds100 on 30/03/2020.
 */

suspend fun Context.materialAlertDialog(
    lifecycleOwner: LifecycleOwner,
    model: PopupUi.Dialog
) = suspendCancellableCoroutine<DialogResponse?> { continuation ->

    materialAlertDialog {
        title = model.title
        message = model.message

        setPositiveButton(model.positiveButtonText) { _, _ ->
            continuation.resume(DialogResponse.POSITIVE)
        }

        setNeutralButton(model.neutralButtonText) { _, _ ->
            continuation.resume(DialogResponse.NEUTRAL)
        }

        setNegativeButton(model.negativeButtonText) { _, _ ->
            continuation.resume(DialogResponse.NEGATIVE)
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

suspend fun <ID> Context.multiChoiceDialog(
    lifecycleOwner: LifecycleOwner,
    items: List<Pair<ID, String>>
) = suspendCancellableCoroutine<PopupUi.MultiChoiceResponse<ID>?> { continuation ->
    materialAlertDialog {
        val checkedItems = BooleanArray(items.size) { false }

        setMultiChoiceItems(
            items.map { it.second }.toTypedArray(),
            checkedItems
        ) { _, which, checked ->
            checkedItems[which] = checked
        }

        negativeButton(R.string.neg_cancel) { it.cancel() }

        okButton {
            val checkedItemIds = sequence {
                checkedItems.forEachIndexed { index, checked ->
                    if (checked) {
                        yield(items[index].first)
                    }
                }
            }.toList()

            continuation.resume(PopupUi.MultiChoiceResponse(checkedItemIds))
        }

        show().apply {
            resumeNullOnDismiss(continuation)
            dismissOnDestroy(lifecycleOwner)
        }
    }
}

suspend fun <ID> Context.singleChoiceDialog(
    lifecycleOwner: LifecycleOwner,
    items: List<Pair<ID, String>>
) = suspendCancellableCoroutine<PopupUi.SingleChoiceResponse<ID>?> { continuation ->
    materialAlertDialog {
        setItems(
            items.map { it.second }.toTypedArray(),
        ) { _, position ->
            continuation.resume(PopupUi.SingleChoiceResponse(items[position].first))
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
    initialText: String = ""
) = suspendCancellableCoroutine<PopupUi.TextResponse?> { continuation ->

    val text = MutableStateFlow(initialText)

    val alertDialog = materialAlertDialog {
        val inflater = LayoutInflater.from(this@editTextStringAlertDialog)

        DialogEdittextStringBinding.inflate(inflater).apply {
            setHint(hint)
            setText(text)
            setAllowEmpty(allowEmpty)

            setView(this.root)
        }

        okButton {
            continuation.resume(PopupUi.TextResponse(text.value))
        }

        negativeButton(R.string.neg_cancel) { it.cancel() }
    }

    alertDialog.show()

    lifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
        text.collectLatest {
            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                if (allowEmpty) {
                    true
                } else {
                    it.isNotBlank()
                }
        }
    }

    //this prevents window leak
    alertDialog.resumeNullOnDismiss(continuation)
    alertDialog.dismissOnDestroy(lifecycleOwner)
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

    materialAlertDialog {
        val inflater = LayoutInflater.from(this@editTextNumberAlertDialog)
        DialogEdittextNumberBinding.inflate(inflater).apply {

            setHint(hint)
            setText(text)

            setView(this.root)

            okButton {
                isValid(text.value).onSuccess { num ->
                    continuation.resume(num)
                }
            }

            negativeButton(R.string.neg_cancel) { it.cancel() }

            val alertDialog = show()

            alertDialog.resumeNullOnDismiss(continuation)
            alertDialog.dismissOnDestroy(lifecycleOwner)

            lifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
                text.map { isValid(it) }
                    .collectLatest { isValid ->
                        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled =
                            isValid.isSuccess
                        textInputLayout.error =
                            isValid.errorOrNull()?.getFullMessage(resourceProvider)
                    }
            }
        }
    }
}

suspend fun Context.okDialog(
    lifecycleOwner: LifecycleOwner,
    message: String,
    title: String? = null,
) = suspendCancellableCoroutine<PopupUi.OkResponse?> { continuation ->

    val alertDialog = materialAlertDialog {

        setTitle(title)
        setMessage(message)

        okButton {
            continuation.resume(PopupUi.OkResponse)
        }
    }

    alertDialog.show()

    //this prevents window leak
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
            dismiss()
        }
    })
}

object DialogUtils {

    fun getCompatibleOnScreenKeyboardDialog(ctx: Context): MaterialAlertDialogBuilder {
        return MaterialAlertDialogBuilder(ctx).apply {

            val appStoreModel: ChooseAppStoreModel

            if (LeanbackUtils.isTvDevice(ctx)) {
                titleResource = R.string.dialog_title_install_leanback_keyboard
                messageResource = R.string.dialog_message_install_leanback_keyboard

                appStoreModel = ChooseAppStoreModel(
                    githubLink = ctx.str(R.string.url_github_keymapper_leanback_keyboard),
                )
            } else {
                titleResource = R.string.dialog_title_install_gui_keyboard
                messageResource = R.string.dialog_message_install_gui_keyboard

                appStoreModel = ChooseAppStoreModel(
                    playStoreLink = ctx.str(R.string.url_play_store_keymapper_gui_keyboard),
                    githubLink = ctx.str(R.string.url_github_keymapper_gui_keyboard),
                    fdroidLink = ctx.str(R.string.url_fdroid_keymapper_gui_keyboard)
                )
            }

            DialogChooseAppStoreBinding.inflate(LayoutInflater.from(ctx)).apply {
                model = appStoreModel
                setView(this.root)
            }

            negativeButton(R.string.neg_cancel) { it.cancel() }
        }
    }
}