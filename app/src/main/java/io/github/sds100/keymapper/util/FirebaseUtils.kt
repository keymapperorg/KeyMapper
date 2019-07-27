package io.github.sds100.keymapper.util

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import io.github.sds100.keymapper.R
import org.jetbrains.anko.defaultSharedPreferences

/**
 * Created by sds100 on 27/07/2019.
 */

object FirebaseUtils {
    fun setFirebaseDataCollection(ctx: Context) = ctx.apply {
        val isDataCollectionEnabled = defaultSharedPreferences.getBoolean(
            str(R.string.key_pref_data_collection),
            bool(R.bool.default_value_data_collection))

        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(isDataCollectionEnabled)
    }
}