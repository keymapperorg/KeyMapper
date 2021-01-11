package io.github.sds100.keymapper.ui.fragment.fingerprint

import android.content.Context
import androidx.fragment.app.FragmentActivity
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.options.FingerprintMapOptions
import io.github.sds100.keymapper.data.viewmodel.BaseOptionsViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigFingerprintMapViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.ui.adapter.OptionsController
import io.github.sds100.keymapper.ui.fragment.DefaultRecyclerViewFragment
import io.github.sds100.keymapper.util.Data
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 29/11/20.
 */
class FingerprintMapOptionsFragment : DefaultRecyclerViewFragment() {

    val optionsViewModel: BaseOptionsViewModel<FingerprintMapOptions> by lazy {
        navGraphViewModels<ConfigFingerprintMapViewModel>(R.id.nav_config_fingerprint_map) {
            InjectorUtils.provideConfigFingerprintMapViewModel(requireContext())
        }.value.optionsViewModel
    }

    override var isInPagerAdapter = true
    override var isAppBarVisible = false

    private val controller by lazy {
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
            epoxyRecyclerView.adapter = controller.adapter

            optionsViewModel.checkBoxModels.observe(viewLifecycleOwner, {
                state = Data(Unit)
                controller.checkBoxModels = it
            })

            optionsViewModel.sliderModels.observe(viewLifecycleOwner, {
                state = Data(Unit)
                controller.sliderModels = it
            })
        }
    }
}