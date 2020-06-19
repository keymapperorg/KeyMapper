package io.github.sds100.keymapper.ui.fragment

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.viewmodel.KeymapListViewModel
import io.github.sds100.keymapper.databinding.FragmentMenuBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.*
import splitties.alertdialog.appcompat.*

class MenuFragment : BottomSheetDialogFragment(), SharedPreferences.OnSharedPreferenceChangeListener {

    private val mViewModel: KeymapListViewModel by viewModels {
        InjectorUtils.provideKeymapListViewModel(requireContext())
    }

    private val mKeymapsPaused = MutableLiveData(AppPreferences.keymapsPaused)

    private val mAccessibilityServiceEnabled by lazy {
        MutableLiveData(AccessibilityUtils.isServiceEnabled(requireContext()))
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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

            MyAccessibilityService.provideBus().observe(viewLifecycleOwner) {
                when (it.peekContent().first) {
                    MyAccessibilityService.EVENT_ON_SERVICE_STARTED ->
                        mAccessibilityServiceEnabled.value = true

                    MyAccessibilityService.EVENT_ON_SERVICE_STOPPED ->
                        mAccessibilityServiceEnabled.value = false
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

        requireContext().defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this)
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {
        if (key == str(R.string.key_pref_keymaps_paused)) {
            mKeymapsPaused.value = AppPreferences.keymapsPaused
        }
    }
}