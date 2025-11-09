package io.github.sds100.keymapper.system.apps

import android.content.Intent
import android.os.Bundle

interface KeyMapShortcutActivityIntentBuilder {
    fun build(intentAction: String, intentExtras: Bundle): Intent
}
