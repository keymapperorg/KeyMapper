package io.github.sds100.keymapper.base.floating

import io.github.sds100.keymapper.data.entities.FloatingLayoutEntity
import io.github.sds100.keymapper.data.entities.FloatingLayoutEntityWithButtons
import java.util.UUID

data class FloatingLayoutData(
    val uid: String = UUID.randomUUID().toString(),
    val name: String,
    val buttons: List<FloatingButtonData> = emptyList(),
)

object FloatingLayoutEntityMapper {
    fun fromEntity(entity: FloatingLayoutEntityWithButtons): FloatingLayoutData =
        FloatingLayoutData(
            uid = entity.layout.uid,
            name = entity.layout.name,
            buttons =
                entity.buttons.map { buttonEntity ->
                    FloatingButtonEntityMapper.fromEntity(buttonEntity, entity.layout.name)
                },
        )

    fun toEntity(layout: FloatingLayoutData): FloatingLayoutEntity =
        FloatingLayoutEntity(
            uid = layout.uid,
            name = layout.name,
        )
}
