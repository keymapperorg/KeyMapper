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
import androidx.annotation.VisibleForTesting
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.observe
import androidx.navigation.navGraphViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyTouchHelper
import com.google.android.material.card.MaterialCardView
import io.github.sds100.keymapper.R
import io.github.sds100.keymapper.TriggerKeyBindingModel_
import io.github.sds100.keymapper.data.model.Extra
import io.github.sds100.keymapper.data.model.SeekBarListItemModel
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.data.viewmodel.ConfigKeymapViewModel
import io.github.sds100.keymapper.databinding.FragmentTriggerBinding
import io.github.sds100.keymapper.service.MyAccessibilityService
import io.github.sds100.keymapper.service.MyAccessibilityService.Companion.ACTION_RECORD_TRIGGER_KEY
import io.github.sds100.keymapper.service.MyAccessibilityService.Companion.ACTION_RECORD_TRIGGER_TIMER_INCREMENTED
import io.github.sds100.keymapper.service.MyAccessibilityService.Companion.ACTION_STOP_RECORDING_TRIGGER
import io.github.sds100.keymapper.triggerKey
import io.github.sds100.keymapper.util.*
import kotlinx.coroutines.launch
import splitties.alertdialog.appcompat.alertDialog
import splitties.alertdialog.appcompat.cancelButton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Created by sds100 on 19/03/2020.
 */
class TriggerFragment(private val mKeymapId: Long) : Fragment() {
    private val mViewModel: ConfigKeymapViewModel by navGraphViewModels(R.id.nav_config_keymap) {
        InjectorUtils.provideConfigKeymapViewModel(requireContext(), mKeymapId)
    }

    private lateinit var mBinding: FragmentTriggerBinding

    /**
     * Listens for key events from the accessibility service
     */
    private val mBroadcastReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                ACTION_RECORD_TRIGGER_KEY -> {
                    val keyEvent = intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT) ?: return

                    lifecycleScope.launchWhenCreated {
                        val deviceName = keyEvent.device.name
                        val deviceDescriptor = keyEvent.device.descriptor
                        val isExternal = keyEvent.device.isExternalCompat

                        mViewModel.addTriggerKey(keyEvent.keyCode, deviceDescriptor, deviceName, isExternal)
                    }
                }

                ACTION_RECORD_TRIGGER_TIMER_INCREMENTED -> {
                    mViewModel.recordingTrigger.value = true
                    val timeLeft = intent.getIntExtra(MyAccessibilityService.EXTRA_TIME_LEFT, 5)

                    mViewModel.recordTriggerTimeLeft.value = timeLeft
                }

                ACTION_STOP_RECORDING_TRIGGER -> {
                    mViewModel.recordingTrigger.value = false
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
        FragmentTriggerBinding.inflate(inflater, container, false).apply {
            mBinding = this

            return this.root
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mBinding.apply {
            viewModel = mViewModel
            lifecycleOwner = viewLifecycleOwner

            mViewModel.chooseTriggerTimeout.observe(viewLifecycleOwner, EventObserver {
                lifecycleScope.launchWhenCreated {
                    var timeout = mViewModel.triggerExtras.value?.find {
                        it.id == Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT
                    }!!.data.toInt()

                    val model = SeekBarListItemModel(
                        id = Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
                        title = str(R.string.seekbar_title_sequence_trigger_timeout),
                        min = int(R.integer.sequence_trigger_timeout_min),
                        max = int(R.integer.sequence_trigger_timeout_max),
                        stepSize = int(R.integer.sequence_trigger_timeout_step_size),
                        initialValue = timeout
                    )

                    timeout = requireActivity().seekBarAlertDialog(model)

                    mViewModel.setTriggerExtra(
                        id = Extra.EXTRA_SEQUENCE_TRIGGER_TIMEOUT,
                        data = timeout.toString()
                    )
                }
            })

            mViewModel.chooseParallelTriggerClickType.observe(viewLifecycleOwner, EventObserver {
                lifecycleScope.launchWhenCreated {
                    val newClickType = showClickTypeDialog()
                    mViewModel.setParallelTriggerClickType(newClickType)
                }
            })

            subscribeTriggerList()

            mViewModel.triggerMode.observe(viewLifecycleOwner) {
                epoxyRecyclerViewTriggers.requestModelBuild()
            }

            mViewModel.buildTriggerKeyModelListEvent.observe(viewLifecycleOwner, EventObserver { triggerKeys ->
                lifecycleScope.launchWhenCreated {
                    val deviceInfoList = mViewModel.getDeviceInfoList()

                    val modelList = sequence {
                        triggerKeys.forEach {
                            val model = it.buildModel(requireContext(), deviceInfoList)
                            yield(model)
                        }
                    }.toList()

                    mViewModel.triggerKeyModelList.value = modelList
                }
            })
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
    private fun FragmentTriggerBinding.subscribeTriggerList() {
        mViewModel.triggerKeyModelList.observe(viewLifecycleOwner) { triggerKeyList ->
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
                            triggerKey?.let {
                                mViewModel.removeTriggerKey(it.keyCode)
                            }
                        }

                        onMoreClick { _ ->
                            if (mViewModel.triggerInSequence.value == true) {
                                lifecycleScope.launch {
                                    val newClickType = showClickTypeDialog()

                                    mViewModel.setTriggerKeyClickType(model.keyCode, newClickType)
                                }
                            }
                        }

                        onDeviceClick { _ ->
                            lifecycleScope.launch {
                                val deviceId = showChooseDeviceDialog()

                                mViewModel.setTriggerKeyDevice(model.keyCode, deviceId)
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
                yield(str(R.string.this_device))
                yield(str(R.string.any_device))

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
                    str(R.string.clicktype_short_press),
                    str(R.string.clicktype_long_press)
                )
            } else {
                arrayOf(
                    str(R.string.clicktype_short_press),
                    str(R.string.clicktype_long_press),
                    str(R.string.clicktype_double_press)
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

    private fun FragmentTriggerBinding.enableTriggerKeyDragging(controller: EpoxyController): ItemTouchHelper {
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