package io.github.sds100.keymapper.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.github.sds100.keymapper.Constants
import io.github.sds100.keymapper.data.db.AppDatabase
import io.github.sds100.keymapper.data.model.Action
import io.github.sds100.keymapper.data.model.KeyMap
import io.github.sds100.keymapper.data.model.Trigger
import io.github.sds100.keymapper.util.ActionType
import kotlinx.coroutines.coroutineScope
import kotlin.random.Random

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
                        actionList = createRandomActionList()
                    ))
                }
            }.toList().toTypedArray()

            val database = AppDatabase.getInstance(applicationContext)
            database.keymapDao().insert(*keymaps)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createRandomTrigger(): Trigger {
        val keys = sequence {
            repeat(10) {
                yield(Trigger.Key(Random.nextInt(1, 100)))
            }
        }.toList()

        return Trigger(keys)
    }

    private fun createRandomActionList(): List<Action> {
        return sequence {
            yield(Action(
                type = ActionType.APP,
                data = Constants.PACKAGE_NAME
            ))
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