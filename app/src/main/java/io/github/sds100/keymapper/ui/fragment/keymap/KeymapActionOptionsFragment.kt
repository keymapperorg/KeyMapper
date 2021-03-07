package io.github.sds100.keymapper.ui.fragment.keymap

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyControllerAdapter
import io.github.sds100.keymapper.data.model.options.KeymapActionOptions
import io.github.sds100.keymapper.data.viewmodel.KeymapActionOptionsViewModel
import io.github.sds100.keymapper.databinding.FragmentKeymapActionOptionsBinding
import io.github.sds100.keymapper.ui.fragment.BaseOptionsDialogFragment
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 27/06/2020.
 */
class KeymapActionOptionsFragment : BaseOptionsDialogFragment<FragmentKeymapActionOptionsBinding, KeymapActionOptions>() {

    companion object {
        const val REQUEST_KEY = "request_choose_action_options"
    }

    override val optionsViewModel: KeymapActionOptionsViewModel by viewModels {
        InjectorUtils.provideKeymapActionOptionsViewModel()
    }

    override val requestKey = REQUEST_KEY

    override val initialOptions: KeymapActionOptions
        get() = navArgs<KeymapActionOptionsFragmentArgs>().value.StringNavArgKeymapActionOptions

    override fun subscribeCustomUi(binding: FragmentKeymapActionOptionsBinding) {
        binding.apply {
            viewModel = optionsViewModel
        }
    }

    override fun setRecyclerViewAdapter(binding: FragmentKeymapActionOptionsBinding, adapter: EpoxyControllerAdapter) {
        binding.epoxyRecyclerView.adapter = adapter
    }

    override fun bind(inflater: LayoutInflater, container: ViewGroup?): FragmentKeymapActionOptionsBinding {
        return FragmentKeymapActionOptionsBinding.inflate(inflater, container, false)
    }
}