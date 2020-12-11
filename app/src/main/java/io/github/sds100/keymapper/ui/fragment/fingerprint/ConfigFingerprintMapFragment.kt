package io.github.sds100.keymapper.ui.fragment.fingerprint

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import io.github.sds100.keymapper.data.model.options.FingerprintActionOptions
import io.github.sds100.keymapper.data.viewmodel.ConfigFingerprintMapViewModel
import io.github.sds100.keymapper.databinding.FragmentConfigFingerprintMapBinding
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
class ConfigFingerprintMapFragment : Fragment() {
    private val mArgs by navArgs<ConfigFingerprintMapFragmentArgs>()

    private val mViewModel: ConfigFingerprintMapViewModel
        by navGraphViewModels(R.id.nav_config_fingerprint_map) {
            InjectorUtils.provideConfigFingerprintMapViewModel(requireContext())
        }

    private lateinit var mRecoverFailureDelegate: RecoverFailureDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //only load the fingerprint map if opening this fragment for the first time
        if (savedInstanceState == null) {
            mViewModel.loadFingerprintMap(mArgs.gestureId)
        }

        mRecoverFailureDelegate = RecoverFailureDelegate(
            "ConfigFingerprintMapFragment",
            requireActivity().activityResultRegistry,
            this) {

            mViewModel.actionListViewModel.rebuildModels()
        }

        setFragmentResultListener(ActionListFragment.CHOOSE_ACTION_REQUEST_KEY) { _, result ->
            result.getParcelable<Action>(ChooseActionFragment.EXTRA_ACTION)?.let {
                mViewModel.actionListViewModel.addAction(it)
            }
        }

        setFragmentResultListener(ConstraintListFragment.CHOOSE_CONSTRAINT_REQUEST_KEY) { _,
                                                                                          result ->
            result.getParcelable<Constraint>(ChooseConstraintFragment.EXTRA_CONSTRAINT)?.let {
                mViewModel.constraintListViewModel.addConstraint(it)
            }
        }

        setFragmentResultListener(FingerprintActionOptionsFragment.REQUEST_KEY) { _, result ->
            result.getParcelable<FingerprintActionOptions>(BaseOptionsDialogFragment.EXTRA_OPTIONS)
                ?.let {
                    mViewModel.actionListViewModel.setOptions(it)
                }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentConfigFingerprintMapBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            viewPager.adapter = createFragmentPagerAdapter()

            TabLayoutMediator(tabLayout, viewPager) { tab, position ->
                tab.text = strArray(R.array.config_fingerprint_map_tab_titles)[position]
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
                        mViewModel.save(lifecycleScope)
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

    override fun onSaveInstanceState(outState: Bundle) {
        mViewModel.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState ?: return

        mViewModel.restoreState(savedInstanceState)
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
        intArray(R.array.config_fingerprint_map_fragments).map {
            when (it) {

                int(R.integer.fragment_id_action_list) ->
                    it to { FingerprintActionListFragment() }

                int(R.integer.fragment_id_constraint_list) ->
                    it to { FingerprintConstraintListFragment() }

                int(R.integer.fragment_id_fingerprint_map_options) ->
                    it to { FingerprintMapOptionsFragment() }

                int(R.integer.fragment_id_constraints_and_options) ->
                    it to { ConstraintsAndOptionsFragment() }

                else -> throw Exception("Don't know how to instantiate a fragment for this id $id")
            }
        }
    )

    class ConstraintsAndOptionsFragment : TwoFragments(
        top = R.string.option_list_header to FingerprintMapOptionsFragment::class.java,
        bottom = R.string.constraint_list_header to FingerprintConstraintListFragment::class.java
    )
}