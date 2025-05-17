package io.github.sds100.keymapper.base.actions.keyevent

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.base.util.Inject
import io.github.sds100.keymapper.common.util.state.State
import io.github.sds100.keymapper.common.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.base.util.ui.RecyclerViewUtils
import io.github.sds100.keymapper.base.util.ui.SimpleListItemOld
import io.github.sds100.keymapper.base.util.ui.SimpleRecyclerViewFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

class ChooseKeyCodeFragment : SimpleRecyclerViewFragment<SimpleListItemOld>() {
    companion object {
        const val EXTRA_KEYCODE = "extra_keycode"
        const val SEARCH_STATE_KEY = "key_keycode_search_state"
    }

    override var searchStateKey: String? =
        io.github.sds100.keymapper.mapping.actions.keyevent.ChooseKeyCodeFragment.Companion.SEARCH_STATE_KEY

    private val args: ChooseKeyCodeFragmentArgs by navArgs()
    private val viewModel: io.github.sds100.keymapper.base.actions.keyevent.ChooseKeyCodeViewModel by viewModels {
        Inject.chooseKeyCodeViewModel()
    }

    override val listItems: Flow<State<List<SimpleListItemOld>>>
        get() = viewModel.state

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

        RecyclerViewUtils.applySimpleListItemDecorations(binding.epoxyRecyclerView)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest {
                returnResult(io.github.sds100.keymapper.mapping.actions.keyevent.ChooseKeyCodeFragment.Companion.EXTRA_KEYCODE to it)
            }
        }
    }

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<SimpleListItemOld>) {
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

    override fun getRequestKey(): String = args.requestKey
}
