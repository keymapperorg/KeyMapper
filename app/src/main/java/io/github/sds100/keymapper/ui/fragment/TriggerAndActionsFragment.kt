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
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentTriggerAndActionsBinding
import io.github.sds100.keymapper.util.getAvailableFlags
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.okButton
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag
import splitties.experimental.ExperimentalSplittiesApi
import splitties.resources.appStr

/**
 * Created by sds100 on 19/03/2020.
 */
@ExperimentalSplittiesApi
class TriggerAndActionsFragment : Fragment() {
    private val mConfigKeymapViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentTriggerAndActionsBinding.inflate(inflater, container, false).apply {

            viewModel = mConfigKeymapViewModel
            lifecycleOwner = viewLifecycleOwner

            setOnAddActionClick {
                val direction = ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToChooseActionFragment()
                findNavController().navigate(direction)
            }

            subscribeActionList()

            return this.root
        }
    }

    private fun FragmentTriggerAndActionsBinding.subscribeActionList() {
        mConfigKeymapViewModel.actionModelList.observe(viewLifecycleOwner) { actionList ->
            epoxyRecyclerViewActions.withModels {
                actionList.forEachIndexed { index, model ->
                    action {
                        val action = mConfigKeymapViewModel.actionList.value?.get(index)

                        id(model.id)
                        model(model)
                        flagsAreAvailable(action?.getAvailableFlags()?.isNotEmpty())

                        onRemoveClick { _ ->
                            mConfigKeymapViewModel.removeAction(model.id)
                        }

                        onMoreClick { _ ->
                            requireActivity().alertDialog {
                                action ?: return@alertDialog

                                val flagIds = action.getAvailableFlags()
                                val labels = sequence {
                                    flagIds.forEach { flagId ->
                                        val label = appStr(Action.ACTION_FLAG_LABEL_MAP.getValue(flagId))
                                        yield(label)
                                    }
                                }.toList().toTypedArray()

                                val checkedArray = sequence {
                                    flagIds.forEach {
                                        yield(action.flags.hasFlag(it))
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

                                    mConfigKeymapViewModel.setActionFlags(action.uniqueId, flags)
                                }
                            }.show()
                        }
                    }
                }
            }
        }
    }
}