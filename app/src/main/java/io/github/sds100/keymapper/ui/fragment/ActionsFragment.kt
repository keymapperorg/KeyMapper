package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.map
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.action
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentActionsBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.*
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.okButton
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag
import splitties.toast.toast

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

            findNavController().observeCurrentDestinationEvent<Action>(
                viewLifecycleOwner,
                ChooseActionFragment.SAVED_STATE_KEY
            ) {

                if (!mViewModel.addAction(it)) {
                    toast(R.string.error_action_exists)
                }
            }

            mViewModel.chooseAction.observe(viewLifecycleOwner, EventObserver {
                val direction = ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToChooseActionFragment()
                findNavController().navigate(direction)
            })

            subscribeActionList()

            mViewModel.testAction.observe(viewLifecycleOwner, EventObserver {
                if (AccessibilityUtils.isServiceEnabled(requireContext())) {
                    MyAccessibilityService.provideBus().value = Event(MyAccessibilityService.EVENT_TEST_ACTION to it)
                } else {
                    mViewModel.promptToEnableAccessibilityService.value = Event(Unit)
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
        mActionModelList.observe(viewLifecycleOwner) { actionList ->
            epoxyRecyclerViewActions.withModels {

                actionList.forEachIndexed { index, model ->
                    action {
                        val action = mViewModel.actionList.value?.get(index)

                        id(model.id)
                        model(model)
                        icon(model.icon)
                        flagsAreAvailable(action?.availableFlags?.isNotEmpty())

                        onRemoveClick { _ ->
                            mViewModel.removeAction(model.id)
                        }

                        onMoreClick { _ ->
                            action?.chooseFlags()
                        }

                        onClick { _ ->
                            mActionModelList.value?.single { it.id == model.id }?.let {
                                mViewModel.onActionModelClick(it)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun Action.chooseFlags() {
        requireActivity().alertDialog {
            val flagIds = availableFlags
            val labels = sequence {
                flagIds.forEach { flagId ->
                    val label = str(Action.ACTION_FLAG_LABEL_MAP.getValue(flagId))
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

            okButton {
                var flags = 0

                flagIds.forEachIndexed { index, flag ->
                    if (checkedArray[index]) {
                        flags = flags.withFlag(flag)
                    }
                }

                mViewModel.setActionFlags(uniqueId, flags)
            }

            cancelButton()
            show()
        }
    }
}