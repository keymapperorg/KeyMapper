package io.github.sds100.keymapper.system.intents

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntentAdapterImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IntentAdapter {
    // ... existing code ...
} 