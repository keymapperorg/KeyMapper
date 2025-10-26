package io.github.sds100.keymapper.api

import android.content.Context
import android.content.Intent
import android.os.Bundle
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.sds100.keymapper.system.apps.KeyMapShortcutActivityIntentBuilder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KeyMapShortcutActivityIntentBuilderImpl
    @Inject
    constructor(
        @ApplicationContext private val ctx: Context,
    ) : KeyMapShortcutActivityIntentBuilder {
        override fun build(
            intentAction: String,
            intentExtras: Bundle,
        ): Intent =
            Intent(ctx, LaunchKeyMapShortcutActivity::class.java).apply {
                action = intentAction

                putExtras(intentExtras)
            }
    }
