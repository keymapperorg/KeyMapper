package io.github.sds100.keymapper.system.apps

import androidx.fragment.app.activityViewModels
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Created by sds100 on 22/02/2020.
 */
class ChooseActivityFragment : SimpleRecyclerViewFragment<ActivityListItem>() {

    companion object {
        const val REQUEST_KEY = "request_activity"
        const val EXTRA_ACTIVITY_INFO = "extra_activity_info"
        const val SEARCH_STATE_KEY = "key_activity_list_search_state"
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var requestKey: String? = REQUEST_KEY

    private val viewModel: ChooseActivityViewModel by activityViewModels {
        Inject.chooseActivityViewModel(requireContext())
    }

    override val listItems: Flow<ListUiState<ActivityListItem>>
        get() = viewModel.listItems

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<ActivityListItem>) {
        recyclerView.withModels {
            listItems.forEach {
                simple {
                    id(it.id)
                    primaryText(it.appName)
                    secondaryText(it.activityInfo.activityName)
                    icon(it.icon)

                    onClick { _ ->
                        returnResult(EXTRA_ACTIVITY_INFO to Json.encodeToString(it.activityInfo))
                    }
                }
            }
        }
    }
}