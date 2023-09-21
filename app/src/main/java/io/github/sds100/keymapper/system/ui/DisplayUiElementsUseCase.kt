package io.github.sds100.keymapper.system.ui

import io.github.sds100.keymapper.data.repositories.ViewIdRepository
import io.github.sds100.keymapper.util.State
import io.github.sds100.keymapper.util.dataOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.withContext

class DisplayUiElementsUseCaseImpl (
    private val viewIdRepository: ViewIdRepository
): DisplayUiElementsUseCase {
    override val uiElements: MutableStateFlow<State<List<UiElementInfo>>> = MutableStateFlow(State.Loading)

    override suspend fun updateUiElementsList() {
        withContext(Dispatchers.Default) {
            uiElements.value = State.Loading

            viewIdRepository.viewIdList.collectLatest { data ->
                val tmpList = arrayListOf<UiElementInfo>()

                data.dataOrNull()?.map { viewIdEntity ->
                    tmpList.add(
                        UiElementInfo(
                            elementName = viewIdEntity.viewId,
                            packageName = viewIdEntity.packageName,
                            fullName = viewIdEntity.fullName,
                        )
                    )
                }

                uiElements.value = State.Data(tmpList)
            }
        }
    }
}

interface DisplayUiElementsUseCase {
    val uiElements: MutableStateFlow<State<List<UiElementInfo>>>

    suspend fun updateUiElementsList()
}