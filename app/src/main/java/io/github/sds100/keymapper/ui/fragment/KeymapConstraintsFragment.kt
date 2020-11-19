package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.map
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.constraint
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentKeymapConstraintsBinding
import io.github.sds100.keymapper.util.Event
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.TintType
import io.github.sds100.keymapper.util.buildModel

/**
 * Created by sds100 on 19/03/2020.
 */
class KeymapConstraintsFragment(private val mKeymapId: Long) : Fragment() {

    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), mKeymapId)
    }

    private val mConstraintModelList by lazy {
        mViewModel.constraintList.map { constraintList ->
            sequence {
                constraintList.forEach {
                    yield(it.buildModel(requireContext()))
                }
            }.toList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentKeymapConstraintsBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            subscribeConstraintsList()

            setOnAddConstraintClick {
                val direction = ConfigKeymapFragmentDirections
                    .actionConfigKeymapFragmentToChooseConstraint(ConfigKeymapFragment.CHOOSE_CONSTRAINT_REQUEST_KEY)

                findNavController().navigate(direction)
            }

            return this.root
        }
    }

    override fun onResume() {
        super.onResume()

        mViewModel.rebuildConstraintModels()
    }

    private fun FragmentKeymapConstraintsBinding.subscribeConstraintsList() {
        mConstraintModelList.observe(viewLifecycleOwner, { constraintList ->
            epoxyRecyclerViewConstraints.withModels {
                constraintList.forEach { constraint ->
                    constraint {
                        id(constraint.id)
                        model(constraint)

                        onRemoveClick { _ ->
                            mViewModel.removeConstraint(constraint.id)
                        }

                        val tintType = when {
                            constraint.hasError -> TintType.ERROR
                            constraint.iconTintOnSurface -> TintType.ON_SURFACE
                            else -> TintType.NONE
                        }

                        tintType(tintType)

                        onFixClick { _ ->
                            val model = mConstraintModelList.value?.find { it.id == constraint.id && it.hasError }
                                ?: return@onFixClick

                            if (model.hasError) {
                                mViewModel.showFixPrompt.value = Event(model.failure!!)
                            }
                        }
                    }
                }
            }
        })
    }
}