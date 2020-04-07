package io.github.sds100.keymapper.ui.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyTouchHelper
import com.google.android.material.card.MaterialCardView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.TriggerKeyBindingModel_
import io.github.sds100.keymapper.action
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentTriggerAndActionsBinding
import io.github.sds100.keymapper.triggerKey
import io.github.sds100.keymapper.util.availableFlags
import io.github.sds100.keymapper.util.observeLiveData
import io.github.sds100.keymapper.util.removeLiveData
import io.github.sds100.keymapper.util.result.RecoverableFailure
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.coroutines.showAndAwait
import splitties.alertdialog.appcompat.message
import splitties.alertdialog.appcompat.okButton
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag
import splitties.experimental.ExperimentalSplittiesApi
import splitties.resources.appStr
import splitties.snackbar.action
import splitties.snackbar.longSnack
import splitties.toast.toast
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by sds100 on 19/03/2020.
 */
@ExperimentalSplittiesApi
class TriggerAndActionsFragment : Fragment() {
    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap)
    private lateinit var mBinding: FragmentTriggerAndActionsBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentTriggerAndActionsBinding.inflate(inflater, container, false).apply {
            mBinding = this

            viewModel = mViewModel
            lifecycleOwner = viewLifecycleOwner

            findNavController().apply {
                currentBackStackEntry?.observeLiveData<Action>(
                    viewLifecycleOwner,
                    ChooseActionFragment.SAVED_STATE_KEY
                ) {

                    if (!mViewModel.addAction(it)) {
                        toast(R.string.error_action_exists)
                    }

                    currentBackStackEntry?.removeLiveData<Action>(ChooseActionFragment.SAVED_STATE_KEY)
                }
            }

            setOnAddActionClick {
                val direction = ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToChooseActionFragment()
                findNavController().navigate(direction)
            }

            setOnClickTypeClick {
                lifecycleScope.launch {
                    if (!AppPreferences.shownDoublePressRestrictionWarning && mViewModel.triggerInParallel.value == true) {

                        val approvedWarning = requireActivity().alertDialog {
                            message = appStr(R.string.dialog_message_double_press_restricted_to_single_key)

                        }.showAndAwait(okValue = true, cancelValue = null, dismissValue = false)

                        if (approvedWarning) {
                            AppPreferences.shownDoublePressRestrictionWarning = true
                        }
                    }

                    val newClickType = showClickTypeDialog()

                    mViewModel.setParallelTriggerClickType(newClickType)
                }
            }

            subscribeActionList()
            subscribeTriggerList()

            mViewModel.triggerMode.observe(viewLifecycleOwner) {
                epoxyRecyclerViewTriggers.requestModelBuild()
            }

            return this.root
        }
    }

    override fun onResume() {
        super.onResume()

        mViewModel.rebuildActionModels()
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun FragmentTriggerAndActionsBinding.subscribeTriggerList() {
        mViewModel.triggerKeyModels.observe(viewLifecycleOwner) { triggerKeyList ->
            epoxyRecyclerViewTriggers.withModels {
                enableTriggerKeyDragging(this)

                triggerKeyList.forEachIndexed { index, model ->
                    triggerKey {
                        val triggerKey = mViewModel.triggerKeys.value?.get(index)

                        id(model.name)
                        model(model)

                        triggerMode(mViewModel.triggerMode.value)
                        triggerKeyCount(triggerKeyList.size)
                        triggerKeyIndex(index)

                        onRemoveClick { _ ->
                            if (triggerKey != null) {
                                mViewModel.removeTriggerKey(triggerKey.keycode)
                            }
                        }

                        onMoreClick { _ ->
                            if (mViewModel.triggerInSequence.value == true) {
                                triggerKey?.chooseClickType()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FragmentTriggerAndActionsBinding.subscribeActionList() {
        mViewModel.actionModelList.observe(viewLifecycleOwner) { actionList ->
            epoxyRecyclerViewActions.withModels {
                val icons = sequence {
                    actionList.forEach {
                        yield(it.getIcon(requireContext()))
                    }
                }.toList()

                actionList.forEachIndexed { index, model ->
                    action {
                        val action = mViewModel.actionList.value?.get(index)

                        id(model.id)
                        model(model)
                        icon(icons[index])
                        flagsAreAvailable(action?.availableFlags?.isNotEmpty())

                        onRemoveClick { _ ->
                            mViewModel.removeAction(model.id)
                        }

                        onMoreClick { _ ->
                            action?.chooseFlags()
                        }

                        onClick { _ ->
                            if (model.hasError) {
                                coordinatorLayout.longSnack(model.failure!!.fullMessage) {

                                    //only add an action to fix the error if the error can be recovered from
                                    if (model.failure is RecoverableFailure) {
                                        action(R.string.snackbar_fix) {
                                            lifecycleScope.launch {
                                                model.failure.recover(requireActivity()) {
                                                    mViewModel.rebuildActionModels()
                                                }
                                            }
                                        }
                                    }

                                    show()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Trigger.Key.chooseClickType() {
        lifecycleScope.launch {
            val newClickType = showClickTypeDialog()

            mViewModel.setTriggerKeyClickType(keycode, newClickType)
        }
    }

    private suspend fun showClickTypeDialog() = suspendCoroutine<Int> {
        requireActivity().alertDialog {
            val labels = if (mViewModel.triggerInParallel.value == true) {
                arrayOf(
                    appStr(R.string.clicktype_short_press),
                    appStr(R.string.clicktype_long_press)
                )
            } else {
                arrayOf(
                    appStr(R.string.clicktype_short_press),
                    appStr(R.string.clicktype_long_press),
                    appStr(R.string.clicktype_double_press)
                )
            }

            setItems(labels) { _, index ->
                val clickType = when (index) {
                    0 -> Trigger.SHORT_PRESS
                    1 -> Trigger.LONG_PRESS
                    2 -> Trigger.DOUBLE_PRESS
                    else -> throw IllegalStateException("Can't find the click type at index: $index")
                }

                it.resume(clickType)
            }

            cancelButton()

            show()
        }
    }

    private fun Action.chooseFlags() {
        requireActivity().alertDialog {
            val flagIds = availableFlags
            val labels = sequence {
                flagIds.forEach { flagId ->
                    val label = appStr(Action.ACTION_FLAG_LABEL_MAP.getValue(flagId))
                    yield(label)
                }
            }.toList().toTypedArray()

            val checkedArray = sequence {
                flagIds.forEach {
                    yield(flags.hasFlag(it))
                }
            }.toList().toBooleanArray()

            setMultiChoiceItems(labels, checkedArray) { _, index, checked ->
                checkedArray[index] = checked
            }

            cancelButton()

            okButton {
                var flags = 0

                flagIds.forEachIndexed { index, flag ->
                    if (checkedArray[index]) {
                        flags = flags.withFlag(flag)
                    }
                }

                mViewModel.setActionFlags(uniqueId, flags)
            }

            show()
        }
    }

    private fun FragmentTriggerAndActionsBinding.enableTriggerKeyDragging(controller: EpoxyController): ItemTouchHelper {
        return EpoxyTouchHelper.initDragging(controller)
            .withRecyclerView(epoxyRecyclerViewTriggers)
            .forVerticalList()
            .withTarget(TriggerKeyBindingModel_::class.java)
            .andCallbacks(object : EpoxyTouchHelper.DragCallbacks<TriggerKeyBindingModel_>() {

                override fun isDragEnabledForModel(model: TriggerKeyBindingModel_?): Boolean {
                    return mViewModel.triggerKeys.value?.size!! > 1 && mViewModel.triggerInSequence.value == true
                }

                override fun onModelMoved(
                    fromPosition: Int,
                    toPosition: Int,
                    modelBeingMoved: TriggerKeyBindingModel_?,
                    itemView: View?
                ) {
                    mViewModel.moveTriggerKey(fromPosition, toPosition)
                }

                override fun onDragStarted(
                    model: TriggerKeyBindingModel_?,
                    itemView: View?,
                    adapterPosition: Int
                ) {
                    itemView?.findViewById<MaterialCardView>(R.id.cardView)?.isDragged = true
                }

                override fun onDragReleased(model: TriggerKeyBindingModel_?, itemView: View?) {
                    itemView?.findViewById<MaterialCardView>(R.id.cardView)?.isDragged = false
                }
            })
    }
}