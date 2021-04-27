package io.github.sds100.keymapper.system.apps

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.databinding.FragmentChooseAppBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.ui.RecyclerViewFragment
import io.github.sds100.keymapper.util.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 22/02/2020.
 */
class ChooseAppFragment : RecyclerViewFragment<AppListItem, FragmentChooseAppBinding>() {

    companion object {
        const val REQUEST_KEY = "request_app"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val SEARCH_STATE_KEY = "key_app_search_state"
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var requestKey: String? = REQUEST_KEY

    private val viewModel: ChooseAppViewModel by activityViewModels {
        Inject.chooseAppViewModel(requireContext())
    }

    override val listItems: Flow<ListUiState<AppListItem>>
        get() = viewModel.state.map { it.listItems }

    override fun subscribeUi(binding: FragmentChooseAppBinding) {
        binding.viewModel = viewModel
    }

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentChooseAppBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

    override fun getBottomAppBar(binding: FragmentChooseAppBinding) = binding.appBar

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<AppListItem>) {
        binding.epoxyRecyclerView.withModels {
            listItems.forEach {
                simple {
                    id(it.packageName)
                    primaryText(it.appName)
                    icon(it.icon)

                    onClick { _ ->
                        returnResult(EXTRA_PACKAGE_NAME to it.packageName)
                    }
                }
            }
        }
    }

    override fun getRecyclerView(binding: FragmentChooseAppBinding) = binding.epoxyRecyclerView
    override fun getProgressBar(binding: FragmentChooseAppBinding) = binding.progressBar
    override fun getEmptyListPlaceHolder(binding: FragmentChooseAppBinding) =
        binding.emptyListPlaceHolder
}