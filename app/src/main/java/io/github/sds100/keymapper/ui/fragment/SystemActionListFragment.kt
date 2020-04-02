package io.github.sds100.keymapper.ui.fragment

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import com.airbnb.epoxy.EpoxyController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Option
import io.github.sds100.keymapper.data.model.SelectedSystemActionModel
import io.github.sds100.keymapper.data.model.SystemActionDef
import io.github.sds100.keymapper.data.model.SystemActionListItemModel
import io.github.sds100.keymapper.data.viewmodel.SystemActionListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.sectionHeader
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.ui.callback.ProgressCallback
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.SystemActionUtils
import io.github.sds100.keymapper.util.result.handle
import io.github.sds100.keymapper.util.result.onSuccess
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.coroutines.showAndAwaitOkOrDismiss
import splitties.alertdialog.appcompat.message
import splitties.alertdialog.appcompat.title
import splitties.experimental.ExperimentalSplittiesApi
import splitties.resources.appStr
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by sds100 on 31/03/2020.
 */
class SystemActionListFragment : RecyclerViewFragment() {

    companion object {
        const val SAVED_STATE_KEY = "key_system_action"
        const val SEARCH_STATE_KEY = "key_system_action_search_state"
    }

    private val mViewModel: SystemActionListViewModel by activityViewModels {
        InjectorUtils.provideSystemActionListViewModel()
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var selectedModelKey: String? = SAVED_STATE_KEY

    override val progressCallback: ProgressCallback?
        get() = mViewModel

    @ExperimentalSplittiesApi
    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        binding.apply {
            mViewModel.unsupportedSystemActions.observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    caption = appStr(R.string.your_device_doesnt_support_some_actions)
                }
            }

            mViewModel.filteredModelList.observe(viewLifecycleOwner) {
                epoxyRecyclerView.withModels {
                    for ((sectionHeader, systemActions) in it) {
                        sectionHeader {
                            id(sectionHeader)
                            header(sectionHeader)
                        }

                        systemActions.forEach { systemAction ->
                            createSimpleListItem(systemAction)
                        }
                    }
                }
            }
        }
    }

    override fun onSearchQuery(query: String?) {
        mViewModel.searchQuery.value = query
    }

    @ExperimentalSplittiesApi
    private suspend fun onSystemActionClick(systemActionDef: SystemActionDef) = withContext(lifecycleScope.coroutineContext) {

        val messageOnSelection = systemActionDef.getMessageOnSelection()
        var selectedOptionData: String? = null

        if (messageOnSelection != null) {
            requireActivity().alertDialog {
                title = systemActionDef.getDescription()
                message = messageOnSelection
            }.showAndAwaitOkOrDismiss()
        }

        systemActionDef.getOptions().onSuccess { options ->
            val optionLabels = options.map { optionId ->
                Option.getOptionLabel(systemActionDef.id, optionId).handle(
                    onSuccess = { it },
                    onFailure = { it.fullMessage }
                )
            }

            selectedOptionData = suspendCoroutine<String> {
                requireActivity().alertDialog {
                    setItems(optionLabels.toTypedArray()) { _, which ->
                        val option = options[which]

                        it.resume(option)

                        cancelButton {
                            cancel()
                        }
                    }

                    show()
                }
            }
        }

        selectModel(SelectedSystemActionModel(systemActionDef.id, selectedOptionData))
    }

    @ExperimentalSplittiesApi
    private fun EpoxyController.createSimpleListItem(systemAction: SystemActionListItemModel) = simple {
        id(systemAction.id)
        primaryText(systemAction.description)
        icon(systemAction.icon)
        onSurfaceTint(true)

        isSecondaryTextAnError(systemAction.requiresRoot)

        if (systemAction.requiresRoot) {
            secondaryText(appStr(R.string.requires_root))
        } else {
            secondaryText(null)
        }

        onClick { _ ->
            SystemActionUtils.getSystemActionDef(systemAction.id).onSuccess {
                lifecycleScope.launch {
                    onSystemActionClick(it)
                }
            }
        }
    }
}