package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.google.android.material.tabs.TabLayoutMediator
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.behavior.ActionBehavior
import io.github.sds100.keymapper.data.model.behavior.TriggerKeyBehavior
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentConfigKeymapBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.adapter.ConfigKeymapPagerAdapter
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import io.github.sds100.keymapper.util.result.RecoverableFailure
import io.github.sds100.keymapper.util.result.getFullMessage
import splitties.alertdialog.appcompat.*
import splitties.alertdialog.appcompat.coroutines.showAndAwait
import splitties.snackbar.action
import splitties.snackbar.longSnack
import splitties.snackbar.snack
import splitties.toast.toast

/**
 * Created by sds100 on 19/02/2020.
 */
class ConfigKeymapFragment : Fragment() {
    companion object {
        const val CHOOSE_ACTION_REQUEST_KEY = "request_choose_action"
        const val CHOOSE_CONSTRAINT_REQUEST_KEY = "request_choose_constraint"
    }

    private val mArgs by navArgs<ConfigKeymapFragmentArgs>()

    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), mArgs.keymapId)
    }

    private val mFragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String) =
            when (className) {
                TriggerFragment::class.java.name -> TriggerFragment(mArgs.keymapId)
                ActionsFragment::class.java.name -> ActionsFragment(mArgs.keymapId)
                KeymapOptionsFragment::class.java.name -> KeymapOptionsFragment(mArgs.keymapId)
                KeymapConstraintsFragment::class.java.name -> KeymapConstraintsFragment(mArgs.keymapId)
                ConstraintsAndMoreFragment::class.java.name -> ConstraintsAndMoreFragment()

                else -> super.instantiate(classLoader, className)
            }
    }

    private lateinit var mRecoverFailureDelegate: RecoverFailureDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        childFragmentManager.fragmentFactory = mFragmentFactory

        super.onCreate(savedInstanceState)

        mRecoverFailureDelegate = RecoverFailureDelegate(
            "ConfigKeymapFragment",
            requireActivity().activityResultRegistry,
            this) {

            mViewModel.rebuildActionModels()
        }

        setFragmentResultListener(CHOOSE_ACTION_REQUEST_KEY) { _, result ->
            val action = result.getSerializable(ChooseActionFragment.EXTRA_ACTION) as Action
            mViewModel.addAction(action)
        }

        setFragmentResultListener(CHOOSE_CONSTRAINT_REQUEST_KEY) { _, result ->
            val constraint = result.getSerializable(ChooseConstraintFragment.EXTRA_CONSTRAINT) as Constraint
            mViewModel.addConstraint(constraint)
        }

        setFragmentResultListener(ActionBehaviorFragment.REQUEST_KEY) { _, result ->
            mViewModel.setActionBehavior(
                result.getSerializable(ActionBehaviorFragment.EXTRA_ACTION_BEHAVIOR) as ActionBehavior)
        }

        setFragmentResultListener(TriggerKeyBehaviorFragment.REQUEST_KEY) { _, result ->
            mViewModel.setTriggerKeyBehavior(
                result.getSerializable(TriggerKeyBehaviorFragment.EXTRA_TRIGGER_KEY_BEHAVIOR) as TriggerKeyBehavior)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentConfigKeymapBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            viewPager.adapter = ConfigKeymapPagerAdapter(this@ConfigKeymapFragment, mArgs.keymapId)

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = strArray(R.array.config_keymap_tab_titles)[position]
            }.attach()

            tabLayout.isVisible = tabLayout.tabCount > 1

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                showOnBackPressedWarning()
            }

            appBar.setNavigationOnClickListener {
                showOnBackPressedWarning()
            }

            appBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.action_save -> {
                        mViewModel.saveKeymap(lifecycleScope)
                        findNavController().navigateUp()

                        true
                    }

                    R.id.action_help -> {
                        val direction = ConfigKeymapFragmentDirections.actionGlobalHelpFragment()
                        findNavController().navigate(direction)

                        true
                    }

                    else -> false
                }
            }

            mViewModel.duplicateConstraintsEvent.observe(viewLifecycleOwner, EventObserver {
                toast(R.string.error_keymap_constraint_exists)
            })

            mViewModel.showOnboardingPrompt.observe(viewLifecycleOwner, EventObserver {

                lifecycleScope.launchWhenStarted {
                    val approvedWarning = requireActivity().alertDialog {
                        message = str(it.message)

                    }.showAndAwait(okValue = true, cancelValue = null, dismissValue = false)

                    if (approvedWarning) {
                        it.onApproved.invoke()
                    }
                }
            })

            mViewModel.showFixPrompt.observe(viewLifecycleOwner, EventObserver {
                coordinatorLayout.longSnack(it.getFullMessage(requireContext())) {

                    //only add an action to fix the error if the error can be recovered from
                    if (it is RecoverableFailure) {
                        action(R.string.snackbar_fix) {
                            mRecoverFailureDelegate.recover(requireActivity(), it)
                        }
                    }

                    show()
                }
            })

            mViewModel.startRecordingTriggerInService.observe(viewLifecycleOwner, EventObserver {
                val serviceEnabled = AccessibilityUtils.isServiceEnabled(requireContext())

                if (serviceEnabled) {
                    requireContext().sendPackageBroadcast(MyAccessibilityService.ACTION_RECORD_TRIGGER)

                } else {
                    mViewModel.promptToEnableAccessibilityService.value = Event(Unit)
                }
            })

            mViewModel.promptToEnableAccessibilityService.observe(viewLifecycleOwner, EventObserver {
                coordinatorLayout.snack(R.string.error_accessibility_service_disabled_record_trigger) {
                    setAction(str(R.string.snackbar_fix)) {
                        AccessibilityUtils.enableService(requireContext())
                    }
                }
            })

            mViewModel.stopRecordingTrigger.observe(viewLifecycleOwner, EventObserver {
                val serviceEnabled = AccessibilityUtils.isServiceEnabled(requireContext())

                if (serviceEnabled) {
                    stopRecordingTrigger()
                } else {
                    mViewModel.promptToEnableAccessibilityService.value = Event(Unit)
                }
            })

            mViewModel.promptToEnableCapsLockKeyboardLayout.observe(viewLifecycleOwner, EventObserver {
                requireActivity().alertDialog {
                    messageResource = R.string.dialog_message_enable_physical_keyboard_caps_lock_a_keyboard_layout

                    okButton()

                    show()
                }
            })

            return this.root
        }
    }

    override fun onPause() {
        super.onPause()

        stopRecordingTrigger()
    }

    private fun showOnBackPressedWarning() {
        requireContext().alertDialog {
            messageResource = R.string.dialog_message_are_you_sure_want_to_leave_without_saving

            positiveButton(R.string.pos_yes) {
                findNavController().navigateUp()
            }

            cancelButton()
            show()
        }
    }

    private fun stopRecordingTrigger() {
        requireContext().sendPackageBroadcast(MyAccessibilityService.ACTION_STOP_RECORDING_TRIGGER)
    }
}