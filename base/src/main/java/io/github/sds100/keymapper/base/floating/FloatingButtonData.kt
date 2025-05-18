package io.github.sds100.keymapper.base.floating

import io.github.sds100.keymapper.data.db.typeconverter.ConstantTypeConverters
import io.github.sds100.keymapper.data.entities.FloatingButtonEntity
import io.github.sds100.keymapper.floating.FloatingButtonData.Location
import io.github.sds100.keymapper.common.utils.Orientation
import io.github.sds100.keymapper.common.utils.SizeKM
import io.github.sds100.keymapper.common.utils.getKey
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class FloatingButtonData(
    val uid: String = UUID.randomUUID().toString(),
    val layoutUid: String,
    val layoutName: String,
    val appearance: FloatingButtonAppearance,
    val location: Location,
) {
    /**
     * This stores data about where a draggable overlay is located. It needs extra information
     * about the default orientation so it can be placed in the exact same location regardless
     * of whether it is 90 or 270 degrees landscape for example.
     */
    @Serializable
    data class Location(
        val x: Int,
        val y: Int,
        /**
         * The orientation the screen was in when the user picked the location of the button.
         */
        val orientation: Orientation,
        /**
         * The size of the display this button was placed on.
         */
        val displaySize: SizeKM,
    )
}

object FloatingButtonEntityMapper {
    fun setAppearance(
        entity: FloatingButtonEntity,
        appearance: FloatingButtonAppearance,
    ): FloatingButtonEntity {
        return entity.copy(
            text = appearance.text,
            buttonSize = appearance.size,
            borderOpacity = appearance.borderOpacity,
            backgroundOpacity = appearance.backgroundOpacity,
        )
    }

    fun setLocation(entity: FloatingButtonEntity, location: Location): FloatingButtonEntity {
        return entity.copy(
            x = location.x,
            y = location.y,
            orientation = ConstantTypeConverters.ORIENTATION_MAP[location.orientation]!!,
            displayWidth = location.displaySize.width,
            displayHeight = location.displaySize.height,
        )
    }

    fun fromEntity(entity: FloatingButtonEntity, layoutName: String): FloatingButtonData {
        return FloatingButtonData(
            uid = entity.uid,
            layoutUid = entity.layoutUid,
            layoutName = layoutName,
            appearance = FloatingButtonAppearance(
                text = entity.text,
                size = entity.buttonSize,
                borderOpacity = entity.borderOpacity
                    ?: FloatingButtonAppearance.DEFAULT_BORDER_OPACITY,
                backgroundOpacity = entity.backgroundOpacity
                    ?: FloatingButtonAppearance.DEFAULT_BACKGROUND_OPACITY,
            ),
            location = Location(
                x = entity.x,
                y = entity.y,
                orientation = ConstantTypeConverters.ORIENTATION_MAP.getKey(entity.orientation)!!,
                displaySize = SizeKM(entity.displayWidth, entity.displayHeight),
            ),
        )
    }

    fun toEntity(button: FloatingButtonData): FloatingButtonEntity {
        return FloatingButtonEntity(
            uid = button.uid,
            layoutUid = button.layoutUid,
            text = button.appearance.text,
            buttonSize = button.appearance.size,
            borderOpacity = button.appearance.borderOpacity,
            backgroundOpacity = button.appearance.backgroundOpacity,
            x = button.location.x,
            y = button.location.y,
            orientation = ConstantTypeConverters.ORIENTATION_MAP[button.location.orientation]!!,
            displayWidth = button.location.displaySize.width,
            displayHeight = button.location.displaySize.height,
        )
    }
}
