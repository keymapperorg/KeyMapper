package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.data.viewmodel.ChooseConstraintListViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewToolbarBinding
import io.github.sds100.keymapper.sectionHeader
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * A placeholder fragment containing a simple view.
 */
class ChooseConstraintListFragment : Fragment() {

    private val mViewModel: ChooseConstraintListViewModel by viewModels {
        InjectorUtils.provideChooseConstraintListViewModel()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentRecyclerviewToolbarBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = this@ChooseConstraintListFragment

            fragmentRecyclerView.epoxyRecyclerView.withModels {
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
                        }
                    }
                }
            }

            appBar.setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        return binding.root
    }
}