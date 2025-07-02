package io.github.sds100.keymapper.api

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.sds100.keymapper.system.apps.KeyMapShortcutActivityIntentBuilder

@Module
@InstallIn(SingletonComponent::class)
abstract class ApiHiltModule {
    @Binds
    abstract fun bindKeyMapShortcutActivityIntentBuilder(
        impl: KeyMapShortcutActivityIntentBuilderImpl,
    ): KeyMapShortcutActivityIntentBuilder
}
