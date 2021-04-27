package io.github.sds100.keymapper.constraints

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.constraint
import io.github.sds100.keymapper.databinding.FragmentConstraintListBinding
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.ui.RecyclerViewFragment
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import splitties.toast.toast

/**
 * Created by sds100 on 29/11/20.
 */
abstract class ConfigConstraintsFragment
    : RecyclerViewFragment<ConstraintListItem, FragmentConstraintListBinding>() {

    companion object {
        const val CHOOSE_CONSTRAINT_REQUEST_KEY = "request_choose_constraint"
    }

    abstract val configConstraintsViewModel: ConfigConstraintsViewModel

    override val listItems: Flow<ListUiState<ConstraintListItem>>
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
            val direction = NavAppDirections.actionGlobalChooseConstraint(
                CHOOSE_CONSTRAINT_REQUEST_KEY,
                Json.encodeToString(configConstraintsViewModel.allowedConstraints)
            )

            findNavController().navigate(direction)
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
    override fun getEmptyListPlaceHolder(binding: FragmentConstraintListBinding) =
        binding.emptyListPlaceHolder
}