package io.github.sds100.keymapper.ui.fragment

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.google.gson.Gson
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.action
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.options.KeymapActionOptions
import io.github.sds100.keymapper.data.viewmodel.CreateActionShortcutViewModel
import io.github.sds100.keymapper.databinding.FragmentCreateActionShortcutBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.ui.activity.LaunchActionShortcutActivity
import io.github.sds100.keymapper.ui.fragment.keymap.KeymapActionOptionsFragment
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.delegate.RecoverFailureDelegate
import io.github.sds100.keymapper.util.result.RecoverableFailure
import io.github.sds100.keymapper.util.result.getFullMessage
import io.github.sds100.keymapper.util.result.valueOrNull
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import splitties.alertdialog.appcompat.messageResource
import splitties.alertdialog.appcompat.positiveButton
import splitties.snackbar.action
import splitties.snackbar.longSnack
import java.util.*

/**
 * Created by sds100 on 08/09/20.
 */

class CreateActionShortcutFragment : Fragment() {
    companion object {
        private const val CHOOSE_ACTION_REQUEST_KEY = "request_choose_action"
    }

    private val mViewModel by navGraphViewModels<CreateActionShortcutViewModel>(R.id.nav_action_shortcut) {
        InjectorUtils.provideCreateActionShortcutViewModel(requireContext())
    }

    private lateinit var mRecoverFailureDelegate: RecoverFailureDelegate

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mRecoverFailureDelegate = RecoverFailureDelegate(
            "CreateActionShortcutFragment",
            requireActivity().activityResultRegistry,
            this) {

            mViewModel.rebuildActionModels()
        }

        setFragmentResultListener(CHOOSE_ACTION_REQUEST_KEY) { _, result ->
            val action = result.getSerializable(ChooseActionFragment.EXTRA_ACTION) as Action
            mViewModel.addAction(action)
        }

        setFragmentResultListener(KeymapActionOptionsFragment.REQUEST_KEY) { _, result ->
            mViewModel.setActionBehavior(
                result.getSerializable(BaseOptionsFragment.EXTRA_OPTIONS) as KeymapActionOptions)
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
                val direction = CreateActionShortcutFragmentDirections
                    .actionActionShortcutListFragmentToChooseActionFragment(CHOOSE_ACTION_REQUEST_KEY)
                findNavController().navigate(direction)
            })

            mViewModel.testAction.observe(viewLifecycleOwner, EventObserver {
                if (AccessibilityUtils.isServiceEnabled(requireContext())) {

                    requireContext().sendPackageBroadcast(MyAccessibilityService.ACTION_TEST_ACTION,
                        bundleOf(MyAccessibilityService.EXTRA_ACTION to it))

                } else {
                    mViewModel.promptToEnableAccessibilityService.value = Event(Unit)
                }
            })

            mViewModel.editActionOptions.observe(viewLifecycleOwner, EventObserver {
                val direction =
                    CreateActionShortcutFragmentDirections.actionActionShortcutListFragmentToActionBehaviorFragment(it)

                findNavController().navigate(direction)
            })

            mViewModel.showFixPrompt.observe(viewLifecycleOwner, EventObserver {
                coordinatorLayout.longSnack(it.getFullMessage(requireContext())) {

                    //only add an action to fix the error if the error can be recovered from
                    if (it is RecoverableFailure) {
                        action(R.string.snackbar_fix) {
                            mRecoverFailureDelegate.recover(requireActivity(), it)
                        }
                    }

                    show()
                }
            })

            subscribeActionList()

            appBar.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_done -> {
                        lifecycleScope.launchWhenResumed {
                            ShortcutInfoCompat.Builder(requireContext(), UUID.randomUUID().toString()).apply {
                                val icon = createShortcutIcon()
                                val shortcutLabel = createShortcutLabel()

                                setIcon(icon)
                                setShortLabel(shortcutLabel)

                                Intent(requireContext(), LaunchActionShortcutActivity::class.java).apply {
                                    action = MyAccessibilityService.ACTION_PERFORM_ACTIONS

                                    putExtra(MyAccessibilityService.EXTRA_ACTION_LIST,
                                        Gson().toJson(mViewModel.actionList.value))

                                    setIntent(this)
                                }

                                ShortcutManagerCompat.createShortcutResultIntent(requireContext(), this.build()).apply {
                                    requireActivity().setResult(Activity.RESULT_OK, this)
                                    requireActivity().finish()
                                }
                            }
                        }

                        true
                    }

                    else -> false
                }
            }

            return this.root
        }
    }

    private fun createShortcutIcon(): IconCompat {
        if (mViewModel.actionList.value?.size == 1) {
            val action = mViewModel.actionList.value!![0]

            action.getIcon(requireContext()).valueOrNull()?.let {
                val bitmap = it.toBitmap()

                return IconCompat.createWithBitmap(bitmap)
            }
        }

        return IconCompat.createWithResource(
            requireContext(),
            R.mipmap.ic_launcher_round)
    }

    private suspend fun createShortcutLabel(): String {
        if (mViewModel.actionList.value?.size == 1) {
            val action = mViewModel.actionList.value!![0]

            action.getTitle(requireContext(), mViewModel.getDeviceInfoList()).valueOrNull()?.let {
                return it
            }
        }

        return requireActivity().editTextStringAlertDialog(
            str(R.string.hint_shortcut_name),
            allowEmpty = false
        )
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

        mViewModel.actionModelList.observe(viewLifecycleOwner, { actionList ->
            epoxyRecyclerView.withModels {

                actionList.forEach { model ->
                    action {
                        id(model.id)
                        model(model)

                        onRemoveClick { _ ->
                            mViewModel.removeAction(model.id)
                        }

                        onMoreClick { _ ->
                            mViewModel.editActionOptions(model.id)
                        }

                        onClick { _ ->
                            mViewModel.onActionModelClick(model.id)
                        }
                    }
                }
            }
        })
    }
}