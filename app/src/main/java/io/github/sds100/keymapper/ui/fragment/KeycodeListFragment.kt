package io.github.sds100.keymapper.ui.fragment

import androidx.fragment.app.activityViewModels
import io.github.sds100.keymapper.data.viewmodel.KeycodeListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.delegate.IModelState

/**
 * Created by sds100 on 30/03/2020.
 */

class KeycodeListFragment : DefaultRecyclerViewFragment<Map<Int, String>>() {
    companion object {
        const val REQUEST_KEY = "request_keycode"
        const val EXTRA_KEYCODE = "extra_keycode"
        const val SEARCH_STATE_KEY = "key_keycode_search_state"
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var requestKey: String? = REQUEST_KEY

    private val viewModel: KeycodeListViewModel by activityViewModels {
        InjectorUtils.provideKeycodeListViewModel()
    }

    override val modelState: IModelState<Map<Int, String>>
        get() = viewModel

    override fun populateList(binding: FragmentRecyclerviewBinding, model: Map<Int, String>?) {
        binding.epoxyRecyclerView.withModels {
            model?.forEach {
                val keycode = it.key
                val label = it.value

                simple {
                    id(keycode)
                    primaryText(label)

                    onClick { _ ->
                        returnResult(EXTRA_KEYCODE to keycode)
                    }
                }
            }
        }

    }

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }
}