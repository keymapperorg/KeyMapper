package io.github.sds100.keymapper.ui.callback

import androidx.lifecycle.LiveData

/**
 * Created by sds100 on 27/01/2020.
 */
interface ProgressCallback {
    val loadingContent: LiveData<Boolean>
}