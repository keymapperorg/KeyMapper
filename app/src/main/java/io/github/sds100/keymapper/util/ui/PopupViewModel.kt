package io.github.sds100.keymapper.util.ui

import android.view.LayoutInflater
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.addRepeatingJob
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.DialogChooseAppStoreBinding
import io.github.sds100.keymapper.home.ChooseAppStoreModel
import io.github.sds100.keymapper.util.str
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.positiveButton
import splitties.alertdialog.appcompat.titleResource
import splitties.alertdialog.material.materialAlertDialog
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

    lifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
        showPopup.collectLatest { event ->
            var responded = false

            lifecycleOwner.lifecycle.addObserver(object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    if (!responded) {
                        onUserResponse(event.key, null)
                        responded = true
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
                    event.ui.allowEmpty
                )

                is PopupUi.Dialog -> ctx.materialAlertDialog(lifecycleOwner, event.ui)

                is PopupUi.InstallGuiKeyboard -> {
                    suspendCancellableCoroutine { continuation ->
                        val dialog = ctx.materialAlertDialog {
                            titleResource = R.string.dialog_title_install_gui_keyboard
                            messageResource = R.string.dialog_message_install_gui_keyboard

                            DialogChooseAppStoreBinding.inflate(LayoutInflater.from(ctx)).apply {
                                model = ChooseAppStoreModel(
                                    playStoreLink = ctx.str(R.string.url_play_store_keymapper_gui_keyboard),
                                    githubLink = ctx.str(R.string.url_github_keymapper_gui_keyboard),
                                    fdroidLink = ctx.str(R.string.url_fdroid_keymapper_gui_keyboard)
                                )

                                setView(this.root)
                            }

                            positiveButton(R.string.pos_never_show_again) {
                                continuation.resume(DialogResponse.POSITIVE)
                            }

                            show()
                        }

                        dialog.apply {
                            resumeNullOnDismiss(continuation)
                            dismissOnDestroy(lifecycleOwner)

                            continuation.invokeOnCancellation {
                                dismiss()
                            }
                        }
                    }
                }
            }

            if (!responded) {
                onUserResponse(event.key, response)
                responded = true
            }
        }
    }
}