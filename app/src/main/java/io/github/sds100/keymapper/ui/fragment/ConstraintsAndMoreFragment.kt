package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentConstraintsAndMoreBinding
import io.github.sds100.keymapper.util.toggleFlag
import splitties.bitflags.hasFlag
import splitties.resources.str

/**
 * Created by sds100 on 19/03/2020.
 */
class ConstraintsAndMoreFragment : Fragment() {
    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentConstraintsAndMoreBinding>(
            inflater,
            R.layout.fragment_constraints_and_more,
            container,
            false
        )

        binding.apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            mViewModel.flags.observe(viewLifecycleOwner) { flags ->
                epoxyRecyclerViewFlags.withModels {
                    KeyMap.KEYMAP_FLAG_LABEL_MAP.keys.forEach { flagId ->
                        checkbox {
                            id(flagId)

                            val labelResId = KeyMap.KEYMAP_FLAG_LABEL_MAP[flagId]

                            if (labelResId != null) {
                                primaryText(str(labelResId))
                            }

                            isSelected(flags.hasFlag(flagId))

                            onClick { _ ->
                                mViewModel.flags.value = flags.toggleFlag(flagId)
                            }
                        }
                    }
                }
            }
        }

        return binding.root
    }
}