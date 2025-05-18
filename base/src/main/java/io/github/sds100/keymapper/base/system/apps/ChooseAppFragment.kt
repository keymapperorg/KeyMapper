package io.github.sds100.keymapper.base.system.apps

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyRecyclerView
import io.github.sds100.keymapper.common.utils.State
import io.github.sds100.keymapper.base.databinding.FragmentChooseAppBinding
import io.github.sds100.keymapper.base.simple
import io.github.sds100.keymapper.base.utils.Inject
import io.github.sds100.keymapper.base.utils.ui.launchRepeatOnLifecycle
import io.github.sds100.keymapper.base.utils.ui.RecyclerViewFragment
import io.github.sds100.keymapper.base.utils.ui.RecyclerViewUtils
import io.github.sds100.keymapper.base.utils.ui.SimpleListItemOld
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map

class ChooseAppFragment : RecyclerViewFragment<SimpleListItemOld, FragmentChooseAppBinding>() {

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val SEARCH_STATE_KEY = "key_app_search_state"
    }

    private val args: ChooseAppFragmentArgs by navArgs()

    override var searchStateKey: String? = SEARCH_STATE_KEY

    private val viewModel: ChooseAppViewModel by viewModels {
        Inject.chooseAppViewModel(requireContext())
    }

    override val listItems: Flow<State<List<SimpleListItemOld>>>
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

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<SimpleListItemOld>) {
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
