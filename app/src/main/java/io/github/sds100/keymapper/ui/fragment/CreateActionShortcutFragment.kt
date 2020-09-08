package io.github.sds100.keymapper.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.map
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.action
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.ActionBehavior
import io.github.sds100.keymapper.data.viewmodel.CreateActionShortcutViewModel
import io.github.sds100.keymapper.databinding.FragmentCreateActionShortcutBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.util.*
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.positiveButton

/**
 * Created by sds100 on 08/09/20.
 */

class CreateActionShortcutFragment : Fragment() {

    private val mViewModel by navGraphViewModels<CreateActionShortcutViewModel>(R.id.nav_action_shortcut) {
        InjectorUtils.provideCreateActionShortcutViewModel()
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(ChooseActionFragment.REQUEST_KEY) { _, result ->
            val action = result.getSerializable(ChooseActionFragment.EXTRA_ACTION) as Action
            mViewModel.addAction(action)
        }

        setFragmentResultListener(ActionBehaviorFragment.REQUEST_KEY) { _, result ->
            mViewModel.setActionBehavior(
                result.getSerializable(ActionBehaviorFragment.EXTRA_ACTION_BEHAVIOR) as ActionBehavior)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentCreateActionShortcutBinding.inflate(inflater).apply {
            lifecycleOwner = viewLifecycleOwner
            viewModel = mViewModel

            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                showOnBackPressedWarning()
            }

            appBar.setNavigationOnClickListener {
                showOnBackPressedWarning()
            }

            mViewModel.actionList.observe(viewLifecycleOwner, Observer {
                appBar.menu?.findItem(R.id.action_done)?.isVisible = it.isNotEmpty()
            })

            mViewModel.chooseActionEvent.observe(viewLifecycleOwner, EventObserver {
                findNavController().navigate(
                    CreateActionShortcutFragmentDirections.actionActionShortcutListFragmentToChooseActionFragment())
            })

            mViewModel.testAction.observe(viewLifecycleOwner, EventObserver {
                if (AccessibilityUtils.isServiceEnabled(requireContext())) {

                    requireContext().sendPackageBroadcast(MyAccessibilityService.ACTION_TEST_ACTION,
                        bundleOf(MyAccessibilityService.EXTRA_ACTION to it))

                } else {
                    mViewModel.promptToEnableAccessibilityService.value = Event(Unit)
                }
            })

            mViewModel.chooseActionBehavior.observe(viewLifecycleOwner, EventObserver {
                val direction =
                    CreateActionShortcutFragmentDirections.actionActionShortcutListFragmentToActionBehaviorFragment(it)

                findNavController().navigate(direction)
            })

            subscribeActionList()

            return this.root
        }
    }

    private fun showOnBackPressedWarning() {
        requireContext().alertDialog {
            messageResource = R.string.dialog_message_are_you_sure_want_to_leave_without_saving

            positiveButton(R.string.pos_yes) {
                requireActivity().finish()
            }

            cancelButton()
            show()
        }
    }

    private fun FragmentCreateActionShortcutBinding.subscribeActionList() {
        mActionModelList.observe(viewLifecycleOwner, Observer { actionList ->
            epoxyRecyclerView.withModels {

                actionList.forEachIndexed { _, model ->
                    action {
                        id(model.id)
                        model(model)
                        icon(model.icon)

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