package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.constraint
import io.github.sds100.keymapper.data.viewmodel.ConstraintListViewModel
import io.github.sds100.keymapper.databinding.FragmentConstraintListBinding
import io.github.sds100.keymapper.util.*
import splitties.toast.toast

/**
 * Created by sds100 on 29/11/20.
 */
abstract class ConstraintListFragment : Fragment() {
    companion object {
        const val CHOOSE_CONSTRAINT_REQUEST_KEY = "request_choose_constraint"
    }

    abstract val constraintListViewModel: ConstraintListViewModel

    /**
     * Scoped to the lifecycle of the fragment's view (between onCreateView and onDestroyView)
     */
    private var _binding: FragmentConstraintListBinding? = null
    val binding: FragmentConstraintListBinding
        get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentConstraintListBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = constraintListViewModel

            _binding = this
        }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            setOnAddConstraintClick {
                val direction = NavAppDirections.actionGlobalChooseConstraint(CHOOSE_CONSTRAINT_REQUEST_KEY)
                findNavController().navigate(direction)
            }

            constraintListViewModel.eventStream.observe(viewLifecycleOwner, { event ->
                when (event) {
                    is MessageEvent -> toast(event.textRes)

                    is BuildConstraintListModels -> {
                        val modelList = event.source.map { it.buildModel(requireContext()) }
                        constraintListViewModel.setModels(modelList)
                    }
                }
            })

            subscribeConstraintsList()

        }
    }

    override fun onResume() {
        super.onResume()

        constraintListViewModel.rebuildModels()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun FragmentConstraintListBinding.subscribeConstraintsList() {
        constraintListViewModel.modelList.observe(viewLifecycleOwner, { constraintList ->
            epoxyRecyclerViewConstraints.withModels {
                constraintList.ifIsData {
                    it.forEach { constraint ->
                        constraint {
                            id(constraint.id)
                            model(constraint)

                            onRemoveClick { _ ->
                                constraintListViewModel.removeConstraint(constraint.id)
                            }

                            val tintType = when {
                                constraint.hasError -> TintType.ERROR
                                constraint.iconTintOnSurface -> TintType.ON_SURFACE
                                else -> TintType.NONE
                            }

                            tintType(tintType)

                            onFixClick { _ ->
                                constraintListViewModel.onModelClick(constraint.id)
                            }
                        }
                    }
                }
            }
        })
    }
}