package io.github.sds100.keymapper.base.util.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.DialogChooseAppStoreBinding
import io.github.sds100.keymapper.system.url.UrlUtils
import io.github.sds100.keymapper.common.util.launchRepeatOnLifecycle
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.dropWhile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.runBlocking
import splitties.toast.toast



class PopupViewModelImpl : PopupViewModel {

    private val _onUserResponse by lazy { MutableSharedFlow<OnPopupResponseEvent>() }
    override val onUserResponse by lazy { _onUserResponse.asSharedFlow() }

    private val getUserResponse by lazy { MutableSharedFlow<ShowPopupEvent>() }
    override val showPopup by lazy { getUserResponse.asSharedFlow() }

    override suspend fun showPopup(event: ShowPopupEvent) {
        // wait for the view to collect so no dialogs are missed
        getUserResponse.subscriptionCount.first { it > 0 }

        getUserResponse.emit(event)
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

fun PopupViewModel.onUserResponse(key: String, response: Any?) {
    onUserResponse(OnPopupResponseEvent(key, response))
}

suspend inline fun <reified R> PopupViewModel.showPopup(
    key: String,
    ui: PopupUi<R>,
): R? {
    showPopup(ShowPopupEvent(key, ui))

    /*
    This ensures only one job for a dialog is active at once by cancelling previous jobs when a new
    dialog is shown with the same key
     */
    return merge(
        showPopup.dropWhile { it.key != key }.map { null },
        onUserResponse.dropWhile { it.response !is R? && it.key != key }.map { it.response },
    ).first() as R?
}

fun PopupViewModel.showPopups(
    fragment: Fragment,
    binding: ViewDataBinding,
) {
    showPopups(fragment.requireContext(), fragment.viewLifecycleOwner, binding.root)
}

fun PopupViewModel.showPopups(
    fragment: Fragment,
    rootView: View,
) {
    showPopups(fragment.requireContext(), fragment.viewLifecycleOwner, rootView)
}

fun PopupViewModel.showPopups(
    activity: FragmentActivity,
    rootView: View,
) {
    showPopups(activity, activity, rootView)
}

fun PopupViewModel.showPopups(
    ctx: Context,
    lifecycleOwner: LifecycleOwner,
    rootView: View,
) {
    // must be onCreate because dismissing in onDestroy
    lifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
        showPopup.onEach { event ->
            var responded = false

            val observer = object : LifecycleObserver {
                @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
                fun onDestroy() {
                    if (!responded) {
                        onUserResponse(event.key, null)
                        responded = true
                        lifecycleOwner.lifecycle.removeObserver(this)
                    }
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)

            val response: Any?

            when (event.ui) {
                is PopupUi.Ok ->
                    response = ctx.okDialog(lifecycleOwner, event.ui.message, event.ui.title)

                is PopupUi.MultiChoice<*> ->
                    response = ctx.multiChoiceDialog(lifecycleOwner, event.ui.items)

                is PopupUi.SingleChoice<*> ->
                    response = ctx.singleChoiceDialog(lifecycleOwner, event.ui.items)

                is PopupUi.SnackBar ->
                    response = SnackBarUtils.show(
                        rootView.findViewById(R.id.coordinatorLayout),
                        event.ui.message,
                        event.ui.actionText,
                        event.ui.long,
                    )

                is PopupUi.Text ->
                    response = ctx.editTextStringAlertDialog(
                        lifecycleOwner,
                        event.ui.hint,
                        event.ui.allowEmpty,
                        event.ui.text,
                        event.ui.inputType,
                        event.ui.message,
                        event.ui.autoCompleteEntries,
                    )

                is PopupUi.Dialog ->
                    response = ctx.materialAlertDialog(lifecycleOwner, event.ui)

                is PopupUi.Toast -> {
                    ctx.toast(event.ui.text)
                    response = Unit
                }

                is PopupUi.ChooseAppStore -> {
                    val view = DialogChooseAppStoreBinding.inflate(LayoutInflater.from(ctx)).apply {
                        model = event.ui.model
                    }.root

                    response = ctx.materialAlertDialogCustomView(
                        lifecycleOwner,
                        event.ui.title,
                        event.ui.message,
                        positiveButtonText = event.ui.positiveButtonText,
                        negativeButtonText = event.ui.negativeButtonText,
                        view = view,
                    )
                }

                is PopupUi.OpenUrl -> {
                    UrlUtils.openUrl(ctx, event.ui.url)
                    response = Unit
                }
            }

            if (!responded) {
                onUserResponse(event.key, response)
                responded = true
            }

            lifecycleOwner.lifecycle.removeObserver(observer)
        }.launchIn(this)
    }
}
