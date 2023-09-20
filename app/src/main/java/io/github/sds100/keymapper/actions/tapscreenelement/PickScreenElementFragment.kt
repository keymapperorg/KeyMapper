package io.github.sds100.keymapper.actions.tapscreenelement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.SearchView
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentPickScreenElementBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.simpleGrid
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.dataOrNull
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.ui.DefaultSimpleListItem
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import timber.log.Timber

class PickScreenElementFragment : Fragment() {
    companion object {
        const val EXTRA_ELEMENT_ID = "extra_element_id"
    }

    private val args: PickScreenElementFragmentArgs by navArgs()
    private val requestKey: String by lazy { args.requestKey }
    private var interactionTypesDisplayValues = mutableListOf<String>()

    private val viewModel: PickScreenElementViewModel by viewModels {
        Inject.pickScreenElementActionTypeViewModel(requireContext())
    }

    private var _binding: FragmentPickScreenElementBinding? = null
    val binding: FragmentPickScreenElementBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentPickScreenElementBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this
            return this.root
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        interactionTypesDisplayValues = arrayOf(
            str(R.string.extra_label_pick_screen_element_interaction_type_click),
            str(R.string.extra_label_pick_screen_element_interaction_type_long_click)
        ).toMutableList();
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.caption = str(R.string.header_pick_screen_element)
        binding.emptyListPlaceholder = str(R.string.recyclerview_placeholder)
        binding.interactionTypeSpinnerAdapter = ArrayAdapter(
            this.requireActivity(),
            android.R.layout.simple_spinner_dropdown_item,
            interactionTypesDisplayValues
        )

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Timber.d("onQueryTextSubmit: %s", query)
                viewModel.listItemsSearchQuery.value = query
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Timber.d("onQueryTextChange: %s", newText)
                viewModel.listItemsSearchQuery.value = newText
                return true
            }

        })

        viewModel.showPopups(this, binding)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        binding.appBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest { result ->
                setFragmentResult(
                    requestKey,
                    bundleOf(EXTRA_ELEMENT_ID to Json.encodeToString(result))
                )
                findNavController().navigateUp()
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            binding.epoxyRecyclerView.withModels {
                viewModel.listItems.value.dataOrNull()?.forEach { listItem ->
                    if (listItem is DefaultSimpleListItem) {
                        if (spanCount == 1) {
                            simple {
                                id(listItem.id)
                                model(listItem)

                                onClickListener { _ ->
                                    viewModel.onListItemClick(listItem.title, listItem.subtitle!!)
                                }
                            }
                        } else {
                            simpleGrid {
                                id(listItem.id)
                                model(listItem)

                                onClickListener { _ ->
                                    viewModel.onListItemClick(listItem.title, listItem.subtitle!!)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}