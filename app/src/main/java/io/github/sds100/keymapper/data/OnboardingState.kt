package io.github.sds100.keymapper.data

import android.content.Context
import androidx.core.content.edit
import io.github.sds100.keymapper.util.defaultSharedPreferences
import io.github.sds100.keymapper.util.str

/**
 * Created by sds100 on 20/02/2020.
 */

@Suppress("EXPERIMENTAL_API_USAGE")
class OnboardingState(ctx: Context) : IOnboardingState {
    private val mCtx = ctx.applicationContext

    override fun getShownPrompt(key: Int): Boolean {
        return mCtx.defaultSharedPreferences.getBoolean(mCtx.str(key), false)
    }

    override fun setShownPrompt(key: Int) {
        mCtx.defaultSharedPreferences.edit {
            putBoolean(mCtx.str(key), true)
        }
    }
}