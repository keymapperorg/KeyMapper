package io.github.sds100.keymapper.ui.fragment

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.constraint
import io.github.sds100.keymapper.data.model.ConstraintModel
import io.github.sds100.keymapper.data.viewmodel.ConstraintListViewModel
import io.github.sds100.keymapper.databinding.FragmentConstraintListBinding
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.IModelState
import splitties.toast.toast

/**
 * Created by sds100 on 29/11/20.
 */
abstract class ConstraintListFragment
    : RecyclerViewFragment<List<ConstraintModel>, FragmentConstraintListBinding>() {

    companion object {
        const val CHOOSE_CONSTRAINT_REQUEST_KEY = "request_choose_constraint"
    }

    abstract val constraintListViewModel: ConstraintListViewModel

    override val modelState: IModelState<List<ConstraintModel>>
        get() = constraintListViewModel

    override fun onResume() {
        super.onResume()

        constraintListViewModel.rebuildModels()
    }

    override fun bind(
        inflater: LayoutInflater,
        container: ViewGroup?
    ) = FragmentConstraintListBinding.inflate(inflater, container, false).apply {
        lifecycleOwner = viewLifecycleOwner
    }

    override fun subscribeUi(binding: FragmentConstraintListBinding) {
        binding.viewModel = constraintListViewModel

        binding.setOnAddConstraintClick {
            val direction = NavAppDirections.actionGlobalChooseConstraint(
                CHOOSE_CONSTRAINT_REQUEST_KEY,
                constraintListViewModel.supportedConstraintList.toTypedArray())
            findNavController().navigate(direction)
        }

        constraintListViewModel.eventStream.observe(viewLifecycleOwner, { event ->
            when (event) {
                is MessageEvent -> toast(event.textRes)

                is BuildConstraintListModels -> {
                    viewLifecycleScope.launchWhenResumed {
                        val modelList = event.source.map { it.buildModel(requireContext()) }
                        constraintListViewModel.setModels(modelList)
                    }
                }
            }
        })
    }

    override fun populateList(
        binding: FragmentConstraintListBinding,
        model: List<ConstraintModel>?
    ) {
        binding.epoxyRecyclerViewConstraints.withModels {
            model?.forEach { constraint ->
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
}