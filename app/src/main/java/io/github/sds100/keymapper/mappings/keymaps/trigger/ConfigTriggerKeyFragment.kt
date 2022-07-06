package io.github.sds100.keymapper.mappings.keymaps.trigger

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.databinding.FragmentOptionsBinding
import io.github.sds100.keymapper.mappings.OptionsBottomSheetFragment
import io.github.sds100.keymapper.mappings.keymaps.ConfigKeyMapViewModel
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 12/04/2021.
 */
class ConfigTriggerKeyFragment : OptionsBottomSheetFragment<FragmentOptionsBinding>() {

    private val configKeyMapViewModel: ConfigKeyMapViewModel by navGraphViewModels(R.id.nav_config_keymap)

    override val viewModel: ConfigTriggerKeyViewModel
        get() = configKeyMapViewModel.configTriggerKeyViewModel

    override val helpUrl: String
        get() = str(R.string.url_trigger_key_options_guide)

    override fun bind(inflater: LayoutInflater, container: ViewGroup?): FragmentOptionsBinding {
        return FragmentOptionsBinding.inflate(inflater, container, false)
    }

    override fun getRecyclerView(binding: FragmentOptionsBinding) = binding.epoxyRecyclerView
    override fun getProgressBar(binding: FragmentOptionsBinding) = binding.progressBar
    override fun getDoneButton(binding: FragmentOptionsBinding) = binding.buttonDone
    override fun getHelpButton(binding: FragmentOptionsBinding) = binding.buttonHelp
}