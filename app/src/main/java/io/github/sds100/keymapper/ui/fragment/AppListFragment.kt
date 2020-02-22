package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.viewmodel.AppListViewModel
import io.github.sds100.keymapper.data.viewmodel.ChooseActionSharedViewModel
import io.github.sds100.keymapper.databinding.FragmentRecyclerviewBinding
import io.github.sds100.keymapper.simple
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.InjectorUtils

/**
 * Created by sds100 on 22/02/2020.
 */
class AppListFragment : Fragment() {
    private val mChooseActionSharedViewModel: ChooseActionSharedViewModel by activityViewModels()

    private val mViewModel: AppListViewModel by viewModels {
        InjectorUtils.provideAppListViewModel(requireContext())
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentRecyclerviewBinding.inflate(inflater, container, false)

        binding.progressCallback = mViewModel
        binding.lifecycleOwner = viewLifecycleOwner

        mViewModel.appModelList.observe(viewLifecycleOwner) { appModelList ->
            binding.epoxyRecyclerView.withModels {
                appModelList.forEach {
                    simple {
                        id(it.packageName)
                        primaryText(it.appName)
                        icon(it.icon)

                        onClick { _ ->
                            val action = Action(ActionType.APP, data = it.packageName)
                            mChooseActionSharedViewModel.selectAction(action)

                            findNavController().navigateUp()
                        }
                    }
                }
            }
        }
        return binding.root
    }
}