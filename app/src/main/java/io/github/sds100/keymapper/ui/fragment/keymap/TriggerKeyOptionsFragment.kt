package io.github.sds100.keymapper.ui.fragment.keymap

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyControllerAdapter
import io.github.sds100.keymapper.data.model.options.TriggerKeyOptions
import io.github.sds100.keymapper.data.viewmodel.TriggerKeyOptionsViewModel
import io.github.sds100.keymapper.databinding.FragmentTriggerKeyOptionsBinding
import io.github.sds100.keymapper.ui.fragment.BaseOptionsDialogFragment
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 27/06/2020.
 */
class TriggerKeyOptionsFragment
    : BaseOptionsDialogFragment<FragmentTriggerKeyOptionsBinding, TriggerKeyOptions>() {

    companion object {
        const val REQUEST_KEY = "request_trigger_key_behavior"
    }

    override val optionsViewModel: TriggerKeyOptionsViewModel by viewModels {
        InjectorUtils.provideTriggerKeyOptionsViewModel()
    }

    override val requestKey = REQUEST_KEY

    override val initialOptions: TriggerKeyOptions
        get() = navArgs<TriggerKeyOptionsFragmentArgs>().value.StringNavArgTriggerKeyOptions

    override fun subscribeCustomUi(binding: FragmentTriggerKeyOptionsBinding) {
        binding.viewModel = optionsViewModel
    }

    override fun setRecyclerViewAdapter(
        binding: FragmentTriggerKeyOptionsBinding,
        adapter: EpoxyControllerAdapter
    ) {
        binding.epoxyRecyclerView.adapter = adapter
    }

    override fun bind(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentTriggerKeyOptionsBinding.inflate(inflater, container, false)
}