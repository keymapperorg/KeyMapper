package io.github.sds100.keymapper.mappings.keymaps

import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.addRepeatingJob
import androidx.navigation.fragment.findNavController
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.home.HomeViewModel
import io.github.sds100.keymapper.databinding.FragmentSimpleRecyclerviewBinding
import io.github.sds100.keymapper.home.HomeFragmentDirections
import io.github.sds100.keymapper.keymap
import io.github.sds100.keymapper.util.ui.ChipUi
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.ui.OnChipClickCallback
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 22/02/2020.
 */
class KeyMapListFragment : SimpleRecyclerViewFragment<KeyMapListItem>() {

    private val homeViewModel: HomeViewModel by activityViewModels {
        Inject.homeViewModel(requireContext())
    }

    private val viewModel: KeyMapListViewModel
        get() = homeViewModel.keymapListViewModel

    override val listItems: Flow<ListUiState<KeyMapListItem>>
        get() = viewModel.state

    override fun subscribeUi(binding: FragmentSimpleRecyclerviewBinding) {

        viewLifecycleOwner.addRepeatingJob(Lifecycle.State.RESUMED) {
            viewModel.launchConfigKeymap.collectLatest {
                val direction = HomeFragmentDirections.actionToConfigKeymap(it)
                findNavController().navigate(direction)
            }
        }
    }

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<KeyMapListItem>
    ) {
        recyclerView.withModels {
            listItems.forEach { listItem ->
                keymap {
                    id(listItem.keyMapUiState.uid)
                    keyMapUiState(listItem.keyMapUiState)

                    selectionState(listItem.selectionUiState)

                    onTriggerErrorClick(object : OnChipClickCallback {
                        override fun onChipClick(chipModel: ChipUi) {
                           viewModel.onTriggerErrorChipClick(chipModel)
                        }
                    })

                    onActionChipClick(object : OnChipClickCallback {
                        override fun onChipClick(chipModel: ChipUi) {
                            viewModel.onActionChipClick(chipModel)
                        }
                    })

                    onConstraintChipClick(object : OnChipClickCallback {
                        override fun onChipClick(chipModel: ChipUi) {
                            viewModel.onConstraintsChipClick(chipModel)
                        }
                    })

                    onCardClick { _ ->
                        viewModel.onKeymapCardClick(listItem.keyMapUiState.uid)
                    }

                    onCardLongClick { _ ->
                        viewModel.onKeymapCardLongClick(listItem.keyMapUiState.uid)
                        true
                    }
                }
            }
        }
    }
}