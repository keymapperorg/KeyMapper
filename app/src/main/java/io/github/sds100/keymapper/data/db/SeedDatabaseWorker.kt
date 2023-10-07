package io.github.sds100.keymapper.data.db

import android.content.Context
import android.view.KeyEvent
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.ServiceLocator
import io.github.sds100.keymapper.data.entities.ActionEntity
import io.github.sds100.keymapper.data.entities.KeyMapEntity
import io.github.sds100.keymapper.data.entities.TriggerEntity
import kotlinx.coroutines.coroutineScope

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
                    yield(
                        KeyMapEntity(
                            id = 0,
                            trigger = createRandomTrigger(),
                            actionList = createRandomActionList(),
                            flags = 0
                        )
                    )
                }
            }.toList().toTypedArray()

            ServiceLocator.roomKeymapRepository(applicationContext).insert(*keymaps)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createRandomTrigger(): TriggerEntity {
        val keys = sequence {
            yield(
                TriggerEntity.KeyEntity(
                    KeyEvent.KEYCODE_CTRL_LEFT,
                    TriggerEntity.KeyEntity.DEVICE_ID_THIS_DEVICE,
                    null,
                    TriggerEntity.SHORT_PRESS
                )
            )
            yield(
                TriggerEntity.KeyEntity(
                    KeyEvent.KEYCODE_ALT_LEFT,
                    TriggerEntity.KeyEntity.DEVICE_ID_ANY_DEVICE,
                    null,
                    TriggerEntity.LONG_PRESS
                )
            )
            yield(
                TriggerEntity.KeyEntity(
                    KeyEvent.KEYCODE_DEL,
                    TriggerEntity.KeyEntity.DEVICE_ID_THIS_DEVICE,
                    null,
                    TriggerEntity.SHORT_PRESS
                )
            )
        }.toList()

        return TriggerEntity(keys, mode = TriggerEntity.SEQUENCE, flags = TriggerEntity.TRIGGER_FLAG_VIBRATE)
    }

    private fun createRandomActionList(): List<ActionEntity> {
        return sequence {
            yield(
                ActionEntity(
                    type = ActionEntity.Type.APP,
                    data = Constants.PACKAGE_NAME
                )
            )
            yield(
                ActionEntity(
                    type = ActionEntity.Type.APP,
                    data = "this.app.doesnt.exist"
                )
            )
        }.toList()
    }
}