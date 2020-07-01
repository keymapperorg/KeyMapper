package io.github.sds100.keymapper.worker

import android.content.Context
import android.view.KeyEvent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.MyApplication
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.Constraint
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.ActionType
import kotlinx.coroutines.coroutineScope
import splitties.bitflags.withFlag

/**
 * Created by sds100 on 26/01/2020.
 */

class SeedDatabaseWorker(
    context: Context, workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {
    override suspend fun doWork(): Result = coroutineScope {
        try {
            val keymaps = sequence {
                for (i in 1..100) {
                    yield(KeyMap(
                        id = 0,
                        trigger = createRandomTrigger(),
                        actionList = createRandomActionList(),
                        constraintList = listOf(
                            Constraint.appConstraint(Constraint.APP_FOREGROUND, Constants.PACKAGE_NAME),
                            Constraint.appConstraint(Constraint.APP_NOT_FOREGROUND, "io.github.sds100.keymapper.ci")
                        ),
                        flags = 0.withFlag(KeyMap.KEYMAP_FLAG_VIBRATE)
                    ))
                }
            }.toList().toTypedArray()

            (applicationContext as MyApplication).keymapRepository.insertKeymap(*keymaps)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
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