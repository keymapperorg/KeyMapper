package io.github.sds100.keymapper.actions

import androidx.fragment.app.activityViewModels
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.ui.TintType
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 31/03/2020.
 */
class UnsupportedActionListFragment
    : SimpleRecyclerViewFragment<UnsupportedActionListItem>() {

    private val viewModel: UnsupportedActionListViewModel by activityViewModels {
        Inject.unsupportedActionListViewModel(requireContext())
    }

    override val listItems: Flow<ListUiState<UnsupportedActionListItem>>
        get() = viewModel.state

    override fun populateList(
        recyclerView: EpoxyRecyclerView,
        listItems: List<UnsupportedActionListItem>
    ) {
        binding.epoxyRecyclerView.withModels {
            listItems.forEach { model ->
                simple {
                    id(model.id)
                    icon(model.icon)
                    tintType(TintType.ON_SURFACE)
                    primaryText(model.description)
                    secondaryText(model.reason)
                }
            }
        }
    }
}