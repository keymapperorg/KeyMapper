package io.github.sds100.keymapper.util.ui

import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.alertdialog.appcompat.positiveButton
import splitties.toast.toast
import timber.log.Timber
import kotlin.coroutines.resume

/**
 * Created by sds100 on 23/03/2021.
 */

class PopupViewModelImpl : PopupViewModel {

    private val _onUserResponse = MutableSharedFlow<OnPopupResponseEvent>()
    override val onUserResponse = _onUserResponse.asSharedFlow()

    private val _getUserResponse = MutableSharedFlow<ShowPopupEvent>()
    override val showPopup = _getUserResponse.asSharedFlow()

    override suspend fun showPopup(event: ShowPopupEvent) {
        //wait for the view to collect so no dialogs are missed
        _getUserResponse.subscriptionCount.first { it > 0 }

        _getUserResponse.emit(event)
    }

    override fun onUserResponse(event: OnPopupResponseEvent) {
        runBlocking { _onUserResponse.emit(event) }
    }
}

interface PopupViewModel {
    val showPopup: SharedFlow<ShowPopupEvent>
    val onUserResponse: SharedFlow<OnPopupResponseEvent>

    suspend fun showPopup(event: ShowPopupEvent)
    fun onUserResponse(event: OnPopupResponseEvent)
}

fun PopupViewModel.onUserResponse(key: String, response: PopupResponse?) {
    onUserResponse(OnPopupResponseEvent(key, response))
}

suspend inline fun <reified R : PopupResponse> PopupViewModel.showPopup(
    key: String,
    ui: PopupUi<R>
): R? {
    showPopup(ShowPopupEvent(key, ui))

    /*
    This ensures only one job for a dialog is active at once by cancelling previous jobs when a new
    dialog is shown with the same key
     */
    return merge(
        showPopup.dropWhile { it.key != key }.map { null },
        onUserResponse.dropWhile { it.response !is R? && it.key != key }.map { it.response }
    ).first() as R?
}

fun PopupViewModel.showPopups(
    fragment: Fragment,
    binding: ViewDataBinding
) {
    val lifecycleOwner = fragment.viewLifecycleOwner
    val ctx = fragment.requireContext()

    //must be onCreate because dismissing in onDestroy
    lifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
        showPopup.onEach { event ->
            var responded = false

            lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    if (!responded) {
                        onUserResponse(event.key, null)
                        responded = true
                        lifecycleOwner.lifecycle.removeObserver(this)
                    }
                }
            })

            val response = when (event.ui) {
                is PopupUi.Ok ->
                    ctx.okDialog(lifecycleOwner, event.ui.message, event.ui.title)

                is PopupUi.MultiChoice<*> ->
                    ctx.multiChoiceDialog(lifecycleOwner, event.ui.items)

                is PopupUi.SingleChoice<*> ->
                    ctx.singleChoiceDialog(lifecycleOwner, event.ui.items)

                is PopupUi.SnackBar ->
                    SnackBarUtils.show(
                        binding.root.findViewById(R.id.coordinatorLayout),
                        event.ui.message,
                        event.ui.actionText,
                        event.ui.long
                    )

                is PopupUi.Text -> ctx.editTextStringAlertDialog(
                    lifecycleOwner,
                    event.ui.hint,
                    event.ui.allowEmpty,
                    event.ui.text
                )

                is PopupUi.Dialog -> ctx.materialAlertDialog(lifecycleOwner, event.ui)

                is PopupUi.InstallCompatibleOnScreenKeyboard -> {
                    suspendCancellableCoroutine { continuation ->
                        val dialogBuilder = DialogUtils.getCompatibleOnScreenKeyboardDialog(ctx)

                        dialogBuilder.positiveButton(R.string.pos_never_show_again) {
                            continuation.resume(DialogResponse.POSITIVE)
                        }

                        dialogBuilder.show().apply {
                            resumeNullOnDismiss(continuation)
                            dismissOnDestroy(lifecycleOwner)

                            continuation.invokeOnCancellation {
                                dismiss()
                            }
                        }
                    }
                }

                is PopupUi.Toast -> {
                    ctx.toast(event.ui.text)
                    object : PopupResponse {}
                }
            }

            if (!responded) {
                onUserResponse(event.key, response)
                responded = true
            }
        }.launchIn(this)
    }
}