package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.data.model.AppListItemModel
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.ConstraintType
import io.github.sds100.keymapper.data.viewmodel.ChooseConstraintListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.sectionHeader
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.InjectorUtils
import io.github.sds100.keymapper.util.observeLiveData

/**
 * A placeholder fragment containing a simple view.
 */
class ChooseConstraintListFragment : RecyclerViewFragment() {

    companion object {
        const val SAVED_STATE_KEY = "key_constraint"
    }

    private val mViewModel: ChooseConstraintListViewModel by viewModels {
        InjectorUtils.provideChooseConstraintListViewModel()
    }

    override val savedStateKey = SAVED_STATE_KEY

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        observeFragmentChildrenLiveData()

        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun subscribeList(binding: FragmentRecyclerviewBinding) {
        binding.epoxyRecyclerView.withModels {
            mViewModel.constraintsSortedByCategory.forEach {
                val sectionHeader = it.first
                val constraints = it.second

                sectionHeader {
                    id(sectionHeader)
                    header(sectionHeader)
                }

                constraints.forEach { constraint ->
                    simple {
                        id(constraint.id)
                        primaryText(constraint.description)

                        onClick { _ ->
                            onConstraintClick(constraint.id)
                        }
                    }
                }
            }
        }
    }

    private fun observeFragmentChildrenLiveData() {
        findNavController().currentBackStackEntry?.observeLiveData<AppListItemModel>(
            viewLifecycleOwner,
            AppListFragment.SAVED_STATE_KEY
        ) {
            selectModel(Constraint.appConstraint(it.packageName))
        }
    }

    private fun onConstraintClick(@ConstraintType id: String) {
        when (id) {
            Constraint.APP_FOREGROUND -> {
                val direction =
                    ChooseConstraintListFragmentDirections.actionChooseConstraintListFragmentToAppListFragment()
                findNavController().navigate(direction)
            }
        }
    }
}