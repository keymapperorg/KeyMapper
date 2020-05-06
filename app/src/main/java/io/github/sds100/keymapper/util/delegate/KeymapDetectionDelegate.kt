package io.github.sds100.keymapper.util.delegate

import android.os.SystemClock
import android.util.Log
import android.view.KeyEvent
import androidx.collection.SparseArrayCompat
import androidx.collection.keyIterator
import io.github.sds100.keymapper.data.AppPreferences
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import splitties.bitflags.hasFlag
import splitties.bitflags.withFlag

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
        private const val FLAG_INTERNAL_DEVICE = 8192

        private fun createDeviceDescriptorMap(descriptors: Set<String>): SparseArrayCompat<String> {
            var key = 16384
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
         * - An 8192 flag means the event came from an internal device.
         * - If the key is from an external device, a flag greater than 8192 is for the key that points to the descriptor
         * in the [mDeviceDescriptorMap].
         */
        private fun encodeEvent(keyCode: Int, @Trigger.ClickType clickType: Int, descriptorKey: Int): Int {
            val clickTypeFlag = when (clickType) {
                Trigger.SHORT_PRESS -> FLAG_SHORT_PRESS
                Trigger.LONG_PRESS -> FLAG_LONG_PRESS
                Trigger.DOUBLE_PRESS -> FLAG_DOUBLE_PRESS
                else -> 0
            }

            return if (descriptorKey == -1) {
                keyCode.withFlag(clickTypeFlag).withFlag(FLAG_INTERNAL_DEVICE)
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
                val longPressKeycodes = mutableSetOf<Int>()

                mActionMap = createActionMap(value.flatMap { it.actionList }.toSet())

                // Extract all the external device descriptors used in enabled keymaps because the list is used later
                val deviceDescriptors = mutableSetOf<String>()

                for (keyMap in value) {
                    if (!keyMap.isEnabled) {
                        continue
                    }

                    keyMap.trigger.keys.forEach {

                        if (it.deviceId != Trigger.Key.DEVICE_ID_THIS_DEVICE &&
                            it.deviceId != Trigger.Key.DEVICE_ID_ANY_DEVICE) {
                            deviceDescriptors.add(it.deviceId)
                        }
                    }
                }

                mDeviceDescriptorMap = createDeviceDescriptorMap(deviceDescriptors)

                for (keyMap in value) {
                    // ignore the keymap if it has no action.
                    if (keyMap.actionList.isEmpty()) {
                        continue
                    }

                    if (!keyMap.isEnabled) {
                        continue
                    }

                    //TRIGGER STUFF
                    when (keyMap.trigger.mode) {
                        Trigger.PARALLEL -> parallelKeyMaps.add(keyMap)
                        Trigger.SEQUENCE -> sequenceKeyMaps.add(keyMap)
                    }

                    val encodedTriggerList = mutableListOf<Int>()

                    keyMap.trigger.keys.forEach { key ->

                        if (key.clickType == Trigger.LONG_PRESS) {
                            longPressKeycodes.add(key.keyCode)
                        }

                        when (key.deviceId) {
                            Trigger.Key.DEVICE_ID_THIS_DEVICE -> {
                                mDetectInternalEvents = true
                            }

                            Trigger.Key.DEVICE_ID_ANY_DEVICE -> {
                                mDetectInternalEvents = true
                                mDetectExternalEvents = true
                            }

                            else -> {
                                mDetectExternalEvents = true
                            }
                        }

                        encodedTriggerList.add(encodeTriggerKey(key))
                    }

                    val encodedActionList = encodeActionList(keyMap.actionList)

                    when (keyMap.trigger.mode) {
                        Trigger.SEQUENCE -> {
                            mDetectSequenceTriggers = true
                            mSequenceTriggerActionMap[encodedTriggerList.toIntArray()] = encodedActionList
                        }

                        Trigger.PARALLEL -> {
                            mDetectParallelTriggers = true
                            mParallelTriggerActionMap[encodedTriggerList.toIntArray()] = encodedActionList
                        }
                    }
                }

                val sequenceTriggerKeys = sequenceKeyMaps.map { it.trigger.keys }
                val parallelTriggerKeys = parallelKeyMaps.map { it.trigger.keys }

                mLongestSequenceTrigger = sequenceTriggerKeys.maxBy { it.size }?.size ?: 0
                mLongPressKeyCodes = longPressKeycodes

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

    /**
     * Keys, which have been remapped on long press, need to be consumed on the down event so they don't perform
     * the action.
     */
    private var mLongPressKeyCodes = setOf<Int>()

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
    private val mSequenceTriggerActionMap = mutableMapOf<IntArray, IntArray>()

    /**
     * Maps a string representation of a parallel trigger to the actions it should trigger. The actions will
     * be represented by using bit flags.
     */
    private val mParallelTriggerActionMap = mutableMapOf<IntArray, IntArray>()

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

        if (mLongPressKeyCodes.contains(keyCode)) {
            return true
        }

        return false
    }

    /**
     * @return whether to consume the event.
     */
    private fun onKeyUp(keyCode: Int, downTime: Long, descriptorKey: Int): Boolean {

        var consumeEvent = false

        var encodedEvent = encodeEvent(keyCode, clickType = Trigger.UNDETERMINED, descriptorKey = descriptorKey)
        mHeldDownKeys.remove(encodedEvent)

        if ((SystemClock.uptimeMillis() - downTime) < AppPreferences.longPressDelay) {
            encodedEvent = encodedEvent.withFlag(FLAG_SHORT_PRESS)
        } else {
            encodedEvent = encodedEvent.withFlag(FLAG_LONG_PRESS)
        }

        if (mDetectSequenceTriggers) {
            onSequenceEvent(encodedEvent).let { consume ->
                if (consume) {
                    consumeEvent = true
                }
            }

        }

        if (mDetectParallelTriggers) {

        }

        return consumeEvent
    }

    fun reset() {
        mHeldDownKeys.clear()
        mSequenceEvents.clear()
    }

    /**
     * @return whether to consume the event.
     */
    private fun onSequenceEvent(encodedEvent: Int): Boolean {
        mSequenceEvents.add(encodedEvent)

        if (mSequenceEvents.size > mLongestSequenceTrigger) {
            mSequenceEvents.removeAt(0)
        }

        var consumeEvent = false
        val actionKeysToPerform = mutableSetOf<Int>()

        triggerLoop@ for (entry in mSequenceTriggerActionMap) {
            val trigger = entry.key
            var previousKeyMatchedEvent = false

            //the index of the event which matched the previous key in the trigger
            var previousKeyMatchedEventIndex = -1

            keyLoop@ for ((keyIndex, key) in trigger.withIndex()) {

                /*if the previous key in the trigger didn't match to any event, skip the trigger because
                * at least one key doesn't match. */
                if (keyIndex > 0 && !previousKeyMatchedEvent) {
                    continue@triggerLoop
                }

                eventLoop@ for ((eventIndex, event) in mSequenceEvents.withIndex()) {

                    //set this to false because if this key does match the event, it will be set to true
                    previousKeyMatchedEvent = false

                    /* the last key matched with the last event which means this key definitely won't match
                    * with an event after it. */
                    if (previousKeyMatchedEventIndex == mSequenceEvents.lastIndex) {
                        continue@triggerLoop
                    }

                    if (event.keyCode != key.keyCode) {
                        continue@eventLoop
                    }

                    if (event.clickType != key.clickType) {
                        continue@eventLoop
                    }

                    if (event.internalDevice) {
                        if (key.externalDevice) {
                            continue@eventLoop
                        }

                    } else {
                        if (key.internalDevice) {
                            continue@eventLoop
                        }

                        /* if the trigger key isn't for ANY device, then the key is for a particular device, so if
                        the device descriptors don't match, skip the event. */
                        if (key.deviceDescriptor != 0 && event.deviceDescriptor != key.deviceDescriptor) {
                            continue@eventLoop
                        }
                    }

                    if (previousKeyMatchedEventIndex >= eventIndex) {
                        continue@eventLoop
                    }

                    previousKeyMatchedEvent = true
                    previousKeyMatchedEventIndex = eventIndex
                    consumeEvent = true

                    /* if the event hasn't been skipped then it is a match so break out of the event loop and go to the
                    next key in the loop
                     */
                    break@eventLoop
                }

                if (previousKeyMatchedEvent && keyIndex == trigger.lastIndex) {
                    val actionKeys = entry.value
                    actionKeysToPerform.addAll(actionKeys.toList())
                }
            }

        }

        actionKeysToPerform.forEach {
            val action = mActionMap[it]
            Log.e(this::class.java.simpleName, "perform... ${action?.uniqueId}")
        }

        if (actionKeysToPerform.isNotEmpty()) {
            mSequenceEvents.clear()
        }

        return consumeEvent
    }

    /**
     * Key presses will be encoded as an integer to improve performance and simplify the data structures that
     * could be needed. Attributes will be stored as flags added to the keycode.
     *
     * - 0 to 1023 will be reserved for the keycode.
     * - 1024, 2048, 4096 are the click types.
     * - An 8192 flag means the event came from an internal device.
     * - If the key is from an external device, a flag greater than 8192 is for the key that points to the descriptor
     * in the [mDeviceDescriptorMap].
     */
    private fun encodeTriggerKey(triggerKey: Trigger.Key): Int {
        val clickTypeFlag = when (triggerKey.clickType) {
            Trigger.SHORT_PRESS -> FLAG_SHORT_PRESS
            Trigger.LONG_PRESS -> FLAG_LONG_PRESS
            Trigger.DOUBLE_PRESS -> FLAG_DOUBLE_PRESS
            else -> 0
        }

        return when (triggerKey.deviceId) {
            Trigger.Key.DEVICE_ID_THIS_DEVICE ->
                triggerKey.keyCode.withFlag(clickTypeFlag).withFlag(FLAG_INTERNAL_DEVICE)

            Trigger.Key.DEVICE_ID_ANY_DEVICE ->
                triggerKey.keyCode.withFlag(clickTypeFlag)

            else -> {
                val descriptorKey = getDescriptorKey(triggerKey.deviceId)
                triggerKey.keyCode.withFlag(clickTypeFlag).withFlag(descriptorKey)
            }
        }
    }

    private fun encodeActionList(actions: List<Action>): IntArray {
        return actions.map { getActionKey(it) }.toIntArray()
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

    private val Int.internalDevice
        get() = this.hasFlag(FLAG_INTERNAL_DEVICE)

    private val Int.externalDevice
        get() = this > 16384

    private val Int.anyDevice
        get() = this < 8192

    private val Int.keyCode
        get() = this and 1023

    private val Int.clickType
        //bit shift right 10x and only keep last 3 bits
        get() = (this shr 10) and 7

    private val Int.deviceDescriptor
        get() = (this shr 13) shl 13
}