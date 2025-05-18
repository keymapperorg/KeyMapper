package io.github.sds100.keymapper.api

import dagger.Binds
import dagger.Module
import io.github.sds100.keymapper.system.apps.KeyMapShortcutActivityIntentBuilder

@Module
class ApiHiltModule {
    @Binds
    fun bindKeyMapShortcutActivityIntentBuilder(
        impl: KeyMapShortcutActivityIntentBuilderImpl,
    ): KeyMapShortcutActivityIntentBuilder {
        return impl
    }
}
