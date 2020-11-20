package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyTouchHelper
import com.google.android.material.card.MaterialCardView
import io.github.sds100.keymapper.ActionBindingModel_
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.action
import io.github.sds100.keymapper.data.model.ActionModel
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

    private val mActionListController = ActionListController()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentActionsBinding.inflate(inflater, container, false).apply {
            viewModel = mViewModel
            lifecycleOwner = viewLifecycleOwner

            mViewModel.chooseAction.observe(viewLifecycleOwner, EventObserver {
                val direction = ConfigKeymapFragmentDirections
                    .actionConfigKeymapFragmentToChooseActionFragment(ConfigKeymapFragment.CHOOSE_ACTION_REQUEST_KEY)
                findNavController().navigate(direction)
            })

            subscribeActionList()
            epoxyRecyclerViewActions.adapter = mActionListController.adapter

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

            mViewModel.buildActionModelList.observe(viewLifecycleOwner, EventObserver { actionList ->
                lifecycleScope.launchWhenStarted {
                    val deviceInfoList = mViewModel.getDeviceInfoList()

                    val models = sequence {
                        actionList.forEach {
                            yield(it.buildModel(requireContext(), deviceInfoList))
                        }
                    }.toList()

                    mViewModel.setActionModels(models)
                }
            })

            return this.root
        }
    }

    override fun onResume() {
        super.onResume()

        mViewModel.rebuildActionModels()
    }

    private fun FragmentActionsBinding.subscribeActionList() {
        mViewModel.actionModelList.observe(viewLifecycleOwner, { actionList ->
            enableActionDragging(mActionListController)

            mActionListController.modelList = actionList
        })
    }

    private fun FragmentActionsBinding.enableActionDragging(controller: EpoxyController): ItemTouchHelper {
        return EpoxyTouchHelper.initDragging(controller)
            .withRecyclerView(epoxyRecyclerViewActions)
            .forVerticalList()
            .withTarget(ActionBindingModel_::class.java)
            .andCallbacks(object : EpoxyTouchHelper.DragCallbacks<ActionBindingModel_>() {

                override fun isDragEnabledForModel(model: ActionBindingModel_?): Boolean {
                    return mViewModel.actionList.value?.size!! > 1
                }

                override fun onModelMoved(
                    fromPosition: Int,
                    toPosition: Int,
                    modelBeingMoved: ActionBindingModel_?,
                    itemView: View?
                ) {
                    mViewModel.moveAction(fromPosition, toPosition)
                }

                override fun onDragStarted(
                    model: ActionBindingModel_?,
                    itemView: View?,
                    adapterPosition: Int
                ) {
                    itemView?.findViewById<MaterialCardView>(R.id.cardView)?.isDragged = true
                }

                override fun onDragReleased(model: ActionBindingModel_?, itemView: View?) {
                    itemView?.findViewById<MaterialCardView>(R.id.cardView)?.isDragged = false
                }
            })
    }

    private inner class ActionListController : EpoxyController() {
        var modelList: List<ActionModel> = listOf()
            set(value) {
                requestModelBuild()
                field = value
            }

        override fun buildModels() {
            modelList.forEachIndexed { index, model ->
                action {
                    id(model.id)
                    model(model)
                    icon(model.icon)
                    actionCount(modelList.size)

                    onRemoveClick { _ ->
                        mViewModel.removeAction(model.id)
                    }

                    onMoreClick { _ ->
                        mViewModel.chooseActionBehavior(model.id)
                    }

                    onClick { _ ->
                        mViewModel.onActionModelClick(model.id)
                    }
                }
            }
        }
    }
}