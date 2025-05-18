package io.github.sds100.keymapper.api

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.system.apps.KeyMapShortcutActivityIntentBuilder
import javax.inject.Singleton

@Singleton
class KeyMapShortcutActivityIntentBuilderImpl(
    @ApplicationContext private val ctx: Context,
) : KeyMapShortcutActivityIntentBuilder {
    override fun build(intentAction: String, intentExtras: Bundle): Intent {
        return Intent(ctx, LaunchKeyMapShortcutActivity::class.java).apply {
            action = intentAction

            putExtras(intentExtras)
        }
    }
}
