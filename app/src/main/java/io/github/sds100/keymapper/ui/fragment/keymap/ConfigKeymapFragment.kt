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
import io.github.sds100.keymapper.data.model.options.TriggerKeyOptions
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentConfigKeymapBinding
import io.github.sds100.keymapper.ui.adapter.GenericFragmentPagerAdapter
import io.github.sds100.keymapper.ui.fragment.*
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.positiveButton

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
                TriggerOptionsFragment::class.java.name -> TriggerOptionsFragment(mArgs.keymapId)

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
            val options = result.getSerializable(BaseOptionsDialogFragment.EXTRA_OPTIONS) as TriggerKeyOptions
            mViewModel.triggerViewModel.setTriggerKeyOptions(options)
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
                    is FixFailure -> coordinatorLayout.showFixActionSnackBar(
                        event.failure,
                        requireActivity(),
                        mRecoverFailureDelegate
                    )

                    is EnableAccessibilityServicePrompt -> coordinatorLayout.showEnableAccessibilityServiceSnackBar()
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
                int(R.integer.fragment_id_trigger_and_actions) -> it to { TriggerAndActionsFragment() }
                int(R.integer.fragment_id_keymap_constraints_and_more) -> it to { ConstraintsAndOptionsFragment() }
                int(R.integer.fragment_id_config_keymap_all) -> it to { AllFragments() }

                else -> throw Exception("Don't know how to instantiate a fragment for this id $id")
            }
        }
    )

    class TriggerAndActionsFragment : TwoFragments(
        top = R.string.trigger_list_header to TriggerFragment::class.java,
        bottom = R.string.action_list_header to KeymapActionListFragment::class.java
    )

    class ConstraintsAndOptionsFragment : TwoFragments(
        top = R.string.trigger_options_header to TriggerOptionsFragment::class.java,
        bottom = R.string.constraint_list_header to KeymapConstraintListFragment::class.java
    )

    class AllFragments : FourFragments(
        topLeft = R.string.trigger_list_header to TriggerFragment::class.java,
        topRight = R.string.trigger_options_header to TriggerOptionsFragment::class.java,
        bottomLeft = R.string.action_list_header to KeymapActionListFragment::class.java,
        bottomRight = R.string.constraint_list_header to KeymapConstraintListFragment::class.java
    )
}