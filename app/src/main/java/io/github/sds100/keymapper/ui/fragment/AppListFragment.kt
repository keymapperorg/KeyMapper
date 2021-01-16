package io.github.sds100.keymapper.ui.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import io.github.sds100.keymapper.data.model.AppListItemModel
import io.github.sds100.keymapper.data.viewmodel.AppListViewModel
import io.github.sds100.keymapper.databinding.FragmentAppListBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 22/02/2020.
 */
class AppListFragment : RecyclerViewFragment<List<AppListItemModel>, FragmentAppListBinding>() {

    companion object {
        const val REQUEST_KEY = "request_app"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val SEARCH_STATE_KEY = "key_app_search_state"
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var requestKey: String? = REQUEST_KEY

    private val viewModel: AppListViewModel by activityViewModels {
        InjectorUtils.provideAppListViewModel(requireContext())
    }

    override val modelState
        get() = viewModel

    override fun subscribeUi(binding: FragmentAppListBinding) {
        binding.viewModel = viewModel
    }

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentAppListBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

    override fun getBottomAppBar(binding: FragmentAppListBinding) = binding.appBar

    override fun populateList(binding: FragmentAppListBinding,
                              model: List<AppListItemModel>?) {

        binding.epoxyRecyclerView.withModels {
            model?.forEach {
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
}