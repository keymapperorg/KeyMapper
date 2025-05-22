package io.github.sds100.keymapper.base.actions.keyevent

import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyRecyclerView
import dagger.hilt.android.AndroidEntryPoint
import io.github.sds100.keymapper.base.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.base.simple
import io.github.sds100.keymapper.base.utils.ui.RecyclerViewUtils
import io.github.sds100.keymapper.base.utils.ui.SimpleListItemOld
import io.github.sds100.keymapper.base.utils.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.common.utils.State
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class ChooseKeyCodeFragment : SimpleRecyclerViewFragment<SimpleListItemOld>() {
    companion object {
        const val EXTRA_KEYCODE = "extra_keycode"
        const val SEARCH_STATE_KEY = "key_keycode_search_state"
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY

    private val args: ChooseKeyCodeFragmentArgs by navArgs()

    private val viewModel: ChooseKeyCodeViewModel by viewModels()

    override val listItems: Flow<State<List<SimpleListItemOld>>>
        get() = viewModel.state

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

        RecyclerViewUtils.applySimpleListItemDecorations(binding.epoxyRecyclerView)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.returnResult.collectLatest {
                returnResult(EXTRA_KEYCODE to it)
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
