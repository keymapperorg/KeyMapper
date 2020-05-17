package io.github.sds100.keymapper.ui.fragment

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
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
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.SeekBarListItemModel
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentTriggerAndActionsBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.service.MyAccessibilityService.Companion.ACTION_RECORD_TRIGGER
import io.github.sds100.keymapper.service.MyAccessibilityService.Companion.ACTION_RECORD_TRIGGER_KEY
import io.github.sds100.keymapper.service.MyAccessibilityService.Companion.ACTION_RECORD_TRIGGER_TIMER_INCREMENTED
import io.github.sds100.keymapper.service.MyAccessibilityService.Companion.ACTION_STOP_RECORDING_TRIGGER
import io.github.sds100.keymapper.triggerKey
import io.github.sds100.keymapper.util.*
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
import splitties.resources.appInt
import splitties.resources.appStr
import splitties.snackbar.action
import splitties.snackbar.longSnack
import splitties.snackbar.snack
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
    private val mRecordTriggerTimeLeft = MutableLiveData(0)
    private val mRecordingTrigger = MutableLiveData(false)

    /**
     * Listens for key events from the accessibility service
     */
    private val mBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_RECORD_TRIGGER_KEY -> {
                    val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return

                    mViewModel.addTriggerKey(keyEvent)
                }

                ACTION_RECORD_TRIGGER_TIMER_INCREMENTED -> {
                    mRecordingTrigger.value = true
                    val timeLeft = intent.getIntExtra(MyAccessibilityService.EXTRA_TIME_LEFT, 5)

                    mRecordTriggerTimeLeft.value = timeLeft
                }

                ACTION_STOP_RECORDING_TRIGGER -> {
                    mRecordingTrigger.value = false
                }

                Intent.ACTION_INPUT_METHOD_CHANGED -> {
                    mViewModel.rebuildActionModels()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IntentFilter().apply {
            addAction(ACTION_RECORD_TRIGGER_KEY)
            addAction(ACTION_STOP_RECORDING_TRIGGER)
            addAction(ACTION_RECORD_TRIGGER_TIMER_INCREMENTED)
            addAction(Intent.ACTION_INPUT_METHOD_CHANGED)

            requireActivity().registerReceiver(mBroadcastReceiver, this)
        }
    }

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
                }
            }

            setOnAddActionClick {
                val direction = ConfigKeymapFragmentDirections.actionConfigKeymapFragmentToChooseActionFragment()
                findNavController().navigate(direction)
            }

            setOnTriggerOptionsClick {
                lifecycleScope.launch {
                    when (mViewModel.triggerMode.value) {
                        Trigger.SEQUENCE -> {
                            var timeout = mViewModel.triggerExtras.value?.find {
                                it.id == Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT
                            }!!.data.toInt()

                            val model = SeekBarListItemModel(
                                id = Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
                                title = appStr(R.string.seekbar_title_sequence_trigger_timeout),
                                min = appInt(R.integer.sequence_trigger_timeout_min),
                                max = appInt(R.integer.sequence_trigger_timeout_max),
                                stepSize = appInt(R.integer.sequence_trigger_timeout_step_size),
                                initialValue = timeout
                            )

                            timeout = requireActivity().seekBarAlertDialog(model)

                            mViewModel.setTriggerExtra(
                                id = Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
                                data = timeout.toString()
                            )
                        }

                        Trigger.PARALLEL -> {
                            if (!AppPreferences.shownDoublePressRestrictionWarning &&
                                mViewModel.triggerInParallel.value == true) {

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
                }
            }

            subscribeActionList()
            subscribeTriggerList()

            mViewModel.triggerMode.observe(viewLifecycleOwner) {
                epoxyRecyclerViewTriggers.requestModelBuild()
            }

            mViewModel.triggerInParallel.observe(viewLifecycleOwner) {

                /* when the user first chooses to make parallel a trigger, show a dialog informing them that
                the order in which they list the keys is the order in which they will need to be held down.
                 */
                if (it == true && mViewModel.triggerKeys.value?.size!! > 1
                    && !AppPreferences.shownParallelTriggerOrderDialog) {

                    lifecycleScope.launch {

                        val approvedWarning = requireActivity().alertDialog {
                            message = appStr(R.string.dialog_message_parallel_trigger_order)

                        }.showAndAwait(okValue = true, cancelValue = null, dismissValue = false)

                        if (approvedWarning) {
                            AppPreferences.shownParallelTriggerOrderDialog = true
                        }
                    }
                }
            }

            mViewModel.triggerInSequence.observe(viewLifecycleOwner) {
                if (it == true && mViewModel.triggerKeys.value?.size!! > 1
                    && !AppPreferences.shownSequenceTriggerExplanationDialog) {

                    lifecycleScope.launch {
                        val approvedWarning = requireActivity().alertDialog {
                            message = appStr(R.string.dialog_message_sequence_trigger_explanation)

                        }.showAndAwait(okValue = true, cancelValue = null, dismissValue = false)

                        if (approvedWarning) {
                            AppPreferences.shownSequenceTriggerExplanationDialog = true
                        }
                    }
                }
            }

            setOnRecordTriggerClick {
                val serviceEnabled = AccessibilityUtils.isServiceEnabled(requireContext())

                if (serviceEnabled) {
                    requireActivity().sendBroadcast(Intent(ACTION_RECORD_TRIGGER))
                } else {
                    coordinatorLayout.snack(R.string.error_accessibility_service_disabled_record_trigger) {
                        setAction(appStr(R.string.snackbar_fix)) {
                            AccessibilityUtils.enableService(requireContext())
                        }
                    }
                }
            }

            timeLeft = mRecordTriggerTimeLeft
            recordingTrigger = mRecordingTrigger

            return this.root
        }
    }

    override fun onResume() {
        super.onResume()

        mViewModel.rebuildActionModels()
    }

    override fun onDestroy() {
        super.onDestroy()

        requireActivity().unregisterReceiver(mBroadcastReceiver)
    }


    @SuppressLint("ClickableViewAccessibility")
    private fun FragmentTriggerAndActionsBinding.subscribeTriggerList() {
        mViewModel.triggerKeyModels.observe(viewLifecycleOwner) { triggerKeyList ->
            epoxyRecyclerViewTriggers.withModels {
                enableTriggerKeyDragging(this)

                triggerKeyList.forEachIndexed { index, model ->
                    triggerKey {
                        val triggerKey = mViewModel.triggerKeys.value?.get(index)

                        id(triggerKey?.uniqueId)
                        model(model)

                        triggerMode(mViewModel.triggerMode.value)
                        triggerKeyCount(triggerKeyList.size)
                        triggerKeyIndex(index)

                        onRemoveClick { _ ->
                            if (triggerKey != null) {
                                mViewModel.removeTriggerKey(triggerKey.keyCode)
                            }
                        }

                        onMoreClick { _ ->
                            if (mViewModel.triggerInSequence.value == true) {
                                lifecycleScope.launch {
                                    val newClickType = showClickTypeDialog()

                                    triggerKey?.apply {
                                        mViewModel.setTriggerKeyClickType(keyCode, newClickType)
                                    }
                                }
                            }
                        }

                        onDeviceClick { _ ->
                            lifecycleScope.launch {
                                val deviceId = showChooseDeviceDialog()

                                triggerKey?.apply {
                                    mViewModel.setTriggerKeyDevice(keyCode, deviceId)
                                }
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
                                coordinatorLayout.longSnack(model.errorMessage!!) {

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

    private suspend fun showChooseDeviceDialog() = suspendCoroutine<String> {
        requireActivity().alertDialog {

            val deviceIds = sequence {
                yield(Trigger.Key.DEVICE_ID_THIS_DEVICE)
                yield(Trigger.Key.DEVICE_ID_ANY_DEVICE)

                yieldAll(InputDeviceUtils.getExternalDeviceDescriptors())

            }.toList()

            val deviceLabels = sequence {
                yield(appStr(R.string.this_device))
                yield(appStr(R.string.any_device))

                yieldAll(InputDeviceUtils.getExternalDeviceNames())

            }.toList().toTypedArray()

            setItems(deviceLabels) { _, index ->
                val deviceId = deviceIds[index]

                it.resume(deviceId)
            }

            cancelButton()
            show()
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

    private fun FragmentTriggerAndActionsBinding.enableTriggerKeyDragging(controller: EpoxyController): ItemTouchHelper {
        return EpoxyTouchHelper.initDragging(controller)
            .withRecyclerView(epoxyRecyclerViewTriggers)
            .forVerticalList()
            .withTarget(TriggerKeyBindingModel_::class.java)
            .andCallbacks(object : EpoxyTouchHelper.DragCallbacks<TriggerKeyBindingModel_>() {

                override fun isDragEnabledForModel(model: TriggerKeyBindingModel_?): Boolean {
                    return mViewModel.triggerKeys.value?.size!! > 1
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