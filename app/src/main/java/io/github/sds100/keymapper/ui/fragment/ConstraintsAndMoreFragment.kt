package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.checkbox
import io.github.sds100.keymapper.constraint
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentConstraintsAndMoreBinding
import io.github.sds100.keymapper.util.observeLiveData
import io.github.sds100.keymapper.util.removeLiveData
import splitties.bitflags.hasFlag
import splitties.resources.str
import splitties.toast.toast

/**
 * Created by sds100 on 19/03/2020.
 */
class ConstraintsAndMoreFragment : Fragment() {
    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentConstraintsAndMoreBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            subscribeFlagList()
            subscribeConstraintsList()

            findNavController().apply {
                setOnAddConstraintClick {
                    val direction = ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToChooseConstraint()
                    findNavController().navigate(direction)
                }

                currentBackStackEntry?.observeLiveData<Constraint>(
                    viewLifecycleOwner,
                    ChooseConstraintListFragment.SAVED_STATE_KEY) {

                    if (!mViewModel.addConstraint(it)) {
                        toast(R.string.error_constraint_exists)
                    }

                    // prevents the livedata observers receiving callbacks for the same data repeatedly.
                    // e.g on configuration changes
                    currentBackStackEntry?.removeLiveData<Constraint>(ChooseConstraintListFragment.SAVED_STATE_KEY)
                }
            }

            return this.root
        }
    }

    private fun FragmentConstraintsAndMoreBinding.subscribeConstraintsList() {
        mViewModel.constraintModelList.observe(viewLifecycleOwner) { constraintList ->
            epoxyRecyclerViewConstraints.withModels {
                constraintList.forEachIndexed { index, constraint ->
                    constraint {
                        id(constraint.id)
                        description(constraint.description)
                        errorMessage(constraint.error?.fullMessage)

                        icon(constraint.icon)

                        onRemoveClick { _ ->
                            mViewModel.removeConstraint(index)
                        }
                    }
                }
            }
        }
    }

    private fun FragmentConstraintsAndMoreBinding.subscribeFlagList() {
        mViewModel.flags.observe(viewLifecycleOwner) { flags ->
            epoxyRecyclerViewFlags.withModels {
                KeyMap.KEYMAP_FLAG_LABEL_MAP.keys.forEach { flagId ->
                    checkbox {
                        id(flagId)

                        val labelResId = KeyMap.KEYMAP_FLAG_LABEL_MAP[flagId]

                        if (labelResId != null) {
                            primaryText(str(labelResId))
                        }

                        isSelected(flags.hasFlag(flagId))

                        onClick { _ ->
                            mViewModel.toggleFlag(flagId)
                        }
                    }
                }
            }
        }
    }
}