package io.github.sds100.keymapper.base.utils.ui

import android.content.Context
import android.view.View
import android.widget.Toast
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import io.github.sds100.keymapper.base.R
import io.github.sds100.keymapper.system.url.UrlUtils
import javax.inject.Inject
import javax.inject.Singleton
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

@Singleton
class DialogProviderImpl @Inject constructor() : DialogProvider {

    private val _onUserResponse by lazy { MutableSharedFlow<OnDialogResponseEvent>() }
    override val onUserResponse by lazy { _onUserResponse.asSharedFlow() }

    private val getUserResponse by lazy { MutableSharedFlow<ShowDialogEvent>() }
    override val showDialog by lazy { getUserResponse.asSharedFlow() }

    override suspend fun showDialog(event: ShowDialogEvent) {
        // wait for the view to collect so no dialogs are missed
        getUserResponse.subscriptionCount.first { it > 0 }

        getUserResponse.emit(event)
    }

    override fun onUserResponse(event: OnDialogResponseEvent) {
        runBlocking { _onUserResponse.emit(event) }
    }
}

interface DialogProvider {
    val showDialog: SharedFlow<ShowDialogEvent>
    val onUserResponse: SharedFlow<OnDialogResponseEvent>

    suspend fun showDialog(event: ShowDialogEvent)
    fun onUserResponse(event: OnDialogResponseEvent)
}

fun DialogProvider.onUserResponse(key: String, response: Any?) {
    onUserResponse(OnDialogResponseEvent(key, response))
}

suspend inline fun <reified R> DialogProvider.showDialog(key: String, ui: DialogModel<R>): R? {
    showDialog(ShowDialogEvent(key, ui))

    /*
    This ensures only one job for a dialog is active at once by cancelling previous jobs when a new
    dialog is shown with the same key
     */
    return merge(
        showDialog.dropWhile { it.key != key }.map { null },
        onUserResponse.dropWhile { it.response !is R? && it.key != key }.map { it.response },
    ).first() as R?
}

fun DialogProvider.showDialogs(fragment: Fragment, binding: ViewDataBinding) {
    showDialogs(fragment.requireContext(), fragment.viewLifecycleOwner, binding.root)
}

fun DialogProvider.showDialogs(fragment: Fragment, rootView: View) {
    showDialogs(fragment.requireContext(), fragment.viewLifecycleOwner, rootView)
}

fun DialogProvider.showDialogs(activity: FragmentActivity, rootView: View) {
    showDialogs(activity, activity, rootView)
}

fun DialogProvider.showDialogs(ctx: Context, lifecycleOwner: LifecycleOwner, rootView: View) {
    // must be onCreate because dismissing in onDestroy
    lifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
        showDialog.onEach { event ->
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
                is DialogModel.Ok ->
                    response = ctx.okDialog(lifecycleOwner, event.ui.message, event.ui.title)

                is DialogModel.MultiChoice<*> ->
                    response = ctx.multiChoiceDialog(lifecycleOwner, event.ui.items)

                is DialogModel.SingleChoice<*> ->
                    response = ctx.singleChoiceDialog(lifecycleOwner, event.ui.items)

                is DialogModel.SnackBar ->
                    response = SnackBarUtils.show(
                        rootView.findViewById(R.id.coordinatorLayout),
                        event.ui.message,
                        event.ui.actionText,
                        event.ui.long,
                    )

                is DialogModel.Text ->
                    response = ctx.editTextStringAlertDialog(
                        lifecycleOwner,
                        event.ui.hint,
                        event.ui.allowEmpty,
                        event.ui.text,
                        event.ui.inputType,
                        event.ui.message,
                        event.ui.autoCompleteEntries,
                    )

                is DialogModel.Alert ->
                    response = ctx.materialAlertDialog(lifecycleOwner, event.ui)

                is DialogModel.Toast -> {
                    Toast.makeText(ctx, event.ui.text, Toast.LENGTH_SHORT).show()
                    response = Unit
                }

                is DialogModel.OpenUrl -> {
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
