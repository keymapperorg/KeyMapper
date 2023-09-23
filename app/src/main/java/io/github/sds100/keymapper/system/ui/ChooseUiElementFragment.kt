package io.github.sds100.keymapper.system.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.databinding.FragmentChooseUiElementBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.RecyclerViewFragment
import io.github.sds100.keymapper.util.ui.RecyclerViewUtils
import io.github.sds100.keymapper.util.ui.SimpleListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ChooseUiElementFragment : RecyclerViewFragment<UiElementInfoListItem, FragmentChooseUiElementBinding>() {
    companion object {
        const val EXTRA_UI_ELEMENT_ID = "extra_ui_element_id"
        const val SEARCH_STATE_KEY = "key_ui_element_search_state"
    }

    private val args: ChooseUiElementFragmentArgs by navArgs()

    override var searchStateKey: String? = SEARCH_STATE_KEY

    private val viewModel: ChooseUiElementViewModel by viewModels {
        Inject.chooseUiElementViewModel(requireContext())
    }

    override val listItems: Flow<State<List<UiElementInfoListItem>>>
        get() = viewModel.state.map { it.listItems }

    override fun subscribeUi(binding: FragmentChooseUiElementBinding) {
        binding.viewModel = viewModel

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest {
                returnResult(EXTRA_UI_ELEMENT_ID to Json.encodeToString(it))
            }
        }

        RecyclerViewUtils.applySimpleListItemDecorations(binding.epoxyRecyclerView)
    }

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentChooseUiElementBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<UiElementInfoListItem>) {
        binding.epoxyRecyclerView.withModels {
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

    override fun getRequestKey(): String {
        return args.requestKey
    }

    override fun getBottomAppBar(binding: FragmentChooseUiElementBinding) = binding.appBar
    override fun getRecyclerView(binding: FragmentChooseUiElementBinding) = binding.epoxyRecyclerView
    override fun getProgressBar(binding: FragmentChooseUiElementBinding) = binding.progressBar
    override fun getEmptyListPlaceHolderTextView(binding: FragmentChooseUiElementBinding) = binding.emptyListPlaceHolder

    override fun onDestroy() {
        super.onDestroy()

        viewModel.stopRecording()
    }

    override fun onBackPressed() {
        super.onBackPressed()

        viewModel.stopRecording()
    }
}