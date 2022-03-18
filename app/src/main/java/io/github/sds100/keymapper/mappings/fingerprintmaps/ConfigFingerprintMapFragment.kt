package io.github.sds100.keymapper.mappings.fingerprintmaps

import android.os.Bundle
import android.view.View
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.constraints.ChooseConstraintFragment
import io.github.sds100.keymapper.constraints.ConfigConstraintsFragment
import io.github.sds100.keymapper.constraints.Constraint
import io.github.sds100.keymapper.mappings.ConfigMappingFragment
import io.github.sds100.keymapper.ui.utils.getJsonSerializable
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.ui.TwoFragments
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 22/11/20.
 */
class ConfigFingerprintMapFragment : ConfigMappingFragment() {
    private val args by navArgs<ConfigFingerprintMapFragmentArgs>()

    override val viewModel: ConfigFingerprintMapViewModel
        by navGraphViewModels(R.id.nav_config_fingerprint_map) {
            Inject.configFingerprintMapViewModel(requireContext())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //only load the fingerprint map if opening this fragment for the first time
        if (savedInstanceState == null) {
            viewModel.loadFingerprintMap(FingerprintMapId.valueOf(args.gestureId))
        }

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
                if (findNavController().currentDestination?.id == R.id.config_fingerprint_map_fragment) {
                    viewModel.editActionViewModel.setActionToConfigure(actionUid)
                    findNavController().navigate(ConfigFingerprintMapFragmentDirections.configActionFragment())
                }
            }
        }
    }

    override fun getFragmentInfoList() =
        intArray(R.array.config_fingerprint_map_fragments).map {
            when (it) {
                int(R.integer.fragment_id_action_list) ->
                    it to FingerprintConfigActionsFragment.Info()

                int(R.integer.fragment_id_fingerprint_map_options) ->
                    it to FingerprintMapOptionsFragment.Info()

                int(R.integer.fragment_id_constraint_list) ->
                    it to FingerprintConfigConstraintsFragment.Info()

                int(R.integer.fragment_id_constraints_and_options) ->
                    it to FragmentInfo(R.string.tab_constraints_and_more) {
                        ConstraintsAndOptionsFragment()
                    }

                else -> throw Exception("Don't know how to create FragmentInfo for this fragment $it")
            }
        }

    class ConstraintsAndOptionsFragment : TwoFragments(
        FingerprintMapOptionsFragment.Info(),
        FingerprintConfigConstraintsFragment.Info()
    )
}