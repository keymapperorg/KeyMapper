package io.github.sds100.keymapper.mappings.keymaps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.constraints.ChooseConstraintFragment
import io.github.sds100.keymapper.constraints.ConfigConstraintsFragment
import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.mappings.ConfigMappingFragment
import io.github.sds100.keymapper.mappings.keymaps.trigger.ConfigTriggerOptionsFragment
import io.github.sds100.keymapper.mappings.keymaps.trigger.TriggerFragment
import io.github.sds100.keymapper.ui.utils.getJsonSerializable
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.Inject
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.intArray
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.FourFragments
import io.github.sds100.keymapper.util.ui.TwoFragments
import io.github.sds100.keymapper.util.ui.setupNavigation
import io.github.sds100.keymapper.util.ui.showPopups
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 22/11/20.
 */
class ConfigKeyMapFragment : ConfigMappingFragment() {

    private val args by navArgs<ConfigKeyMapFragmentArgs>()

    override val viewModel: ConfigKeyMapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        Inject.configKeyMapViewModel(requireContext())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //only load the keymap if opening this fragment for the first time
        if (savedInstanceState == null) {
            args.keymapUid.let {
                if (it == null) {
                    viewModel.loadNewKeymap()
                } else {
                    viewModel.loadKeymap(it)
                }
            }
        }

        viewModel.configTriggerViewModel.setupNavigation(this)

        setFragmentResultListener(ConfigConstraintsFragment.CHOOSE_CONSTRAINT_REQUEST_KEY) { _, result ->
            result.getJsonSerializable<Constraint>(ChooseConstraintFragment.EXTRA_CONSTRAINT)?.let {
                viewModel.configConstraintsViewModel.onChosenNewConstraint(it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.configActionsViewModel.openEditOptions.collectLatest { actionUid ->
                if (findNavController().currentDestination?.id == R.id.config_key_map_fragment) {
                    viewModel.editActionViewModel.setActionToConfigure(actionUid)
                    findNavController().navigate(ConfigKeyMapFragmentDirections.actionConfigKeymapFragmentToActionOptionsFragment())
                }
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.configTriggerViewModel.openEditOptions.collectLatest { triggerKeyUid ->
                if (findNavController().currentDestination?.id == R.id.config_key_map_fragment) {
                    viewModel.configTriggerKeyViewModel.setTriggerKeyToConfigure(triggerKeyUid)
                    findNavController().navigate(ConfigKeyMapFragmentDirections.actionTriggerKeyOptionsFragment())
                }
            }
        }

        viewModel.configTriggerViewModel.showPopups(this, binding)
        viewModel.configTriggerViewModel.optionsViewModel.showPopups(this, binding)

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.configTriggerViewModel.reportBug.collectLatest {
                findNavController().navigate(NavAppDirections.goToReportBugActivity())
            }
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.configTriggerViewModel.fixAppKilling.collectLatest {
                findNavController().navigate(NavAppDirections.goToFixAppKillingActivity())
            }
        }
    }

    override fun getFragmentInfoList() = intArray(R.array.config_keymap_fragments).map {
        when (it) {
            int(R.integer.fragment_id_trigger) -> it to TriggerFragment.Info()
            int(R.integer.fragment_id_trigger_options) -> it to ConfigTriggerOptionsFragment.Info()
            int(R.integer.fragment_id_constraint_list) -> it to ConfigKeyMapConstraintsFragment.Info()
            int(R.integer.fragment_id_action_list) -> it to KeyMapConfigActionsFragment.Info()

            int(R.integer.fragment_id_constraints_and_options) ->
                it to FragmentInfo(R.string.tab_constraints_and_more) {
                    ConstraintsAndOptionsFragment()
                }

            int(R.integer.fragment_id_trigger_and_action_list) ->
                it to FragmentInfo(R.string.tab_trigger_and_actions) { TriggerAndActionsFragment() }

            int(R.integer.fragment_id_config_keymap_all) ->
                it to FragmentInfo { AllFragments() }

            else -> throw Exception("Don't know how to create FragmentInfo for this fragment $it")
        }
    }

    class TriggerAndActionsFragment : TwoFragments(
        TriggerFragment.Info(),
        KeyMapConfigActionsFragment.Info()
    )

    class ConstraintsAndOptionsFragment : TwoFragments(
        ConfigTriggerOptionsFragment.Info(),
        ConfigKeyMapConstraintsFragment.Info()
    )

    class AllFragments : FourFragments(
        TriggerFragment.Info(),
        ConfigTriggerOptionsFragment.Info(),
        KeyMapConfigActionsFragment.Info(),
        ConfigKeyMapConstraintsFragment.Info()
    )
}