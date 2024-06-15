package io.github.sds100.keymapper.mappings.keymaps

import androidx.fragment.app.activityViewModels
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.home.HomeViewModel
import io.github.sds100.keymapper.keymap
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.ui.ChipUi
import io.github.sds100.keymapper.util.ui.OnChipClickCallback
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 22/02/2020.
 */
class KeyMapListFragment : SimpleRecyclerViewFragment<KeyMapListItem>() {

    private val homeViewModel: HomeViewModel by activityViewModels {
        Inject.homeViewModel(requireContext())
    }

    private val viewModel: KeyMapListViewModel
        get() = homeViewModel.keymapListViewModel

    override val listItems: Flow<State<List<KeyMapListItem>>>
        get() = viewModel.state

    override val emptyListPlaceholder: Int = R.string.keymap_recyclerview_placeholder

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<KeyMapListItem>,
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
