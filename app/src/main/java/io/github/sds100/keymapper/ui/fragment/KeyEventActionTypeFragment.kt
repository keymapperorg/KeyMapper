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
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.viewmodel.KeyEventActionTypeViewModel
import io.github.sds100.keymapper.databinding.FragmentKeyeventActionTypeBinding
import io.github.sds100.keymapper.util.EventObserver
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.InputDeviceUtils
import io.github.sds100.keymapper.util.result.getFullMessage
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 30/03/2020.
 */

class KeyEventActionTypeFragment : Fragment() {
    companion object {
        const val REQUEST_KEY = "request_key_event"
        const val EXTRA_KEYCODE = "extra_keycode"
        const val EXTRA_META_STATE = "extra_meta_state"
    }

    private val mViewModel: KeyEventActionTypeViewModel by activityViewModels {
        InjectorUtils.provideKeyEventActionTypeViewModel(requireContext())
    }

    private val mDeviceListAdapter by lazy {
        ArrayAdapter(
            requireContext(),
            R.layout.dropdown_menu_popup_item,
            mutableListOf(str(R.string.from_no_device))
        )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentKeyeventActionTypeBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner

            viewModel = mViewModel

            setOnDoneClick {
                setFragmentResult(REQUEST_KEY,
                    bundleOf(
                        EXTRA_KEYCODE to mViewModel.keyCode.value?.toInt(),
                        EXTRA_META_STATE to mViewModel.metaState.value)
                )

                findNavController().navigateUp()
            }

            mViewModel.failure.observe(viewLifecycleOwner, {
                textInputLayoutKeyCode.error = it?.getFullMessage(requireContext())
            })

            mViewModel.chooseKeycode.observe(viewLifecycleOwner, EventObserver {
                val direction = ChooseActionFragmentDirections.actionChooseActionFragmentToKeycodeListFragment()
                findNavController().navigate(direction)
            })

            mViewModel.modifierKeyModels.observe(viewLifecycleOwner, { models ->
                epoxyRecyclerViewModifiers.withModels {
                    models.forEach {
                        checkbox {
                            id(it.id)
                            primaryText(str(it.label))
                            isSelected(it.isChecked)

                            onClick { view ->
                                mViewModel.setModifierKey(it.id.toInt(), (view as CheckBox).isChecked)
                            }
                        }
                    }
                }
            })

            mViewModel.chosenDevice.observe(viewLifecycleOwner, {
                val text = when {
                    it == null -> str(R.string.from_no_device)
                    AppPreferences.showDeviceDescriptors -> "${it.name} ${it.descriptor.substring(0..4)}"
                    else -> it.name
                }

                dropdownDeviceId.setText(text, false)
            })

            mViewModel.buildDeviceInfoModels.observe(viewLifecycleOwner, {
                mViewModel.setDeviceInfoModels(InputDeviceUtils.createDeviceInfoModelsForAll())
            })

            mViewModel.deviceInfoModels.observe(viewLifecycleOwner, { models ->
                mDeviceListAdapter.apply {
                    clear()
                    add(str(R.string.from_no_device))

                    models.forEach {
                        if (AppPreferences.showDeviceDescriptors) {
                            add("${it.name} ${it.descriptor.substring(0..4)}")
                        } else {
                            add(it.name)
                        }
                    }
                }
            })

            dropdownDeviceId.apply {
                setAdapter(mDeviceListAdapter)
                //set the default value
                setText(str(R.string.from_no_device), false)

                onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        mViewModel.chooseDevice(position)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        mViewModel.chooseNoDevice()
                    }
                }
            }

            return this.root
        }
    }
}