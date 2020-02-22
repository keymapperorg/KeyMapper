package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.ChooseActionSharedViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentConfigKeymapBinding
import io.github.sds100.keymapper.util.InjectorUtils
import splitties.toast.toast

/**
 * Created by sds100 on 19/02/2020.
 */
open class ConfigKeymapFragment : Fragment() {
    private val args by navArgs<ConfigKeymapFragmentArgs>()
    private val mChooseActionSharedViewModel: ChooseActionSharedViewModel by activityViewModels()

    open val configKeymapViewModel: ConfigKeymapViewModel by viewModels {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), args.keymapId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentConfigKeymapBinding>(
                inflater,
                R.layout.fragment_config_keymap,
                container,
                false
        )

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = configKeymapViewModel

            appBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }

            appBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_save -> {
                        configKeymapViewModel.saveKeymap()
                        findNavController().navigateUp()
                        true
                    }

                    else -> false
                }
            }

            setOnAddActionClick {
                val direction = ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToChooseActionFragment()
                findNavController().navigate(direction)
            }
        }

        mChooseActionSharedViewModel.chosenAction.observe(viewLifecycleOwner) {
            it?.let {
                //this will be replaced eventually
                toast("Chose action ${it.data}")

                // don't want the action to be repeatedly selected when this fragment is navigated to.
                mChooseActionSharedViewModel.chosenAction.value = null
            }
        }

        return binding.root
    }
}