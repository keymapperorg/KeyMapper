package io.github.sds100.keymapper.ui.fragment

import androidx.fragment.app.activityViewModels
import io.github.sds100.keymapper.data.model.ActivityListItemModel
import io.github.sds100.keymapper.data.viewmodel.ActivityListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 22/02/2020.
 */
class ActivityListFragment :
    DefaultRecyclerViewFragment<List<ActivityListItemModel>>() {

    companion object {
        const val REQUEST_KEY = "request_activity"
        const val EXTRA_ACTIVITY_INFO = "extra_activity_info"
        const val SEARCH_STATE_KEY = "key_activity_list_search_state"
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var requestKey: String? = REQUEST_KEY

    private val viewModel: ActivityListViewModel by activityViewModels {
        InjectorUtils.provideActivityListViewModel(requireContext())
    }

    override val modelState
        get() = viewModel

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }

    override fun populateList(
        binding: FragmentRecyclerviewBinding,
        model: List<ActivityListItemModel>?
    ) {

        binding.epoxyRecyclerView.withModels {
            model?.forEach {
                simple {
                    id(it.id)
                    primaryText(it.appName)
                    secondaryText(it.activityInfo.activityName)
                    icon(it.icon)

                    onClick { _ ->
                        returnResult(EXTRA_ACTIVITY_INFO to it.activityInfo)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.rebuildModels()
    }
}