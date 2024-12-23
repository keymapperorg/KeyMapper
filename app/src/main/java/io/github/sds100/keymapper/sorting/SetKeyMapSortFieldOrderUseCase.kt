package io.github.sds100.keymapper.sorting

import io.github.sds100.keymapper.data.Keys
import io.github.sds100.keymapper.data.repositories.PreferenceRepository
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SetKeyMapSortFieldOrderUseCase(
    private val preferenceRepository: PreferenceRepository,
) {
    operator fun invoke(sortFieldOrders: List<SortFieldOrder>) {
        val json = Json.encodeToString(sortFieldOrders)
        preferenceRepository.set(Keys.sortOrderJson, json)
    }
}
