package io.github.sds100.keymapper.constraints

import android.os.Bundle
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.GridLayoutManager
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.simpleGrid
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.*
import io.github.sds100.keymapper.util.viewLifecycleScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A placeholder fragment containing a simple view.
 */
class ChooseConstraintFragment
    : SimpleRecyclerViewFragment<SimpleListItem>() {

    companion object {
        const val EXTRA_CONSTRAINT = "extra_constraint"
    }

    private val navArgs by navArgs<ChooseConstraintFragmentArgs>()

    private val viewModel: ChooseConstraintViewModel by viewModels {
        Inject.chooseConstraintListViewModel(requireContext())
    }

    override val listItems: Flow<State<List<SimpleListItem>>>
        get() = viewModel.listItems

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setSupportedConstraints(Json.decodeFromString(navArgs.supportedConstraints))
        viewModel.setupNavigation(this)

        launchRepeatOnLifecycle(Lifecycle.State.CREATED) {
            viewModel.returnResult.collectLatest {
                viewLifecycleScope.launchWhenResumed {
                    returnResult(EXTRA_CONSTRAINT to Json.encodeToString(it))
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

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<SimpleListItem>
    ) {
        RecyclerViewUtils.setSpanCountForSimpleListItemGrid(recyclerView)

        recyclerView.withModels {
            listItems.forEach { listItem ->
                if (spanCount == 1) {
                    simple {
                        id(listItem.id)
                        model(listItem)

                        onClickListener { _ ->
                            viewModel.onListItemClick(listItem.id)
                        }
                    }
                } else {
                    simpleGrid {
                        id(listItem.id)
                        model(listItem)

                        onClickListener { _ ->
                            viewModel.onListItemClick(listItem.id)
                        }
                    }
                }
            }
        }
    }

    override fun getRequestKey(): String {
        return navArgs.requestKey
    }
}