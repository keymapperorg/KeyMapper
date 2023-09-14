package io.github.sds100.keymapper.actions.tapscreenelement

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.constraints.ChooseConstraintFragment
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.simpleGrid
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.RecyclerViewUtils
import io.github.sds100.keymapper.util.ui.SimpleListItem
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.util.ui.setupNavigation
import io.github.sds100.keymapper.util.ui.showPopups
import io.github.sds100.keymapper.util.viewLifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PickScreenElementFragment: SimpleRecyclerViewFragment<SimpleListItem>() {
    companion object {
        const val EXTRA_ELEMENT_ID = "extra_element_id"
    }

    private val navArgs by navArgs<PickScreenElementFragmentArgs>()

    private val viewModel: PickScreenElementViewModel by viewModels {
        Inject.pickScreenElementActionTypeViewModel(requireContext())
    }

    override val listItems: Flow<State<List<SimpleListItem>>> get() = viewModel.listItems

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setupNavigation(this)

        launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.returnResult.collectLatest {
                viewLifecycleScope.launchWhenResumed {
                    returnResult(EXTRA_ELEMENT_ID to Json.encodeToString(it))
                }
            }
        }
    }

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {
        super.subscribeUi(binding)

        viewModel.showPopups(this, binding)

        binding.epoxyRecyclerView.apply {
            RecyclerViewUtils.applySimpleListItemDecorations(this)

            layoutManager = GridLayoutManager(requireContext(), 1)
            clipToPadding = false
            clipChildren = false
        }
    }

    override fun getRequestKey(): String {
        return navArgs.requestKey
    }

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<SimpleListItem>) {
        RecyclerViewUtils.setSpanCountForSimpleListItemGrid(recyclerView)

        recyclerView.withModels {
            listItems.forEach { listItem ->
                if (spanCount == 1) {
                    simple {
                        id(listItem.id)
                        model(listItem)

                        onClickListener { _ ->
                            viewModel.onListItemClick(listItem.title, listItem.subtitle!!)
                        }
                    }
                } else {
                    simpleGrid {
                        id(listItem.id)
                        model(listItem)

                        onClickListener { _ ->
                            viewModel.onListItemClick(listItem.title, listItem.subtitle!!)
                        }
                    }
                }
            }
        }
    }
}