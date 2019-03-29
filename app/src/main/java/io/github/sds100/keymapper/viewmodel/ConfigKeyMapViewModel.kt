package io.github.sds100.keymapper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.sds100.keymapper.KeymapLiveData
import io.github.sds100.keymapper.data.AppDatabase

/**
 * Created by sds100 on 05/09/2018.
 */

abstract class ConfigKeyMapViewModel(application: Application) : AndroidViewModel(application) {

    val db: AppDatabase = AppDatabase.getInstance(application)

    abstract val keyMap: KeymapLiveData
    abstract fun saveKeymap()
}