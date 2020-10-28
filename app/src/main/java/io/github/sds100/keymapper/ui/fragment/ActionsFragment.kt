package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.map
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.action
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentActionsBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.*

/**
 * Created by sds100 on 18/05/2020.
 */
class ActionsFragment(private val mKeymapId: Long) : Fragment() {

    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), mKeymapId)
    }

    private val mActionModelList by lazy {
        mViewModel.actionList.map { actionList ->
            sequence {
                actionList.forEach {
                    yield(it.buildModel(requireContext()))
                }
            }.toList()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentActionsBinding.inflate(inflater, container, false).apply {
            viewModel = mViewModel
            lifecycleOwner = viewLifecycleOwner

            mViewModel.chooseAction.observe(viewLifecycleOwner, EventObserver {
                val direction = ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToChooseActionFragment()
                findNavController().navigate(direction)
            })

            subscribeActionList()

            mViewModel.testAction.observe(viewLifecycleOwner, EventObserver {
                if (AccessibilityUtils.isServiceEnabled(requireContext())) {

                    requireContext().sendPackageBroadcast(MyAccessibilityService.ACTION_TEST_ACTION,
                        bundleOf(MyAccessibilityService.EXTRA_ACTION to it))

                } else {
                    mViewModel.promptToEnableAccessibilityService.value = Event(Unit)
                }
            })

            mViewModel.chooseActionBehavior.observe(viewLifecycleOwner, EventObserver {
                val direction = ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToActionOptionsFragment(it)
                findNavController().navigate(direction)
            })

            return this.root
        }
    }

    override fun onResume() {
        super.onResume()

        mViewModel.rebuildActionModels()
    }

    private fun FragmentActionsBinding.subscribeActionList() {
        mActionModelList.observe(viewLifecycleOwner, { actionList ->
            epoxyRecyclerViewActions.withModels {

                actionList.forEachIndexed { _, model ->
                    action {
                        id(model.id)
                        model(model)
                        icon(model.icon)
                        actionCount(actionList.size)

                        onRemoveClick { _ ->
                            mViewModel.removeAction(model.id)
                        }

                        onMoreClick { _ ->
                            mViewModel.chooseActionBehavior(model.id)
                        }

                        onClick { _ ->
                            mActionModelList.value?.single { it.id == model.id }?.let {
                                mViewModel.onActionModelClick(it)
                            }
                        }
                    }
                }
            }
        })
    }
}