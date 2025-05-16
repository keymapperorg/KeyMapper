package io.github.sds100.keymapper.system.leanback

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LeanbackAdapterImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : LeanbackAdapter {
    // ... existing code ...
} 