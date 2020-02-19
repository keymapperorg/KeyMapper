package io.github.sds100.keymapper.ui.fragment

import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.example.architecturetest.data.viewmodel.NewKeymapViewModel
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 19/02/2020.
 */
class NewKeymapFragment : ConfigKeymapFragment() {
    override val configKeymapViewModel: NewKeymapViewModel by viewModels {
        InjectorUtils.provideNewKeymapViewModel(requireContext())
    }
}