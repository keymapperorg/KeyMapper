package io.github.sds100.keymapper.ui.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.google.android.material.bottomappbar.BottomAppBar
import io.github.sds100.keymapper.data.viewmodel.AppListViewModel
import io.github.sds100.keymapper.databinding.FragmentAppListBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 22/02/2020.
 */
class AppListFragment : RecyclerViewFragment<FragmentAppListBinding>() {

    companion object {
        const val REQUEST_KEY = "request_app"
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val SEARCH_STATE_KEY = "key_app_search_state"
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var requestKey: String? = REQUEST_KEY

    override val appBar: BottomAppBar
        get() = binding.appBar

    private val viewModel: AppListViewModel by activityViewModels {
        InjectorUtils.provideAppListViewModel(requireContext())
    }

    override fun subscribeUi(binding: FragmentAppListBinding) {
        binding.viewModel = viewModel

        viewModel.filteredAppModelList.observe(viewLifecycleOwner, { appModelList ->

            binding.epoxyRecyclerView.withModels {

                if (appModelList !is Data) return@withModels

                appModelList.data.forEach {
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
        })
    }

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentAppListBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }
}