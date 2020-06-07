package io.github.sds100.keymapper.data

import androidx.annotation.StringRes

/**
 * Created by sds100 on 18/05/2020.
 */
interface IOnboardingState {
    fun getShownPrompt(@StringRes key: Int) : Boolean
    fun setShownPrompt(@StringRes key: Int)
}