package io.github.sds100.keymapper.DatabaseAsyncTasks

import android.os.AsyncTask
import io.github.sds100.keymapper.Data.AppDatabase

/**
 * Created by sds100 on 05/09/2018.
 */

class DeleteKeyMapByIdAsync(private val mDb: AppDatabase) : AsyncTask<Long, Unit, Unit>() {
    override fun doInBackground(vararg params: Long?) {
        mDb.keyMapDao().deleteById(*params.map { it!! }.toLongArray())
    }
}