package io.github.sds100.keymapper.constraints

import android.view.LayoutInflater
import android.view.ViewGroup
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.constraint
import io.github.sds100.keymapper.databinding.FragmentConstraintListBinding
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.RecyclerViewFragment
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 29/11/20.
 */
abstract class ConfigConstraintsFragment
    : RecyclerViewFragment<ConstraintListItem, FragmentConstraintListBinding>() {

    companion object {
        const val CHOOSE_CONSTRAINT_REQUEST_KEY = "request_choose_constraint"
    }

    abstract val configConstraintsViewModel: ConfigConstraintsViewModel

    override val listItems: Flow<State<List<ConstraintListItem>>>
        get() = configConstraintsViewModel.state.map { it.constraintList }

    override fun bind(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentConstraintListBinding.inflate(inflater, container, false).apply {
        lifecycleOwner = viewLifecycleOwner
    }

    override fun subscribeUi(binding: FragmentConstraintListBinding) {
        binding.viewModel = configConstraintsViewModel
        configConstraintsViewModel.showPopups(this, binding)

        binding.setOnAddConstraintClick {
            configConstraintsViewModel.onAddConstraintClick()
        }
    }

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<ConstraintListItem>
    ) {
        recyclerView.withModels {
            listItems.forEach { listItem ->
                constraint {
                    id(listItem.id)
                    model(listItem)
                    onCardClick { _ ->
                        configConstraintsViewModel.onListItemClick(listItem.id)
                    }

                    onRemoveClick { _ ->
                        configConstraintsViewModel.onRemoveConstraintClick(listItem.id)
                    }
                }
            }
        }
    }

    override fun getRecyclerView(binding: FragmentConstraintListBinding) = binding.epoxyRecyclerView
    override fun getProgressBar(binding: FragmentConstraintListBinding) = binding.progressBar
    override fun getEmptyListPlaceHolderTextView(binding: FragmentConstraintListBinding) =
        binding.emptyListPlaceHolder
}