package io.github.sds100.keymapper.actions

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyRecyclerView
import com.airbnb.epoxy.EpoxyTouchHelper
import com.google.android.material.card.MaterialCardView
import io.github.sds100.keymapper.ActionBindingModel_
import io.github.sds100.keymapper.NavAppDirections
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.action
import io.github.sds100.keymapper.databinding.FragmentActionListBinding
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.launchRepeatOnLifecycle
import io.github.sds100.keymapper.util.ui.RecyclerViewFragment
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest

/**
 * Created by sds100 on 22/11/20.
 */
abstract class ConfigActionsFragment<A : Action>
    : RecyclerViewFragment<ActionListItem, FragmentActionListBinding>() {

    abstract val configActionsViewModel: ConfigActionsViewModel<A, *>

    override val listItems: Flow<State<List<ActionListItem>>>
        get() = configActionsViewModel.state

    private val actionListController = ActionListController()

    override fun bind(inflater: LayoutInflater, container: ViewGroup?) =
        FragmentActionListBinding.inflate(inflater, container, false).apply {
            lifecycleOwner = viewLifecycleOwner
        }

    override fun populateList(recyclerView: EpoxyRecyclerView, listItems: List<ActionListItem>) {
        binding.enableActionDragging(actionListController)

        actionListController.state = listItems
    }

    override fun subscribeUi(binding: FragmentActionListBinding) {
        binding.epoxyRecyclerView.adapter = actionListController.adapter

        binding.setOnAddActionClick {
            configActionsViewModel.onAddActionClick()
        }

        viewLifecycleOwner.launchRepeatOnLifecycle(Lifecycle.State.RESUMED) {
            configActionsViewModel.navigateToShizukuSetup.collectLatest {
                findNavController().navigate(NavAppDirections.toShizukuSettingsFragment())
            }
        }
    }

    override fun getRecyclerView(binding: FragmentActionListBinding) = binding.epoxyRecyclerView
    override fun getProgressBar(binding: FragmentActionListBinding) = binding.progressBar
    override fun getEmptyListPlaceHolderTextView(binding: FragmentActionListBinding) =
        binding.emptyListPlaceHolder


    private fun FragmentActionListBinding.enableActionDragging(
        controller: EpoxyController
    ): ItemTouchHelper {

        return EpoxyTouchHelper.initDragging(controller)
            .withRecyclerView(epoxyRecyclerView)
            .forVerticalList()
            .withTarget(ActionBindingModel_::class.java)
            .andCallbacks(object : EpoxyTouchHelper.DragCallbacks<ActionBindingModel_>() {

                override fun isDragEnabledForModel(model: ActionBindingModel_?): Boolean {
                    return model?.state()?.dragAndDrop ?: false
                }

                override fun onModelMoved(
                    fromPosition: Int,
                    toPosition: Int,
                    modelBeingMoved: ActionBindingModel_?,
                    itemView: View?
                ) {
                    configActionsViewModel.moveAction(fromPosition, toPosition)
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
        var state: List<ActionListItem> = listOf()
            set(value) {
                field = value
                requestModelBuild()
            }

        override fun buildModels() {
            state.forEach {
                action {
                    id(it.id)
                    state(it)

                    onRemoveClick { _ ->
                        configActionsViewModel.onRemoveClick(it.id)
                    }

                    onEditClick { _ ->
                        configActionsViewModel.editAction(it.id)
                    }

                    onClick { _ ->
                        configActionsViewModel.onModelClick(it.id)
                    }
                }
            }
        }
    }
}