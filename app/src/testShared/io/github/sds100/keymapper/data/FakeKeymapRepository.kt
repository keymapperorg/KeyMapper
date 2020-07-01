package io.github.sds100.keymapper.data

import android.view.KeyEvent
import androidx.lifecycle.MutableLiveData
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.ActionType
import io.github.sds100.keymapper.util.Event
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 21/05/20.
 */
class FakeKeymapRepository : KeymapRepository {

    private val mKeymapList = mutableMapOf<Long, KeyMap>()

    override val keymapList: MutableLiveData<List<KeyMap>> = MutableLiveData(mKeymapList.values.toList())

    override suspend fun getKeymaps(): List<KeyMap> = mKeymapList.values.toList()

    override suspend fun getKeymap(id: Long) = getKeymaps().single { it.id == id }

    override suspend fun insertKeymap(vararg keymap: KeyMap) {
        keymap.forEach {
            it.id = mKeymapList.size.toLong()
            mKeymapList.putIfAbsent(it.id, it)

            keymapList.postValue(mKeymapList.values.toList())
        }
    }

    override suspend fun updateKeymap(keymap: KeyMap) {
        mKeymapList[keymap.id] = keymap
        keymapList.postValue(mKeymapList.values.toList())
    }

    override suspend fun duplicateKeymap(vararg id: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun enableKeymapById(vararg id: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun disableKeymapById(vararg id: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun deleteKeymap(vararg id: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun enableAll() {
        TODO("Not yet implemented")
    }

    override suspend fun disableAll() {
        TODO("Not yet implemented")
    }

    override suspend fun deleteAll() {
        mKeymapList.clear()
        keymapList.value = mKeymapList.values.toList()
    }

    suspend fun createTestData() {
        for (i in 0..100) {
            insertKeymap(KeyMap(
                id = i.toLong(),
                trigger = createRandomTrigger(),
                actionList = createRandomActionList(),
                constraintList = listOf(
                    Constraint.appConstraint(Constraint.APP_FOREGROUND, Constants.PACKAGE_NAME),
                    Constraint.appConstraint(Constraint.APP_NOT_FOREGROUND, "io.github.sds100.keymapper.ci")
                ),
                flags = 0.withFlag(Trigger.TRIGGER_FLAG_VIBRATE)
            ))
        }
    }

    fun clear() {
        mKeymapList.clear()
    }

    private fun createRandomTrigger(): Trigger {
        val keys = sequence {
            yield(Trigger.Key(
                KeyEvent.KEYCODE_CTRL_LEFT,
                Trigger.Key.DEVICE_ID_THIS_DEVICE,
                Trigger.SHORT_PRESS
            ))
            yield(Trigger.Key(
                KeyEvent.KEYCODE_ALT_LEFT,
                Trigger.Key.DEVICE_ID_ANY_DEVICE,
                Trigger.LONG_PRESS
            ))
            yield(Trigger.Key(
                KeyEvent.KEYCODE_DEL,
                Trigger.Key.DEVICE_ID_THIS_DEVICE,
                Trigger.SHORT_PRESS
            ))
        }.toList()

        return Trigger(keys, mode = Trigger.SEQUENCE)
    }

    private fun createRandomActionList(): List<Action> {
        return sequence {
            yield(Action(
                type = ActionType.APP,
                data = Constants.PACKAGE_NAME
            ))
            yield(Action(
                type = ActionType.APP,
                data = "this.app.doesnt.exist"
            ))
        }.toList()
    }
}