package io.github.sds100.keymapper.ui.fragment.keymap

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
import kotlinx.android.synthetic.main.fragment_home.*
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.positiveButton
import uk.co.samuelwall.materialtaptargetprompt.MaterialTapTargetPrompt

/**
 * Created by sds100 on 22/11/20.
 */
class ConfigKeymapFragment : Fragment() {
    private val args by navArgs<ConfigKeymapFragmentArgs>()

    private val viewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext())
    }

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentConfigKeymapBinding? = null
    val binding: FragmentConfigKeymapBinding
        get() = _binding!!

    private lateinit var recoverFailureDelegate: RecoverFailureDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //only load the keymap if opening this fragment for the first time
        if (savedInstanceState == null) {
            viewModel.loadKeymap(args.keymapId)
        }

        setFragmentResultListener(ActionListFragment.CHOOSE_ACTION_REQUEST_KEY) { _, result ->
            result.getParcelable<Action>(ChooseActionFragment.EXTRA_ACTION)?.let {
                viewModel.actionListViewModel.addAction(it)
            }
        }

        setFragmentResultListener(ConstraintListFragment.CHOOSE_CONSTRAINT_REQUEST_KEY) { _, result ->
            result.getParcelable<Constraint>(ChooseConstraintFragment.EXTRA_CONSTRAINT)?.let {
                viewModel.constraintListViewModel.addConstraint(it)
            }
        }

        setFragmentResultListener(KeymapActionOptionsFragment.REQUEST_KEY) { _, result ->
            result.getParcelable<KeymapActionOptions>(BaseOptionsDialogFragment.EXTRA_OPTIONS)?.let {
                viewModel.actionListViewModel.setOptions(it)
            }
        }

        setFragmentResultListener(TriggerKeyOptionsFragment.REQUEST_KEY) { _, result ->
            result.getParcelable<TriggerKeyOptions>(BaseOptionsDialogFragment.EXTRA_OPTIONS)?.let {
                viewModel.triggerViewModel.setTriggerKeyOptions(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        recoverFailureDelegate = RecoverFailureDelegate(
            "ConfigKeymapFragment",
            requireActivity().activityResultRegistry,
            viewLifecycleOwner) {

            viewModel.actionListViewModel.rebuildModels()
        }

        FragmentConfigKeymapBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            _binding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = viewModel
        binding.viewPager.adapter = createFragmentPagerAdapter()

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = strArray(R.array.config_keymap_tab_titles)[position]
        }.attach()

        binding.tabLayout.isVisible = binding.tabLayout.tabCount > 1

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showOnBackPressedWarning()
        }

        binding.appBar.setNavigationOnClickListener {
            showOnBackPressedWarning()
        }

        binding.appBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.action_save -> {
                    viewModel.saveKeymap(lifecycleScope)
                    findNavController().navigateUp()

                    true
                }
                else -> false
            }
        }

        viewModel.eventStream.observe(viewLifecycleOwner, { event ->
            when (event) {
                is FixFailure -> binding.coordinatorLayout.showFixActionSnackBar(
                    event.failure,
                    requireContext(),
                    recoverFailureDelegate,
                    findNavController()
                )

                is EnableAccessibilityServicePrompt ->
                    binding.coordinatorLayout.showEnableAccessibilityServiceSnackBar()
            }
        })

        binding.apply {
            MaterialTapTargetPrompt.Builder(this@ConfigKeymapFragment)
                .setTarget(appBar.findViewById(R.id.action_help))
                .setFocalColour(color(android.R.color.transparent))
                .setPrimaryText("Check out the quick start guide to get started!")
                .show()
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        viewModel.saveState(outState)

        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        savedInstanceState ?: return

        viewModel.restoreState(savedInstanceState)
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

                int(R.integer.fragment_id_action_list) -> it to { KeymapActionListFragment() }
                int(R.integer.fragment_id_trigger) -> it to { TriggerFragment() }
                int(R.integer.fragment_id_constraint_list) -> it to { KeymapConstraintListFragment() }
                int(R.integer.fragment_id_trigger_options) -> it to { TriggerOptionsFragment() }
                int(R.integer.fragment_id_trigger_and_action_list) -> it to { TriggerAndActionsFragment() }
                int(R.integer.fragment_id_constraints_and_options) -> it to { ConstraintsAndOptionsFragment() }
                int(R.integer.fragment_id_config_keymap_all) -> it to { AllFragments() }

                else -> throw Exception("Don't know how to instantiate a fragment for this id $id")
            }
        }
    )

    class TriggerAndActionsFragment : TwoFragments(
        top = Triple(
            R.string.trigger_list_header,
            TriggerFragment::class.java,
            R.string.url_trigger_options_guide
        ),
        bottom = Triple(
            R.string.action_list_header,
            KeymapActionListFragment::class.java,
            R.string.url_action_guide
        )
    )

    class ConstraintsAndOptionsFragment : TwoFragments(
        top = Triple(
            R.string.trigger_options_header,
            TriggerOptionsFragment::class.java,
            R.string.url_trigger_options_guide
        ),
        bottom = Triple(
            R.string.constraint_list_header,
            KeymapConstraintListFragment::class.java,
            R.string.url_constraints_guide
        )
    )

    class AllFragments : FourFragments(
        topLeft = Triple(
            R.string.trigger_list_header,
            TriggerFragment::class.java,
            R.string.url_trigger_options_guide
        ),
        topRight = Triple(
            R.string.trigger_options_header,
            TriggerOptionsFragment::class.java,
            R.string.url_trigger_options_guide
        ),
        bottomLeft = Triple(
            R.string.action_list_header,
            KeymapActionListFragment::class.java,
            R.string.url_action_guide
        ),
        bottomRight = Triple(
            R.string.constraint_list_header,
            KeymapConstraintListFragment::class.java,
            R.string.url_constraints_guide
        )
    )
}