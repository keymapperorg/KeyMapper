package io.github.sds100.keymapper.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import io.github.sds100.keymapper.data.KeyMapRepository
import io.github.sds100.keymapper.KeymapLiveData

/**
 * Created by sds100 on 05/09/2018.
 */

abstract class ConfigKeyMapViewModel(application: Application) : AndroidViewModel(application) {

    abstract val keyMap: KeymapLiveData

    abstract fun saveKeymap()

    val keyMapRepository = KeyMapRepository.getInstance(application.applicationContext)
}