package io.github.sds100.keymapper.system.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.databinding.FragmentChooseAppBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.RecyclerViewFragment
import io.github.sds100.keymapper.util.ui.RecyclerViewUtils
import io.github.sds100.keymapper.util.ui.SimpleListItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map

/**
 * Created by sds100 on 22/02/2020.
 */
class ChooseAppFragment : RecyclerViewFragment<SimpleListItem, FragmentChooseAppBinding>() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val SEARCH_STATE_KEY = "key_app_search_state"
    }

    private val args: ChooseAppFragmentArgs by navArgs()

    override var searchStateKey: String? = SEARCH_STATE_KEY

    private val viewModel: ChooseAppViewModel by viewModels {
        Inject.chooseAppViewModel(requireContext())
    }

    override val listItems: Flow<State<List<SimpleListItem>>>
        get() = viewModel.state.map { it.listItems }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.allowHiddenApps = args.allowHiddenApps
    }

    override fun subscribeUi(binding: FragmentChooseAppBinding) {
        binding.viewModel = viewModel

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest {
                returnResult(EXTRA_PACKAGE_NAME to it)
            }
        }

        RecyclerViewUtils.applySimpleListItemDecorations(binding.epoxyRecyclerView)
    }

    override fun onSearchQuery(query: String?) {
        viewModel.searchQuery.value = query
    }

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentChooseAppBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<SimpleListItem>) {
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

    override fun getRequestKey(): String = args.requestKey

    override fun getBottomAppBar(binding: FragmentChooseAppBinding) = binding.appBar
    override fun getRecyclerView(binding: FragmentChooseAppBinding) = binding.epoxyRecyclerView
    override fun getProgressBar(binding: FragmentChooseAppBinding) = binding.progressBar
    override fun getEmptyListPlaceHolderTextView(binding: FragmentChooseAppBinding) =
        binding.emptyListPlaceHolder
}
