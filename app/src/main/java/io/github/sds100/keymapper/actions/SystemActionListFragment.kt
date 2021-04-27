package io.github.sds100.keymapper.actions

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.sectionHeader
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by sds100 on 31/03/2020.
 */
class SystemActionListFragment : SimpleRecyclerViewFragment<ListItem>() {

    companion object {
        const val REQUEST_KEY = "request_system_action"
        const val EXTRA_SYSTEM_ACTION = "extra_system_action"
        const val SEARCH_STATE_KEY = "key_system_action_search_state"
    }

    private val viewModel: SystemActionListViewModel by activityViewModels {
        Inject.systemActionListViewModel(requireContext())
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var requestKey: String? = REQUEST_KEY

    override val listItems: Flow<ListUiState<ListItem>>
        get() = viewModel.state.map { it.listItems }

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.state.collectLatest { state ->
                binding.caption = if (state.showUnsupportedActionsMessage) {
                    str(R.string.your_device_doesnt_support_some_actions)
                } else {
                    null
                }
            }
        }

        viewModel.showPopups(this, binding)

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest {
                returnResult(EXTRA_SYSTEM_ACTION to Json.encodeToString(it))
            }
        }
    }

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<ListItem>) {
        recyclerView.withModels {
            listItems.forEach { listItem ->
                if (listItem is SectionHeaderListItem) {
                    sectionHeader {
                        id(listItem.id)
                        header(listItem.text)
                    }
                }

                if (listItem is SystemActionListItem) {
                    simple {
                        id(listItem.id)
                        primaryText(listItem.title)
                        icon(listItem.icon)
                        tintType(TintType.ON_SURFACE)

                        isSecondaryTextAnError(listItem.showRequiresRootMessage)

                        if (listItem.showRequiresRootMessage) {
                            secondaryText(str(R.string.requires_root))
                        } else {
                            secondaryText(null)
                        }

                        onClick { _ ->
                            viewModel.onSystemActionClick(listItem.systemActionId)
                        }
                    }
                }
            }
        }
    }

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }
}