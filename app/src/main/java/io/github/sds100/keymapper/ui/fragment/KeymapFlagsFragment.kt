package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentKeymapFlagsBinding
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.str
import splitties.bitflags.hasFlag

/**
 * Created by sds100 on 19/03/2020.
 */
class KeymapFlagsFragment(private val mKeymapId: Long) : Fragment() {

    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), mKeymapId)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentKeymapFlagsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner

            subscribeFlagList()

            return this.root
        }
    }

    private fun FragmentKeymapFlagsBinding.subscribeFlagList() {
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
                            mViewModel.toggleFlag(flagId)
                        }
                    }
                }
            }
        }
    }
}