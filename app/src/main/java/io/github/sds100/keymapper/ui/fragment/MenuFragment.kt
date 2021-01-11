package io.github.sds100.keymapper.ui.fragment

import android.content.*
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.viewmodel.BackupRestoreViewModel
import io.github.sds100.keymapper.data.viewmodel.MenuFragmentViewModel
import io.github.sds100.keymapper.databinding.FragmentMenuBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.*
import splitties.alertdialog.appcompat.*

class MenuFragment : BottomSheetDialogFragment(),
    SharedPreferences.OnSharedPreferenceChangeListener {

    private val viewModel: MenuFragmentViewModel by activityViewModels {
        InjectorUtils.provideMenuFragmentViewModel(requireContext())
    }

    private val backupRestoreViewModel: BackupRestoreViewModel by activityViewModels {
        InjectorUtils.provideBackupRestoreViewModel(requireContext())
    }

    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                MyAccessibilityService.ACTION_ON_START -> {
                    viewModel.accessibilityServiceEnabled.value = true
                }

                MyAccessibilityService.ACTION_ON_STOP -> {
                    viewModel.accessibilityServiceEnabled.value = false
                }
            }
        }
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentMenuBinding? = null
    val binding: FragmentMenuBinding
        get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IntentFilter().apply {
            addAction(MyAccessibilityService.ACTION_ON_START)
            addAction(MyAccessibilityService.ACTION_ON_STOP)

            requireActivity().registerReceiver(broadcastReceiver, this)
        }

        requireContext().defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        FragmentMenuBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = requireDialog() as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED

        binding.viewModel = viewModel

        viewModel.apply {
            keymapsPaused.value = AppPreferences.keymapsPaused
            accessibilityServiceEnabled.value = AccessibilityUtils.isServiceEnabled(requireContext())

            eventStream.observe(viewLifecycleOwner, {
                when (it) {
                    is ChooseKeyboard -> {
                        KeyboardUtils.showInputMethodPicker()
                        dismiss()
                    }

                    is SendFeedback -> this@MenuFragment.sendFeedback()
                    is OpenSettings -> {
                        findNavController().navigate(R.id.action_global_settingsFragment)
                        dismiss()
                    }

                    is OpenAbout -> {
                        findNavController().navigate(R.id.action_global_aboutFragment)
                        dismiss()
                    }

                    is PauseKeymaps -> AppPreferences.keymapsPaused = true
                    is ResumeKeymaps -> AppPreferences.keymapsPaused = false

                    is EnableAccessibilityService ->
                        AccessibilityUtils.enableService(requireContext())

                    is RequestRestore -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        backupRestoreViewModel.requestRestore()
                        dismiss()
                    }

                    is RequestBackupAll -> backupRestoreViewModel.requestBackupAll()
                }
            })
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        super.onDestroy()

        requireContext().unregisterReceiver(broadcastReceiver)
        requireContext().defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == str(R.string.key_pref_keymaps_paused)) {
            viewModel.keymapsPaused.value = AppPreferences.keymapsPaused
        }
    }

    private fun sendFeedback() {
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
}