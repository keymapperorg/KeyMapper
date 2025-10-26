package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.data.repositories.KeyMapRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnableKeyMapsUseCaseImpl
    @Inject
    constructor(
        private val keyMapRepository: KeyMapRepository,
    ) : EnableKeyMapsUseCase {
        override fun enable(uid: String) {
            keyMapRepository.enableById(uid)
        }

        override fun toggle(uid: String) {
            keyMapRepository.toggleById(uid)
        }

        override fun disable(uid: String) {
            keyMapRepository.disableById(uid)
        }
    }

interface EnableKeyMapsUseCase {
    fun enable(uid: String)

    fun toggle(uid: String)

    fun disable(uid: String)
}
