package io.github.sds100.keymapper.ui.fragment.fingerprint

import android.content.Context
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.options.FingerprintMapOptions
import io.github.sds100.keymapper.data.model.options.OptionsListModel
import io.github.sds100.keymapper.data.viewmodel.BaseOptionsViewModel
import io.github.sds100.keymapper.data.viewmodel.ConfigFingerprintMapViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.ui.adapter.OptionsController
import io.github.sds100.keymapper.ui.fragment.DefaultRecyclerViewFragment
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.delegate.IModelState

/**
 * Created by sds100 on 29/11/20.
 */
class FingerprintMapOptionsFragment : DefaultRecyclerViewFragment<OptionsListModel>() {

    class Info : FragmentInfo(
        R.string.option_list_header,
        R.string.url_fingerprint_map_options_guide,
        { FingerprintMapOptionsFragment() }
    )

    val optionsViewModel: BaseOptionsViewModel<FingerprintMapOptions> by lazy {
        navGraphViewModels<ConfigFingerprintMapViewModel>(R.id.nav_config_fingerprint_map) {
            InjectorUtils.provideConfigFingerprintMapViewModel(requireContext())
        }.value.optionsViewModel
    }

    override val modelState: IModelState<OptionsListModel>
        get() = optionsViewModel

    override var isAppBarVisible = false

    private val controller by lazy {
        object : OptionsController(viewLifecycleOwner) {
            override val ctx: Context
                get() = requireContext()

            override val viewModel: BaseOptionsViewModel<*>
                get() = optionsViewModel
        }
    }

    override fun subscribeUi(binding: FragmentRecyclerviewBinding) {
        super.subscribeUi(binding)

        binding.epoxyRecyclerView.adapter = controller.adapter
    }

    override fun populateList(
        binding: FragmentRecyclerviewBinding,
        model: OptionsListModel?
    ) {
        controller.optionsListModel = model ?: OptionsListModel.EMPTY
    }
}