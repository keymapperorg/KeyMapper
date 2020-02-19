package io.github.sds100.keymapper.ui.fragment

import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.architecturetest.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 19/02/2020.
 */
open class ConfigKeymapFragment : Fragment() {
    private val args by navArgs<ConfigKeymapFragmentArgs>()

    open val configKeymapViewModel: ConfigKeymapViewModel by viewModels {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), args.keymapId)
    }

}