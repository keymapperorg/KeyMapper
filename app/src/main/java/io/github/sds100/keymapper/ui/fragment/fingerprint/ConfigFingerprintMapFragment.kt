package io.github.sds100.keymapper.ui.fragment.fingerprint

import android.os.Bundle
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.options.FingerprintActionOptions
import io.github.sds100.keymapper.data.viewmodel.ConfigFingerprintMapViewModel
import io.github.sds100.keymapper.ui.fragment.*
import io.github.sds100.keymapper.util.FragmentInfo
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.int
import io.github.sds100.keymapper.util.intArray

/**
 * Created by sds100 on 22/11/20.
 */
class ConfigFingerprintMapFragment : ConfigMappingFragment() {
    private val args by navArgs<ConfigFingerprintMapFragmentArgs>()

    override val viewModel: ConfigFingerprintMapViewModel
        by navGraphViewModels(R.id.nav_config_fingerprint_map) {
            InjectorUtils.provideConfigFingerprintMapViewModel(requireContext())
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //only load the fingerprint map if opening this fragment for the first time
        if (savedInstanceState == null) {
            viewModel.loadFingerprintMap(args.gestureId)
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

        setFragmentResultListener(FingerprintActionOptionsFragment.REQUEST_KEY) { _, result ->
            result.getParcelable<FingerprintActionOptions>(BaseOptionsDialogFragment.EXTRA_OPTIONS)
                ?.let {
                    viewModel.actionListViewModel.setOptions(it)
                }
        }
    }

    override fun getFragmentInfoList() =
        intArray(R.array.config_fingerprint_map_fragments).map {
            when (it) {
                int(R.integer.fragment_id_action_list) ->
                    it to FingerprintActionListFragment.Info()

                int(R.integer.fragment_id_fingerprint_map_options) ->
                    it to FingerprintMapOptionsFragment.Info()

                int(R.integer.fragment_id_constraint_list) ->
                    it to FingerprintConstraintListFragment.Info()

                int(R.integer.fragment_id_constraints_and_options) ->
                    it to FragmentInfo(R.string.tab_constraints_and_more) {
                        ConstraintsAndOptionsFragment()
                    }

                else -> throw Exception("Don't know how to create FragmentInfo for this fragment $it")
            }
        }

    class ConstraintsAndOptionsFragment : TwoFragments(
        FingerprintMapOptionsFragment.Info(),
        FingerprintConstraintListFragment.Info()
    )
}