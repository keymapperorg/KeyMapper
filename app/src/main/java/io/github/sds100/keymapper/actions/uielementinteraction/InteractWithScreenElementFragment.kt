package io.github.sds100.keymapper.actions.uielementinteraction

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.github.sds100.keymapper.databinding.FragmentInteractWithScreenElementBinding
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.getDynamicStringValue
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.setupNavigation
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale

class InteractWithScreenElementFragment : Fragment() {
    companion object {
        const val EXTRA_RESULT = "extra_result"
    }

    private val args: InteractWithScreenElementFragmentArgs by navArgs()
    private val requestKey: String by lazy { args.requestKey }
    private var interactionTypesDisplayValues = mutableListOf<String>()

    private val viewModel: InteractWithScreenElementViewModel by viewModels {
        Inject.interactWithScreenElementActionTypeViewModel(requireContext())
    }

    private var _binding: FragmentInteractWithScreenElementBinding? = null
    val binding: FragmentInteractWithScreenElementBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentInteractWithScreenElementBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this
            return this.root
        }
    }

    @SuppressLint("DiscouragedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setupNavigation(this)

        args.result?.let {
            viewModel.loadResult(Json.decodeFromString(it))
        }

        args.showPackageInfoOnly?.let {
            viewModel.showPackageInfoOnly = it
        }

        interactionTypesDisplayValues = InteractionType.values().map {
            val stringName = "extra_label_interact_with_screen_element_interaction_type_${
                it.name.lowercase(
                    Locale.ROOT
                )
            }"

            getDynamicStringValue(stringName)
        }.toMutableList()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.interactionTypeSpinnerAdapter = ArrayAdapter(
            this.requireActivity(),
            android.R.layout.simple_spinner_dropdown_item,
            interactionTypesDisplayValues
        )

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
                    bundleOf(EXTRA_RESULT to Json.encodeToString(result))
                )
                findNavController().navigateUp()
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}