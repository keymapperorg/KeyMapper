package io.github.sds100.keymapper.actions.keyevent

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyRecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.RecyclerViewUtils
import io.github.sds100.keymapper.util.ui.SimpleListItem
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 30/03/2020.
 */

@AndroidEntryPoint
class ChooseKeyCodeFragment : SimpleRecyclerViewFragment<SimpleListItem>() {
    companion object {
        const val EXTRA_KEYCODE = "extra_keycode"
        const val SEARCH_STATE_KEY = "key_keycode_search_state"
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY

    private val args: ChooseKeyCodeFragmentArgs by navArgs()
    private val viewModel: ChooseKeyCodeViewModel by viewModels()

    override val listItems: Flow<State<List<SimpleListItem>>>
        get() = viewModel.state

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

        RecyclerViewUtils.applySimpleListItemDecorations(binding.epoxyRecyclerView)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest {
                returnResult(EXTRA_KEYCODE to it)
            }
        }
    }

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<SimpleListItem>) {
        recyclerView.withModels {
            listItems.forEach {
                simple {
                    id(it.id)
                    model(it)

                    onClickListener { _ ->
                        viewModel.onListItemClick(it.id)
                    }
                }
            }
        }
    }

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }

    override fun getRequestKey(): String {
        return args.requestKey
    }
}