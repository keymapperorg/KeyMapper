package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import io.github.sds100.keymapper.ui.callback.StringResourceProvider
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.getFullMessage
import io.github.sds100.keymapper.util.result.handle
import io.github.sds100.keymapper.util.result.onSuccess
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.coroutines.showAndAwaitOkOrDismiss
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.titleResource
import splitties.experimental.ExperimentalSplittiesApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by sds100 on 31/03/2020.
 */
class SystemActionListFragment : RecyclerViewFragment(), StringResourceProvider {

    companion object {
        const val SAVED_STATE_KEY = "key_system_action"
        const val SEARCH_STATE_KEY = "key_system_action_search_state"
    }

    private val mViewModel: SystemActionListViewModel by activityViewModels {
        InjectorUtils.provideSystemActionListViewModel(requireContext())
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var selectedModelKey: String? = SAVED_STATE_KEY

    override val progressCallback: ProgressCallback?
        get() = mViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mViewModel.registerStringResourceProvider(this)

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    @ExperimentalSplittiesApi
    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        binding.apply {

            mViewModel.unsupportedSystemActions.observe(viewLifecycleOwner) {
                if (it.isNotEmpty()) {
                    caption = str(R.string.your_device_doesnt_support_some_actions)
                }
            }

            mViewModel.filteredModelList.observe(viewLifecycleOwner) {
                epoxyRecyclerView.withModels {
                    for ((sectionHeader, systemActions) in it) {
                        sectionHeader {
                            id(sectionHeader)
                            header(str(sectionHeader))
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

    override fun getStringResource(resId: Int) = str(resId)

    override fun onDestroy() {
        super.onDestroy()

        mViewModel.unregisterStringResourceProvider()
    }

    @ExperimentalSplittiesApi
    private suspend fun onSystemActionClick(systemActionDef: SystemActionDef) = withContext(lifecycleScope.coroutineContext) {

        var selectedOptionData: String? = null

        if (systemActionDef.messageOnSelection != null) {
            requireActivity().alertDialog {
                titleResource = systemActionDef.descriptionRes
                messageResource = systemActionDef.messageOnSelection
            }.showAndAwaitOkOrDismiss()
        }

        systemActionDef.getOptions().onSuccess { options ->
            val optionLabels = options.map { optionId ->
                Option.getOptionLabel(requireContext(), systemActionDef.id, optionId).handle(
                    onSuccess = { it },
                    onFailure = { it.getFullMessage(requireContext()) }
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
        primaryText(str(systemAction.descriptionRes))
        icon(drawable(systemAction.iconRes))
        tintType(TintType.ON_SURFACE)

        isSecondaryTextAnError(systemAction.requiresRoot)

        if (systemAction.requiresRoot) {
            secondaryText(str(R.string.requires_root))
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