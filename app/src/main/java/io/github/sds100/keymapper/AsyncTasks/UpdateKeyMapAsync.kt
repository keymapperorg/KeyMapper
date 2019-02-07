package io.github.sds100.keymapper.AsyncTasks

import android.os.AsyncTask
import io.github.sds100.keymapper.Data.AppDatabase
import io.github.sds100.keymapper.KeyMap

/**
 * Created by sds100 on 05/09/2018.
 */

class UpdateKeyMapAsync(private val mDb: AppDatabase): AsyncTask<KeyMap, Unit, Unit>() {
    override fun doInBackground(vararg params: KeyMap) {
        mDb.keyMapDao().update(*params)
    }
}