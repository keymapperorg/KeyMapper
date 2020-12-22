package io.github.sds100.keymapper.ui.fragment.keymap

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.options.TriggerOptions
import io.github.sds100.keymapper.data.viewmodel.BaseOptionsViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.ui.adapter.OptionsController
import io.github.sds100.keymapper.ui.fragment.DefaultRecyclerViewFragment
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 29/11/20.
 */
class TriggerOptionsFragment : DefaultRecyclerViewFragment() {

    val optionsViewModel: BaseOptionsViewModel<TriggerOptions> by lazy {
        navGraphViewModels<ConfigKeymapViewModel>(R.id.nav_config_keymap) {
            InjectorUtils.provideConfigKeymapViewModel(requireContext())
        }.value.triggerViewModel.optionsViewModel
    }

    override var isInPagerAdapter = true
    override var isAppBarVisible = false

    private val mController by lazy {
        object : OptionsController(this) {
            override val activity: FragmentActivity
                get() = requireActivity()

            override val ctx: Context
                get() = requireContext()

            override val viewModel: BaseOptionsViewModel<*>
                get() = optionsViewModel
        }
    }

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        binding.apply {
            epoxyRecyclerView.adapter = mController.adapter

            optionsViewModel.checkBoxModels.observe(viewLifecycleOwner, {
                state = Data(Unit)
                mController.checkBoxModels = it
            })

            optionsViewModel.sliderModels.observe(viewLifecycleOwner, {
                state = Data(Unit)
                mController.sliderModels = it
            })
        }
    }
}