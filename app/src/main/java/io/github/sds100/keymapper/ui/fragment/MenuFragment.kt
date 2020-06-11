package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.viewmodel.KeymapListViewModel
import io.github.sds100.keymapper.databinding.FragmentMenuBinding
import io.github.sds100.keymapper.util.*
import splitties.alertdialog.appcompat.*

class MenuFragment : BottomSheetDialogFragment() {

    private val mViewModel: KeymapListViewModel by viewModels {
        InjectorUtils.provideKeymapListViewModel(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentMenuBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner

            setChangeKeyboard {
                KeyboardUtils.showInputMethodPicker()
                dismiss()
            }

            setSendFeedback {
                requireActivity().alertDialog {
                    messageResource = R.string.dialog_message_view_faq_and_use_discord_over_email

                    positiveButton(R.string.pos_help_page) {
                        dismiss()
                        findNavController().navigate(MenuFragmentDirections.actionGlobalHelpFragment())
                    }

                    negativeButton(R.string.neutral_discord) {
                        dismiss()
                        requireContext().openUrl(str(R.string.url_discord_server_invite))
                    }

                    neutralButton(R.string.neg_email) {
                        dismiss()
                        FeedbackUtils.emailDeveloper(requireContext())
                    }

                    show()
                }
            }

            setOpenSettings {
                findNavController().navigate(R.id.action_global_settingsFragment)
                dismiss()
            }

            setOpenAbout {
                dismiss()
                findNavController().navigate(R.id.action_global_aboutFragment)
            }

            setEnableAll {
                mViewModel.enableAll()
                dismiss()
            }

            setDisableAll {
                mViewModel.disableAll()
                dismiss()
            }

            return this.root
        }
    }
}