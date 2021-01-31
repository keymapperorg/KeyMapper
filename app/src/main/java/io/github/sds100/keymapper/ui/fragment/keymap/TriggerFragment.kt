package io.github.sds100.keymapper.ui.fragment.keymap

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyTouchHelper
import com.google.android.material.card.MaterialCardView
import com.google.android.material.radiobutton.MaterialRadioButton
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.TriggerKeyBindingModel_
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.model.TriggerKeyModel
import io.github.sds100.keymapper.data.showDeviceDescriptors
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.data.viewmodel.TriggerViewModel
import io.github.sds100.keymapper.databinding.FragmentTriggerBinding
import io.github.sds100.keymapper.globalPreferences
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.triggerKey
import io.github.sds100.keymapper.util.*
import io.github.sds100.keymapper.util.result.onSuccess
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.*
import splitties.alertdialog.appcompat.coroutines.showAndAwait
import splitties.experimental.ExperimentalSplittiesApi
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by sds100 on 25/11/20.
 */

class TriggerFragment : Fragment() {

    class Info : FragmentInfo(
        R.string.trigger_header,
        R.string.url_trigger_guide,
        { TriggerFragment() }
    )

    private lateinit var binding: FragmentTriggerBinding

    private val triggerViewModel: TriggerViewModel by lazy {
        navGraphViewModels<ConfigKeymapViewModel>(R.id.nav_config_keymap) {
            InjectorUtils.provideConfigKeymapViewModel(requireContext())
        }.value.triggerViewModel
    }

    /**
     * Listens for key events from the accessibility service
     */
    private val broadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            intent ?: return

            when (intent.action) {

                MyAccessibilityService.ACTION_RECORDED_TRIGGER_KEY -> {
                    intent.getParcelableExtra<RecordedTriggerKeyEvent>(MyAccessibilityService.EXTRA_RECORDED_TRIGGER_KEY_EVENT)
                        ?.let { event ->
                            if (!successfullyRecordedTrigger) {
                                successfullyRecordedTrigger = true
                            }

                            lifecycleScope.launch {
                                val deviceName = event.deviceName
                                val deviceDescriptor = event.deviceDescriptor
                                val isExternal = event.isExternal

                                triggerViewModel.addTriggerKey(event.keyCode, deviceDescriptor, deviceName, isExternal)
                            }
                        }
                }

                MyAccessibilityService.ACTION_RECORD_TRIGGER_TIMER_INCREMENTED -> {
                    val timeLeft = intent.getIntExtra(MyAccessibilityService.EXTRA_TIME_LEFT, -1)

                    if (timeLeft != -1) {
                        triggerViewModel.recordingTrigger.value = true
                        triggerViewModel.recordTriggerTimeLeft.value = timeLeft
                    }
                }

                MyAccessibilityService.ACTION_STOPPED_RECORDING_TRIGGER -> {
                    val stoppedEarly = triggerViewModel.recordTriggerTimeLeft.value?.let { it > 1 }
                        ?: true

                    if (!stoppedEarly) {
                        recordingTriggerCount++
                    }

                    triggerViewModel.recordingTrigger.value = false

                    if (recordingTriggerCount >= 2 && !successfullyRecordedTrigger) {
                        requireContext().alertDialog {
                            titleResource = R.string.dialog_title_cant_record_trigger
                            messageResource = R.string.dialog_message_cant_record_trigger

                            okButton()

                            show()
                        }
                    }
                }
            }
        }
    }

    /**
     * The number of times the user has attempted to record a trigger.
     */
    private var recordingTriggerCount = 0

    /**
     * Whether the user has successfully recorded a trigger.
     */
    private var successfullyRecordedTrigger = false

    private val triggerKeyController = TriggerKeyController()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        IntentFilter().apply {
            addAction(Intent.ACTION_INPUT_METHOD_CHANGED)
            addAction(MyAccessibilityService.ACTION_RECORD_TRIGGER_TIMER_INCREMENTED)
            addAction(MyAccessibilityService.ACTION_RECORDED_TRIGGER_KEY)
            addAction(MyAccessibilityService.ACTION_STOPPED_RECORDING_TRIGGER)

            requireContext().registerReceiver(broadcastReceiver, this)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        FragmentTriggerBinding.inflate(inflater, container, false).apply {
            binding = this
            lifecycleOwner = viewLifecycleOwner

            return this.root
        }
    }

    @ExperimentalSplittiesApi
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.viewModel = triggerViewModel

        binding.subscribeTriggerList()

        binding.epoxyRecyclerViewTriggers.adapter = triggerKeyController.adapter

        triggerViewModel.mode.observe(viewLifecycleOwner) {
            triggerKeyController.requestModelBuild()
        }

        triggerViewModel.eventStream.observe(viewLifecycleOwner, { event ->
            when (event) {
                is StartRecordingTriggerInService -> {
                    val serviceEnabled = AccessibilityUtils.isServiceEnabled(requireContext())

                    if (serviceEnabled) {
                        requireContext().sendPackageBroadcast(MyAccessibilityService.ACTION_RECORD_TRIGGER)

                    } else {
                        triggerViewModel.promptToEnableAccessibilityService()
                    }
                }

                is BuildTriggerKeyModels -> viewLifecycleScope.launchWhenResumed {
                    val deviceInfoList = triggerViewModel.getDeviceInfoList()

                    val modelList = sequence {
                        event.source.forEach {
                            val model = it.buildModel(requireContext(), deviceInfoList)
                            yield(model)
                        }
                    }.toList()

                    triggerViewModel.setModelList(modelList)
                }

                is OkDialog -> lifecycleScope.launchWhenResumed {
                    val approvedWarning = requireContext().alertDialog {
                        message = str(event.message)

                    }.showAndAwait(okValue = true, cancelValue = null, dismissValue = false)

                    if (approvedWarning) {
                        triggerViewModel.onDialogResponse(event.responseKey,
                            DialogResponse.POSITIVE)
                    }
                }

                is StopRecordingTriggerInService -> {
                    val serviceEnabled = AccessibilityUtils.isServiceEnabled(requireContext())

                    if (serviceEnabled) {
                        stopRecordingTrigger()
                    } else {
                        triggerViewModel.promptToEnableAccessibilityService()
                    }
                }

                is EnableCapsLockKeyboardLayoutPrompt -> requireContext().alertDialog {
                    messageResource = R.string.dialog_message_enable_physical_keyboard_caps_lock_a_keyboard_layout

                    okButton()

                    show()
                }

                is EditTriggerKeyOptions -> {
                    val direction = ConfigKeymapFragmentDirections.actionTriggerKeyOptionsFragment(event.options)
                    findNavController().navigate(direction)
                }
            }
        })

        binding.radioButtonShortPress.setOnClickListener { radioButton ->
            if ((radioButton as MaterialRadioButton).isChecked) {
                triggerViewModel.setParallelTriggerClickType(Trigger.SHORT_PRESS)
            }
        }

        binding.radioButtonLongPress.setOnClickListener { radioButton ->
            if ((radioButton as MaterialRadioButton).isChecked) {
                triggerViewModel.setParallelTriggerClickType(Trigger.LONG_PRESS)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        triggerViewModel.rebuildModels()
    }

    override fun onPause() {
        super.onPause()

        stopRecordingTrigger()
    }

    override fun onDestroy() {
        requireContext().unregisterReceiver(broadcastReceiver)

        super.onDestroy()
    }

    private fun stopRecordingTrigger() {
        requireContext().sendPackageBroadcast(MyAccessibilityService.ACTION_STOP_RECORDING_TRIGGER)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun FragmentTriggerBinding.subscribeTriggerList() {
        triggerViewModel.modelList.observe(viewLifecycleOwner, { triggerKeyList ->

            viewLifecycleScope.launchWhenResumed {
                enableTriggerKeyDragging(triggerKeyController)

                when (triggerKeyList) {
                    is Data -> triggerKeyController.modelList = triggerKeyList.data
                    else -> triggerKeyController.modelList = emptyList()
                }
            }
        })
    }

    private suspend fun showChooseDeviceDialog() = suspendCoroutine<String> {
        requireContext().alertDialog {

            val deviceIds = sequence {
                yield(Trigger.Key.DEVICE_ID_THIS_DEVICE)
                yield(Trigger.Key.DEVICE_ID_ANY_DEVICE)

                yieldAll(InputDeviceUtils.getExternalDeviceDescriptors())

            }.toList()

            val deviceLabels = sequence {
                yield(str(R.string.this_device))
                yield(str(R.string.any_device))

                if (globalPreferences.showDeviceDescriptors.firstBlocking()) {
                    InputDeviceUtils.getExternalDeviceDescriptors().forEach { descriptor ->
                        InputDeviceUtils.getName(descriptor).onSuccess { name ->
                            yield("$name ${descriptor.substring(0..4)}")
                        }
                    }
                } else {
                    yieldAll(InputDeviceUtils.getExternalDeviceNames())
                }

            }.toList().toTypedArray()

            setItems(deviceLabels) { _, index ->
                val deviceId = deviceIds[index]

                it.resume(deviceId)
            }

            cancelButton()
            show()
        }
    }

    private fun FragmentTriggerBinding.enableTriggerKeyDragging(controller: EpoxyController): ItemTouchHelper {
        return EpoxyTouchHelper.initDragging(controller)
            .withRecyclerView(epoxyRecyclerViewTriggers)
            .forVerticalList()
            .withTarget(TriggerKeyBindingModel_::class.java)
            .andCallbacks(object : EpoxyTouchHelper.DragCallbacks<TriggerKeyBindingModel_>() {

                override fun isDragEnabledForModel(model: TriggerKeyBindingModel_?): Boolean {
                    return triggerViewModel.keys.value?.size!! > 1
                }

                override fun onModelMoved(
                    fromPosition: Int,
                    toPosition: Int,
                    modelBeingMoved: TriggerKeyBindingModel_?,
                    itemView: View?
                ) {
                    triggerViewModel.moveTriggerKey(fromPosition, toPosition)
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

    private inner class TriggerKeyController : EpoxyController() {
        var modelList: List<TriggerKeyModel> = listOf()
            set(value) {
                requestModelBuild()
                field = value
            }

        override fun buildModels() {
            modelList.forEachIndexed { index, model ->
                triggerKey {
                    id(model.id)
                    model(model)

                    triggerMode(triggerViewModel.mode.value)
                    triggerKeyCount(modelList.size)
                    triggerKeyIndex(index)

                    onRemoveClick { _ ->
                        triggerViewModel.removeTriggerKey(model.id)
                    }

                    onMoreClick { _ ->
                        triggerViewModel.editTriggerKeyOptions(model.id)
                    }

                    onDeviceClick { _ ->
                        viewLifecycleScope.launch {
                            val deviceId = showChooseDeviceDialog()

                            triggerViewModel.setTriggerKeyDevice(model.id, deviceId)
                        }
                    }
                }
            }
        }
    }
}