package io.github.sds100.keymapper.ui.fragment.keymap

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
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.options.KeymapActionOptions
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentConfigKeymapBinding
import io.github.sds100.keymapper.ui.adapter.GenericFragmentPagerAdapter
import io.github.sds100.keymapper.ui.fragment.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import io.github.sds100.keymapper.util.result.RecoverableFailure
import io.github.sds100.keymapper.util.result.getFullMessage
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.positiveButton
import splitties.resources.intArray
import splitties.snackbar.action
import splitties.snackbar.longSnack
import splitties.snackbar.snack

/**
 * Created by sds100 on 22/11/20.
 */
class ConfigKeymapFragment : Fragment() {
    private val mArgs by navArgs<ConfigKeymapFragmentArgs>()

    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideNewConfigKeymapViewModel(requireContext(), mArgs.keymapId)
    }

    private val mFragmentFactory = object : FragmentFactory() {
        override fun instantiate(classLoader: ClassLoader, className: String) =
            when (className) {
                TriggerFragment::class.java.name -> TriggerFragment(mArgs.keymapId)
                KeymapActionListFragment::class.java.name -> KeymapActionListFragment(mArgs.keymapId)
                KeymapConstraintListFragment::class.java.name -> KeymapConstraintListFragment(mArgs.keymapId)

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

            mViewModel.actionListViewModel.rebuildModels()
        }

        setFragmentResultListener(ActionListFragment.CHOOSE_ACTION_REQUEST_KEY) { _, result ->
            val action = result.getSerializable(ChooseActionFragment.EXTRA_ACTION) as Action
            mViewModel.actionListViewModel.addAction(action)
        }

        setFragmentResultListener(ConstraintListFragment.CHOOSE_CONSTRAINT_REQUEST_KEY) { _, result ->
            val constraint = result.getSerializable(ChooseConstraintFragment.EXTRA_CONSTRAINT) as Constraint
            mViewModel.constraintListViewModel.addConstraint(constraint)
        }

        setFragmentResultListener(KeymapActionOptionsFragment.REQUEST_KEY) { _, result ->
            val options = result.getSerializable(BaseOptionsDialogFragment.EXTRA_OPTIONS) as KeymapActionOptions
            mViewModel.actionListViewModel.setOptions(options)
        }

        setFragmentResultListener(TriggerKeyOptionsFragment.REQUEST_KEY) { _, result ->

        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentConfigKeymapBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            viewPager.adapter = createFragmentPagerAdapter()

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
                        val direction = NavAppDirections.actionGlobalHelpFragment()
                        findNavController().navigate(direction)

                        true
                    }

                    else -> false
                }
            }

            mViewModel.eventStream.observe(viewLifecycleOwner, { event ->
                when (event) {
                    is FixFailure -> {
                        coordinatorLayout.longSnack(event.failure.getFullMessage(requireContext())) {

                            //only add an action to fix the error if the error can be recovered from
                            if (event.failure is RecoverableFailure) {
                                action(R.string.snackbar_fix) {
                                    mRecoverFailureDelegate.recover(requireActivity(), event.failure)
                                }
                            }

                            show()
                        }
                    }

                    is EnableAccessibilityServicePrompt -> {
                        coordinatorLayout.snack(R.string.error_accessibility_service_disabled_record_trigger) {
                            setAction(str(R.string.snackbar_fix)) {
                                AccessibilityUtils.enableService(requireContext())
                            }
                        }
                    }
                }
            })

            return this.root
        }
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

    private fun createFragmentPagerAdapter() = GenericFragmentPagerAdapter(this,
        intArray(R.array.config_keymap_fragments).map {
            when (it) {

                int(R.integer.fragment_id_actions) -> it to { KeymapActionListFragment(mArgs.keymapId) }
                int(R.integer.fragment_id_trigger) -> it to { TriggerFragment(mArgs.keymapId) }
                int(R.integer.fragment_id_keymap_constraints) -> it to { KeymapConstraintListFragment(mArgs.keymapId) }
                int(R.integer.fragment_id_trigger_options) -> it to { TriggerOptionsFragment(mArgs.keymapId) }

                else -> throw Exception("Don't know how to instantiate a fragment for this id $id")
            }
        }
    )
}