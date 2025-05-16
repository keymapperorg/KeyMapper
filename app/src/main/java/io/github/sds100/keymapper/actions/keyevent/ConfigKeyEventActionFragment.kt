package io.github.sds100.keymapper.actions.keyevent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.addCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResult
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentConfigKeyEventBinding
import io.github.sds100.keymapper.ui.utils.putJsonSerializable
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.str
import io.github.sds100.keymapper.util.ui.configuredCheckBox
import io.github.sds100.keymapper.util.ui.setupNavigation
import kotlinx.coroutines.flow.collectLatest
import kotlinx.serialization.json.Json

class ConfigKeyEventActionFragment : Fragment() {
    companion object {
        const val EXTRA_RESULT = "extra_result"
    }

    private val args: ConfigKeyEventActionFragmentArgs by navArgs()

    private val requestKey: String by lazy { args.requestKey }

    private val viewModel: ConfigKeyEventActionViewModel by viewModels {
        Inject.configKeyEventViewModel(requireContext())
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentConfigKeyEventBinding? = null
    val binding: FragmentConfigKeyEventBinding
        get() = _binding!!

    private val deviceArrayAdapter: ArrayAdapter<String> by lazy {
        ArrayAdapter<String>(
            requireContext(),
            R.layout.dropdown_menu_popup_item,
            mutableListOf(),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel.setupNavigation(this)

        if (args.keyEventAction != null) {
            viewModel.loadAction(Json.decodeFromString(args.keyEventAction!!))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        FragmentConfigKeyEventBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
            val insets =
                insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())
            v.updatePadding(insets.left, insets.top, insets.right, insets.bottom)
            WindowInsetsCompat.CONSUMED
        }

        binding.viewModel = viewModel

        binding.setOnChooseKeyCodeClick {
            viewModel.onChooseKeyCodeClick()
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        binding.appBar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.dropdownDeviceId.apply {
            setAdapter(deviceArrayAdapter)

            // set the default value
            setText(str(R.string.from_no_device), false)

            setOnItemClickListener { _, _, position, _ ->
                if (position == 0) {
                    viewModel.chooseNoDevice()
                    return@setOnItemClickListener
                }

                // subtract the list item that selects no device
                viewModel.chooseDevice(position - 1)
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long,
                ) {
                    if (position == 0) {
                        viewModel.chooseNoDevice()
                        return
                    }

                    // subtract the list item that selects no device
                    viewModel.chooseDevice(position - 1)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    viewModel.chooseNoDevice()
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest {
                setFragmentResult(
                    requestKey,
                    Bundle().apply { putJsonSerializable(EXTRA_RESULT, it) },
                )

                findNavController().navigateUp()
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.uiState.collect { state ->
                binding.epoxyRecyclerViewModifiers.withModels {
                    state.modifierListItems.forEach { listItem ->
                        configuredCheckBox(listItem) { isChecked ->
                            viewModel.setModifierKeyChecked(listItem.id.toInt(), isChecked)
                        }
                    }
                }

                deviceArrayAdapter.apply {
                    clear()
                    add(str(R.string.from_no_device))
                    for (device in state.deviceListItems) {
                        add(device.name)
                    }
                    notifyDataSetChanged()
                }

                // Filtering must be false so that the dropdown items aren't cleared
                // when setting text.
                binding.dropdownDeviceId.setText(state.chosenDeviceName, false)

                binding.textInputLayoutKeyCode.error = state.keyCodeErrorMessage
            }
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
