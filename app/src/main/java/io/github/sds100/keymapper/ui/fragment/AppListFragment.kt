package io.github.sds100.keymapper.ui.fragment

import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import io.github.sds100.keymapper.data.viewmodel.AppListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 22/02/2020.
 */
class AppListFragment : RecyclerViewFragment() {

    companion object {
        const val SAVED_STATE_KEY = "key_app"
    }

    override val savedStateKey = SAVED_STATE_KEY

    private val mViewModel: AppListViewModel by viewModels {
        InjectorUtils.provideAppListViewModel(requireContext())
    }

    override val progressCallback
        get() = mViewModel

    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        mViewModel.appModelList.observe(viewLifecycleOwner) { appModelList ->
            binding.epoxyRecyclerView.withModels {
                appModelList.forEach {
                    simple {
                        id(it.packageName)
                        primaryText(it.appName)
                        icon(it.icon)

                        onClick { _ ->
                            selectModel(it)
                        }
                    }
                }
            }
        }
    }
}