package io.github.sds100.keymapper.ui.fragment.actionshortcut

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.navArgs
import com.airbnb.epoxy.EpoxyControllerAdapter
import io.github.sds100.keymapper.data.model.options.ActionShortcutOptions
import io.github.sds100.keymapper.data.viewmodel.ActionShortcutOptionsViewModel
import io.github.sds100.keymapper.databinding.FragmentActionShortcutOptionsBinding
import io.github.sds100.keymapper.ui.fragment.BaseOptionsDialogFragment

/**
 * Created by sds100 on 27/06/2020.
 */
class ActionShortcutOptionsFragment :
    BaseOptionsDialogFragment<FragmentActionShortcutOptionsBinding, ActionShortcutOptions>() {

    companion object {
        const val REQUEST_KEY = "request_choose_action_options"
    }

    override val optionsViewModel: ActionShortcutOptionsViewModel by viewModels {
        ActionShortcutOptionsViewModel.Factory()
    }

    override val requestKey = REQUEST_KEY

    override val initialOptions: ActionShortcutOptions
        get() = navArgs<ActionShortcutOptionsFragmentArgs>().value.StringNavArgKeymapActionOptions

    override fun subscribeCustomUi(binding: FragmentActionShortcutOptionsBinding) {
        binding.apply {
            viewModel = optionsViewModel
        }
    }

    override fun setRecyclerViewAdapter(
        binding: FragmentActionShortcutOptionsBinding,
        adapter: EpoxyControllerAdapter
    ) {
        binding.epoxyRecyclerView.adapter = adapter
    }

    override fun bind(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentActionShortcutOptionsBinding {
        return FragmentActionShortcutOptionsBinding.inflate(inflater, container, false)
    }
}