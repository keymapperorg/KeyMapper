package io.github.sds100.keymapper.base.keymaps

import io.github.sds100.keymapper.data.repositories.KeyMapRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnableGroupUseCaseImpl @Inject constructor(private val keyMapRepository: KeyMapRepository) :
    EnableGroupUseCase {

    override fun enable(groupUid: String) {
        keyMapRepository.enableByGroup(groupUid)
    }

    override fun toggle(groupUid: String) {
        keyMapRepository.toggleByGroup(groupUid)
    }

    override fun disable(groupUid: String) {
        keyMapRepository.disableByGroup(groupUid)
    }
}

interface EnableGroupUseCase {
    fun enable(groupUid: String)
    fun toggle(groupUid: String)
    fun disable(groupUid: String)
}
