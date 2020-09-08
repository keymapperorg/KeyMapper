package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.CreateActionShortcutViewModel
import io.github.sds100.keymapper.databinding.FragmentActionShortcutListBinding
import io.github.sds100.keymapper.util.InjectorUtils
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.positiveButton

/**
 * Created by sds100 on 08/09/20.
 */

class CreateActionShortcutFragment : Fragment() {

    private val mViewModel by navGraphViewModels<CreateActionShortcutViewModel>(R.id.nav_action_shortcut) {
        InjectorUtils.provideCreateActionShortcutViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentActionShortcutListBinding.inflate(inflater).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                showOnBackPressedWarning()
            }

            appBar.setNavigationOnClickListener {
                showOnBackPressedWarning()
            }

            mViewModel.actionList.observe(viewLifecycleOwner, {
                appBar.menu?.findItem(R.id.action_done)?.isVisible = it.isNotEmpty()
            })

            return this.root
        }
    }

    private fun showOnBackPressedWarning() {
        requireContext().alertDialog {
            messageResource = R.string.dialog_message_are_you_sure_want_to_leave_without_saving

            positiveButton(R.string.pos_yes) {
                requireActivity().finish()
            }

            cancelButton()
            show()
        }
    }
}