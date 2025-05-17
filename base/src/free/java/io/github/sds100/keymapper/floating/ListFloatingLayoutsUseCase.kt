package io.github.sds100.keymapper.floating

import io.github.sds100.keymapper.data.repositories.FloatingLayoutRepository
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import io.github.sds100.keymapper.purchasing.PurchasingManager
import io.github.sds100.keymapper.system.accessibility.ServiceAdapter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface ListFloatingLayoutsUseCase {
    val showFloatingLayouts: Flow<Boolean>
}

class ListFloatingLayoutsUseCaseImpl(
    private val repository: FloatingLayoutRepository,
    private val purchasingManager: PurchasingManager,
    private val serviceAdapter: ServiceAdapter,
    private val preferences: PreferenceRepository,
) : ListFloatingLayoutsUseCase,
    PurchasingManager by purchasingManager {

    override val showFloatingLayouts: Flow<Boolean> = flowOf(false)
}
