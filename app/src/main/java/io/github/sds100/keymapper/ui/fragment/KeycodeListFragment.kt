package io.github.sds100.keymapper.ui.fragment

import androidx.fragment.app.activityViewModels
import io.github.sds100.keymapper.data.viewmodel.KeycodeListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 30/03/2020.
 */

class KeycodeListFragment : DefaultRecyclerViewFragment() {
    companion object {
        const val REQUEST_KEY = "request_keycode"
        const val EXTRA_KEYCODE = "extra_keycode"
        const val SEARCH_STATE_KEY = "key_keycode_search_state"
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var requestKey: String? = REQUEST_KEY

    private val mViewModel: KeycodeListViewModel by activityViewModels {
        InjectorUtils.provideKeycodeListViewModel()
    }

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        mViewModel.filteredKeycodeLabelList.observe(viewLifecycleOwner, { labelList ->

            binding.state = labelList

            binding.epoxyRecyclerView.withModels {
                if (labelList !is Data) return@withModels

                labelList.data.forEach {
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
        })
    }

    override fun onSearchQuery(query: String?) {
        mViewModel.searchQuery.value = query
    }
}