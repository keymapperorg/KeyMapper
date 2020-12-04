package io.github.sds100.keymapper.ui.fragment

import android.content.*
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
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

class MenuFragment : BottomSheetDialogFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val mViewModel: MenuFragmentViewModel by activityViewModels {
        InjectorUtils.provideMenuFragmentViewModel(requireContext())
    }

    private val mBackupRestoreViewModel: BackupRestoreViewModel by activityViewModels {
        InjectorUtils.provideBackupRestoreViewModel(requireContext())
    }

    private val mKeymapsPaused = MutableLiveData(AppPreferences.keymapsPaused)
    private val mAccessibilityServiceEnabled by lazy {
        MutableLiveData(AccessibilityUtils.isServiceEnabled(requireContext()))
    }

    private val mBroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {
                MyAccessibilityService.ACTION_ON_START -> {
                    mAccessibilityServiceEnabled.value = true
                }

                MyAccessibilityService.ACTION_ON_STOP -> {
                    mAccessibilityServiceEnabled.value = false
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IntentFilter().apply {
            addAction(MyAccessibilityService.ACTION_ON_START)
            addAction(MyAccessibilityService.ACTION_ON_STOP)

            requireActivity().registerReceiver(mBroadcastReceiver, this)
        }

        requireContext().defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentMenuBinding.inflate(inflater, container, false).apply {

            lifecycleOwner = viewLifecycleOwner

            keymapsPaused = mKeymapsPaused
            accessibilityServiceEnabled = mAccessibilityServiceEnabled

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

            setToggleKeymaps {
                AppPreferences.keymapsPaused = !AppPreferences.keymapsPaused
            }

            setEnableAccessibilityService {
                AccessibilityUtils.enableService(requireContext())
            }

            setRestore {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    mBackupRestoreViewModel.requestRestore()
                    dismiss()
                }
            }

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val dialog = requireDialog() as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onDestroy() {
        super.onDestroy()

        requireContext().unregisterReceiver(mBroadcastReceiver)
        requireContext().defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == str(R.string.key_pref_keymaps_paused)) {
            mKeymapsPaused.value = AppPreferences.keymapsPaused
        }
    }
}