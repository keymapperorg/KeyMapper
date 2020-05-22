package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.map
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
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.buildChipModel
import io.github.sds100.keymapper.util.observeLiveDataEvent
import io.github.sds100.keymapper.util.str
import splitties.bitflags.hasFlag
import splitties.toast.toast

/**
 * Created by sds100 on 19/03/2020.
 */
class ConstraintsAndMoreFragment(private val mKeymapId: Long) : Fragment() {

    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), mKeymapId)
    }

    private val mConstraintModelList by lazy {
        mViewModel.constraintList.map { constraintList ->
            sequence {
                constraintList.forEach {
                    yield(it.buildChipModel(requireContext()))
                }
            }.toList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentConstraintsAndMoreBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            subscribeFlagList()
            subscribeConstraintsList()

            findNavController().apply {
                setOnAddConstraintClick {
                    navigate(ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToChooseConstraint())
                }

                currentBackStackEntry?.observeLiveDataEvent<Constraint>(
                    viewLifecycleOwner,
                    ChooseConstraintListFragment.SAVED_STATE_KEY) {

                    if (!mViewModel.addConstraint(it)) {
                        toast(R.string.error_constraint_exists)
                    }
                }
            }

            return this.root
        }
    }

    private fun FragmentConstraintsAndMoreBinding.subscribeConstraintsList() {
        mConstraintModelList.observe(viewLifecycleOwner) { constraintList ->
            epoxyRecyclerViewConstraints.withModels {
                constraintList.forEachIndexed { index, constraint ->
                    constraint {
                        id(constraint.id)
                        model(constraint)

                        onRemoveClick { _ ->
                            mViewModel.removeConstraint(constraint.id)
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