package io.github.sds100.keymapper.system.ringtones

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidRingtoneAdapter @Inject constructor(
    @ApplicationContext private val context: Context
) : RingtoneAdapter {
    // ... existing code ...
} 