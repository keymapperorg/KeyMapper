package io.github.sds100.keymapper.actions.keyevent

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.actions.ChooseActionFragmentDirections
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.databinding.FragmentConfigKeyEventBinding
import io.github.sds100.keymapper.ui.utils.putJsonSerializable
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 30/03/2020.
 */

class ConfigKeyEventFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_key_event"
        const val EXTRA_RESULT = "extra_result"
    }

    private val viewModel: ConfigKeyEventViewModel by activityViewModels {
        Inject.configKeyEventViewModel(requireContext())
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentConfigKeyEventBinding? = null
    val binding: FragmentConfigKeyEventBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentConfigKeyEventBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel

        binding.setOnChooseKeyCodeClick {
            findNavController().navigate(ChooseActionFragmentDirections.actionChooseActionFragmentToKeycodeListFragment())
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.returnResult.collectLatest {
                setFragmentResult(
                    REQUEST_KEY,
                    Bundle().apply {
                        putJsonSerializable(EXTRA_RESULT, it)
                    }
                )

                findNavController().navigateUp()
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.uiState.collectLatest { state ->
                binding.epoxyRecyclerViewModifiers.withModels {
                    state.modifierListItems.forEach {
                        checkbox {
                            id(it.id)
                            primaryText(it.label)
                            isChecked(it.isChecked)

                            onCheckedChange { _, isChecked ->
                                viewModel.setModifierKeyChecked(it.id.toInt(), isChecked)
                            }
                        }
                    }
                }

                ArrayAdapter<String>(
                    requireContext(),
                    R.layout.dropdown_menu_popup_item,
                    mutableListOf()
                ).apply {
                    clear()
                    add(str(R.string.from_no_device))

                    state.deviceListItems.forEach {
                        add(it.name)
                    }

                    binding.dropdownDeviceId.setAdapter(this)
                }

                binding.textInputLayoutKeyCode.error = state.keyCodeErrorMessage
            }
        }

        binding.dropdownDeviceId.apply {
            //set the default value
            setText(str(R.string.from_no_device), false)

            setOnItemClickListener { _, _, position, _ ->
                if (position == 0) {
                    viewModel.chooseNoDevice()
                    return@setOnItemClickListener
                }

                //subtract the list item that selects no device
                viewModel.chooseDevice(position - 1)
            }

            onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (position == 0) {
                        viewModel.chooseNoDevice()
                        return
                    }

                    //subtract the list item that selects no device
                    viewModel.chooseDevice(position - 1)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    viewModel.chooseNoDevice()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        viewModel.rebuildUiState()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}