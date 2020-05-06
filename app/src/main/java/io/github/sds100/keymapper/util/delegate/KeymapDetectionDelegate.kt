package io.github.sds100.keymapper.util.delegate

import android.os.SystemClock
import android.view.KeyEvent
import androidx.collection.SparseArrayCompat
import androidx.collection.keyIterator
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import splitties.bitflags.withFlag
import kotlin.math.pow

/**
 * Created by sds100 on 05/05/2020.
 */

class KeymapDetectionDelegate(iKeymapDetectionDelegate: IKeymapDetectionDelegate)
    : IKeymapDetectionDelegate by iKeymapDetectionDelegate {

    companion object {
        /**
         * The time in ms between repeating an action while holding down.
         */
        private const val REPEAT_DELAY = 50L

        /**
         * How long a key should be held down to repeatedly perform an action in ms.
         */
        private const val HOLD_DOWN_DELAY = 400L

        /**
         * Some keys need to be consumed on the up event to prevent them from working they way they are intended to.
         */
        private val KEYS_TO_CONSUME_UP_EVENT = listOf(
            KeyEvent.KEYCODE_HOME,
            KeyEvent.KEYCODE_APP_SWITCH
        )

        //FLAGS
        //0 to 1023 is reserved for keycodes
        private const val FLAG_SHORT_PRESS = 1024
        private const val FLAG_LONG_PRESS = 2048
        private const val FLAG_DOUBLE_PRESS = 4096

        private fun createDeviceDescriptorMap(descriptors: Set<String>): SparseArrayCompat<String> {
            var key = 2.0.pow(13).toInt()
            val map = SparseArrayCompat<String>()

            descriptors.forEach {
                map.put(key, it)
                key *= 2
            }

            return map
        }

        private fun createActionMap(actions: Set<Action>): SparseArrayCompat<Action> {
            var key = 0

            val map = SparseArrayCompat<Action>()

            actions.forEach {
                map.put(key, it)
                key++
            }

            return map
        }

        /**
         * Key presses will be encoded as an integer to improve performance and simplify the data structures that
         * could be needed. Attributes will be stored as flags added to the keycode.
         *
         * - 0 to 1023 will be reserved for the keycode.
         * - 1024, 2048, 4096 are the click types.
         * - If the key is from an external device, a flag greater than 4096 is for the key that points to the descriptor
         * in the [mDeviceDescriptorMap]. If there is no flag > 4096, it means the event is from an internal device.
         */
        private fun encodeEvent(keyCode: Int, @Trigger.ClickType clickType: Int, descriptorKey: Int): Int {
            val clickTypeFlag = when (clickType) {
                Trigger.SHORT_PRESS -> FLAG_SHORT_PRESS
                Trigger.LONG_PRESS -> FLAG_LONG_PRESS
                Trigger.DOUBLE_PRESS -> FLAG_DOUBLE_PRESS
                else -> 0
            }

            return if (descriptorKey == -1) {
                keyCode.withFlag(clickTypeFlag)
            } else {
                keyCode.withFlag(clickTypeFlag).withFlag(descriptorKey)
            }
        }
    }

    /**
     * A cached copy of the keymaps in the database
     */
    var keyMapListCache: List<KeyMap> = listOf()
        set(value) {
            mSequenceTriggerActionMap.clear()
            mParallelTriggerActionMap.clear()
            mDeviceDescriptorMap.clear()
            mActionMap.clear()

            // If there are no keymaps with actions then keys don't need to be detected.
            if (!value.any { it.actionList.isNotEmpty() }) {
                field = value
                mDetectKeymaps = false
                return
            }

            if (value.isEmpty()) {
                mDetectKeymaps = false
            } else {
                mDetectKeymaps = true

                val sequenceKeyMaps = mutableListOf<KeyMap>()
                val parallelKeyMaps = mutableListOf<KeyMap>()
                val deviceDescriptors = mutableSetOf<String>()

                mActionMap = createActionMap(value.flatMap { it.actionList }.toSet())

                value.forEach { keyMap ->
                    // ignore the keymap if it has no action.
                    if (keyMap.actionList.isEmpty()) {
                        return@forEach
                    }

                    //TRIGGER STUFF
                    when (keyMap.trigger.mode) {
                        Trigger.PARALLEL -> parallelKeyMaps.add(keyMap)
                        Trigger.SEQUENCE -> sequenceKeyMaps.add(keyMap)
                    }

                    keyMap.trigger.keys.forEach { key ->
                        when (key.deviceId) {
                            Trigger.Key.DEVICE_ID_THIS_DEVICE -> {
                                mDetectInternalEvents = true
                            }

                            Trigger.Key.DEVICE_ID_ANY_DEVICE -> {
                                mDetectInternalEvents = true
                                mDetectExternalEvents = true
                            }

                            else -> {
                                deviceDescriptors.add(key.deviceId)
                                mDetectExternalEvents = true
                            }
                        }
                    }

                    val encodedActionList = encodeActionList(keyMap.actionList)
                    val encodedTrigger = encodeTriggerKeys(keyMap.trigger.keys)

                    when (keyMap.trigger.mode) {
                        Trigger.SEQUENCE -> {
                            mDetectSequenceTriggers = true
                            mSequenceTriggerActionMap[encodedTrigger] = intArrayOf()
                        }

                        Trigger.PARALLEL -> {
                            mDetectParallelTriggers = true
                            mParallelTriggerActionMap[encodedTrigger] = encodedActionList
                        }
                    }

                    actions.addAll(keyMap.actionList)
                }

                mDeviceDescriptorMap = createDeviceDescriptorMap(deviceDescriptors)

                val sequenceTriggerKeys = sequenceKeyMaps.map { it.trigger.keys }
                val parallelTriggerKeys = parallelKeyMaps.map { it.trigger.keys }

                mLongestSequenceTrigger = sequenceTriggerKeys.maxBy { it.size }?.size ?: 0

                //double press parallel triggers
                val doublePressParallelTriggers = parallelTriggerKeys
                    .filter { it.size == 1 && it[0].clickType == Trigger.DOUBLE_PRESS }
                    .map { it[0] }
                    .toTypedArray()

                mDoublePressParallelTriggerKeyCodes = doublePressParallelTriggers.map { it.keyCode }.toTypedArray()
                mDoublePressParallelTriggerDeviceIds = doublePressParallelTriggers.map { it.deviceId }.toTypedArray()
            }

            field = value
        }

    /**
     * The longest trigger in sequence mode. This will determine how many key presses need to be remembered.
     */
    private var mLongestSequenceTrigger = 0
    private var mDetectKeymaps = false
    private var mDetectInternalEvents = false
    private var mDetectExternalEvents = false
    private var mDetectSequenceTriggers = false
    private var mDetectParallelTriggers = false

    private var mDetectDoublePresses = false

    //double press parallel triggers can only have ONE key
    private var mDoublePressParallelTriggerKeyCodes = arrayOf<Int>()
        set(value) {
            mDetectDoublePresses = value.isNotEmpty()

            field = value
        }

    private var mDoublePressParallelTriggerDeviceIds = arrayOf<String>()

    private var mDeviceDescriptorMap = SparseArrayCompat<String>()
    private var mActionMap = SparseArrayCompat<Action>()

    /**
     * Maps a string representation of a sequence trigger to the actions it should trigger. The actions will
     * be represented by using bit flags.
     */
    private val mSequenceTriggerActionMap = mutableMapOf<String, IntArray>()

    /**
     * Maps a string representation of a parallel trigger to the actions it should trigger. The actions will
     * be represented by using bit flags.
     */
    private val mParallelTriggerActionMap = mutableMapOf<String, IntArray>()

    private val mHeldDownKeys = mutableSetOf<Int>()

    private val mSequenceEvents = mutableListOf<Int>()

    /**
     * @return whether to consume the [KeyEvent].
     */
    fun onKeyEvent(keyCode: Int, action: Int, downTime: Long, descriptor: String, isExternal: Boolean): Boolean {
        if (!mDetectKeymaps) return false

        val descriptorKey = getDescriptorKey(descriptor)

        if (isExternal) {
            if (!mDetectExternalEvents) return false

            /*
            If the descriptor key is -1, there are no triggers which map this device
             */
            if (descriptorKey == -1) return false
        } else {
            if (!mDetectInternalEvents) return false
        }

        when (action) {
            KeyEvent.ACTION_DOWN -> return onKeyDown(keyCode, descriptorKey)
            KeyEvent.ACTION_UP -> return onKeyUp(keyCode, downTime, descriptorKey)
        }

        return false
    }

    /**
     * @return whether to consume the [KeyEvent].
     */
    private fun onKeyDown(keyCode: Int, descriptorKey: Int): Boolean {

        val encodedEvent = encodeEvent(keyCode, clickType = Trigger.UNDETERMINED, descriptorKey = descriptorKey)
        mHeldDownKeys.add(encodedEvent)

        return false
    }

    /**
     * @return whether to consume the [KeyEvent].
     */
    private fun onKeyUp(keyCode: Int, downTime: Long, descriptorKey: Int): Boolean {

        var encodedEvent = encodeEvent(keyCode, clickType = Trigger.UNDETERMINED, descriptorKey = descriptorKey)
        mHeldDownKeys.remove(encodedEvent)

        if ((SystemClock.uptimeMillis() - downTime) < AppPreferences.longPressDelay) {
            encodedEvent = encodedEvent.withFlag(FLAG_SHORT_PRESS)
        } else {
            encodedEvent = encodedEvent.withFlag(FLAG_LONG_PRESS)
        }

        if (mDetectSequenceTriggers) {
            addSequenceEvent(encodedEvent)
        }

        if (mDetectParallelTriggers) {

        }

        return false
    }

    fun reset() {
        mHeldDownKeys.clear()
        mSequenceEvents.clear()
    }

    private fun addSequenceEvent(encodedEvent: Int) {
        mSequenceEvents.add(encodedEvent)

        if (mSequenceEvents.size > mLongestSequenceTrigger) {
            mSequenceEvents.removeAt(0)
        }

        val encodedSequenceEvents = mSequenceEvents.joinToString(":")

        mSequenceTriggerActionMap.entries.forEach { entry ->
            val encodedTrigger = entry.key
            val encodedActions = entry.value

            encodedActions

            if (encodedSequenceEvents.contains(encodedTrigger)) {

            }
        }
    }

    private fun encodeTriggerKeys(keys: List<Trigger.Key>) = keys.joinToString(":") {
        var descriptorKey = getDescriptorKey(it.deviceId)

        if (descriptorKey == -1) descriptorKey = 0

        encodeEvent(it.keyCode, it.clickType, descriptorKey).toString()
    }

    private fun encodeActionList(actions: List<Action>): Int {
        var encodedActionList = 0

        actions.forEach {
            encodedActionList = encodedActionList.withFlag(getActionKey(it))
        }

        return encodedActionList
    }

    /**
     * @return the key for the action in [mActionMap]. Returns -1 if the [action] can't be found.
     */
    private fun getActionKey(action: Action): Int {
        mActionMap.keyIterator().forEach { key ->
            if (mActionMap[key] == action) {
                return key
            }
        }

        throw Exception("Action ${action.uniqueId} not in the action map!")
    }

    /**
     * @return the key for the device descriptor in [mDeviceDescriptorMap]. Returns -1 if this descriptor isn't in
     * [mDeviceDescriptorMap]
     */
    private fun getDescriptorKey(descriptor: String): Int {
        mDeviceDescriptorMap.keyIterator().forEach { key ->
            if (mDeviceDescriptorMap[key] == descriptor) {
                return key
            }
        }

        return -1
    }
}