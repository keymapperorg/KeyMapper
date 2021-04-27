package io.github.sds100.keymapper.system.keyevents

import androidx.fragment.app.activityViewModels
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.ui.ListUiState
import io.github.sds100.keymapper.util.ui.SimpleRecyclerViewFragment
import io.github.sds100.keymapper.util.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Created by sds100 on 30/03/2020.
 */

class KeyCodeListFragment : SimpleRecyclerViewFragment<KeyCodeListItem>() {
    companion object {
        const val REQUEST_KEY = "request_keycode"
        const val EXTRA_KEYCODE = "extra_keycode"
        const val SEARCH_STATE_KEY = "key_keycode_search_state"
    }

    override var searchStateKey: String? = SEARCH_STATE_KEY
    override var requestKey: String? = REQUEST_KEY

    private val viewModel: ChooseKeyCodeViewModel by activityViewModels {
        Inject.chooseKeyCodeViewModel()
    }

    override val listItems: Flow<ListUiState<KeyCodeListItem>>
        get() = viewModel.state

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<KeyCodeListItem>) {
        recyclerView.withModels {
            listItems.forEach {
                simple {
                    id(it.keyCode)
                    primaryText(it.label)

                    onClick { _ ->
                        returnResult(EXTRA_KEYCODE to it.keyCode)
                    }
                }
            }
        }
    }

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }
}