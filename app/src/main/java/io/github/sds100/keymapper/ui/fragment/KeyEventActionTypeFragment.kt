package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.CheckBox
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.data.showDeviceDescriptors
import io.github.sds100.keymapper.data.viewmodel.KeyEventActionTypeViewModel
import io.github.sds100.keymapper.databinding.FragmentKeyeventActionTypeBinding
import io.github.sds100.keymapper.globalPreferences
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.getFullMessage

/**
 * Created by sds100 on 30/03/2020.
 */

class KeyEventActionTypeFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_key_event"
        const val EXTRA_KEYCODE = "extra_keycode"
        const val EXTRA_META_STATE = "extra_meta_state"
        const val EXTRA_USE_SHELL = "extra_use_shell"
        const val EXTRA_DEVICE_DESCRIPTOR = "extra_device_descriptor"
    }

    private val viewModel: KeyEventActionTypeViewModel by activityViewModels {
        InjectorUtils.provideKeyEventActionTypeViewModel(requireContext())
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentKeyeventActionTypeBinding? = null
    val binding: FragmentKeyeventActionTypeBinding
        get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentKeyeventActionTypeBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel

        binding.setOnDoneClick {
            setFragmentResult(REQUEST_KEY,
                bundleOf(
                    EXTRA_KEYCODE to viewModel.keyCode.value?.toInt(),
                    EXTRA_META_STATE to viewModel.metaState.value,
                    EXTRA_USE_SHELL to viewModel.useShell.value
                ).apply {
                    viewModel.chosenDevice.value?.let {
                        putString(EXTRA_DEVICE_DESCRIPTOR, it.descriptor)
                    }
                }
            )

            findNavController().navigateUp()
        }

        viewModel.failure.observe(viewLifecycleOwner, {
            binding.textInputLayoutKeyCode.error = it?.getFullMessage(requireContext())
        })

        viewModel.modifierKeyModels.observe(viewLifecycleOwner, { models ->
            viewLifecycleScope.launchWhenResumed {
                binding.epoxyRecyclerViewModifiers.withModels {
                    models.forEach {
                        checkbox {
                            id(it.id)
                            primaryText(str(it.label))
                            isSelected(it.isChecked)

                            onClick { view ->
                                viewModel.setModifierKey(it.id.toInt(), (view as CheckBox).isChecked)
                            }
                        }
                    }
                }
            }
        })

        viewModel.chosenDevice.observe(viewLifecycleOwner, {
            val text = when {
                it == null -> str(R.string.from_no_device)

                globalPreferences.showDeviceDescriptors.firstBlocking() ->
                    "${it.name} ${it.descriptor.substring(0..4)}"

                else -> it.name
            }

            binding.dropdownDeviceId.setText(text, false)
        })

        viewModel.eventStream.observe(viewLifecycleOwner, {
            when (it) {
                is ChooseKeycode -> {
                    val direction = ChooseActionFragmentDirections
                        .actionChooseActionFragmentToKeycodeListFragment()

                    findNavController().navigate(direction)
                }

                is BuildDeviceInfoModels -> {
                    viewLifecycleScope.launchWhenResumed {
                        val modelList = InputDeviceUtils.createDeviceInfoModelsForAll()
                        viewModel.setDeviceInfoModels(modelList)
                    }
                }
            }
        })

        viewModel.deviceInfoModels.observe(viewLifecycleOwner, { models ->
            viewLifecycleScope.launchWhenResumed {
                ArrayAdapter<String>(
                    requireContext(),
                    R.layout.dropdown_menu_popup_item,
                    mutableListOf()
                ).apply {
                    clear()
                    add(str(R.string.from_no_device))

                    models.forEach {
                        if (globalPreferences.showDeviceDescriptors.firstBlocking()) {
                            add("${it.name} ${it.descriptor.substring(0..4)}")
                        } else {
                            add(it.name)
                        }
                    }

                    binding.dropdownDeviceId.setAdapter(this)
                }
            }
        })

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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}