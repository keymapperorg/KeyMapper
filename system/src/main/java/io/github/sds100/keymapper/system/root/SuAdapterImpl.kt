package io.github.sds100.keymapper.system.root

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SuAdapterImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : SuAdapter {
    // ... existing code ...
} 