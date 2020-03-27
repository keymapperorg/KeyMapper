package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.action
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentTriggerAndActionsBinding
import io.github.sds100.keymapper.triggerKey
import io.github.sds100.keymapper.util.getAvailableFlags
import io.github.sds100.keymapper.util.observeLiveData
import io.github.sds100.keymapper.util.removeLiveData
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.okButton
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag
import splitties.experimental.ExperimentalSplittiesApi
import splitties.resources.appStr
import splitties.toast.toast

/**
 * Created by sds100 on 19/03/2020.
 */
@ExperimentalSplittiesApi
class TriggerAndActionsFragment : Fragment() {
    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentTriggerAndActionsBinding.inflate(inflater, container, false).apply {

            viewModel = mViewModel
            lifecycleOwner = viewLifecycleOwner

            findNavController().currentBackStackEntry?.observeLiveData<Action>(viewLifecycleOwner, ChooseActionFragment.SAVED_STATE_KEY) {

                if (!mViewModel.addAction(it)) {
                    toast(R.string.error_action_exists)
                }

                findNavController().currentBackStackEntry?.removeLiveData<Action>(ChooseActionFragment.SAVED_STATE_KEY)
            }

            setOnAddActionClick {
                val direction = ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToChooseActionFragment()
                findNavController().navigate(direction)
            }

            subscribeActionList()
            subscribeTriggerList()

            mViewModel.triggerMode.observe(viewLifecycleOwner) {
                epoxyRecyclerViewTriggers.requestModelBuild()
            }

            return this.root
        }
    }

    private fun FragmentTriggerAndActionsBinding.subscribeTriggerList() {
        mViewModel.triggerKeyModels.observe(viewLifecycleOwner) { triggerKeyList ->
            epoxyRecyclerViewTriggers.withModels {
                triggerKeyList.forEachIndexed { index, model ->
                    triggerKey {
                        id(model.name)
                        model(model)

                        triggerMode(mViewModel.triggerMode.value)
                        triggerKeyCount(triggerKeyList.size)
                        triggerKeyIndex(index)
                    }
                }
            }
        }
    }

    private fun FragmentTriggerAndActionsBinding.subscribeActionList() {
        mViewModel.actionModelList.observe(viewLifecycleOwner) { actionList ->
            epoxyRecyclerViewActions.withModels {
                actionList.forEachIndexed { index, model ->
                    action {
                        val action = mViewModel.actionList.value?.get(index)

                        id(model.id)
                        model(model)
                        flagsAreAvailable(action?.getAvailableFlags()?.isNotEmpty())

                        onRemoveClick { _ ->
                            mViewModel.removeAction(model.id)
                        }

                        onMoreClick { _ ->
                            action?.chooseFlags()
                        }
                    }
                }
            }
        }
    }

    private fun Action.chooseFlags() {
        requireActivity().alertDialog {
            val flagIds = getAvailableFlags()
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
}