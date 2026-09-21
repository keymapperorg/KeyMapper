package io.github.sds100.keymapper.base.trigger

import io.github.sds100.keymapper.base.vibration.VibrateEffect
import io.github.sds100.keymapper.data.entities.EntityExtra
import io.github.sds100.keymapper.data.entities.TriggerEntity
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.nullValue
import org.junit.Test

class TriggerEntityMapperTest {

    @Test
    fun `save and load a custom vibration duration`() {
        val trigger = Trigger(
            vibrate = true,
            vibrateEffect = VibrateEffect.CustomDuration(250L),
        )

        val entity = TriggerEntityMapper.toEntity(trigger)
        val loadedTrigger = TriggerEntityMapper.fromEntity(entity, floatingButtons = emptyList())

        assertThat(loadedTrigger.vibrateEffect, `is`(VibrateEffect.CustomDuration(250L)))
    }

    @Test
    fun `save and load a predefined vibration effect`() {
        val trigger = Trigger(
            vibrate = true,
            vibrateEffect = VibrateEffect.Predefined(VibrateEffect.PredefinedType.TICK),
        )

        val entity = TriggerEntityMapper.toEntity(trigger)
        val loadedTrigger = TriggerEntityMapper.fromEntity(entity, floatingButtons = emptyList())

        assertThat(
            loadedTrigger.vibrateEffect,
            `is`(VibrateEffect.Predefined(VibrateEffect.PredefinedType.TICK)),
        )
    }

    @Test
    fun `load a custom duration from a trigger saved before predefined effects existed`() {
        // GIVEN an entity saved by an older version of the app that only stored a raw duration.
        val entity = TriggerEntity(
            flags = TriggerEntity.TRIGGER_FLAG_VIBRATE,
            extras = listOf(
                @Suppress("DEPRECATION")
                EntityExtra(TriggerEntity.EXTRA_VIBRATION_DURATION, "250"),
            ),
        )

        val trigger = TriggerEntityMapper.fromEntity(entity, floatingButtons = emptyList())

        assertThat(trigger.vibrateEffect, `is`(VibrateEffect.CustomDuration(250L)))
    }

    @Test
    fun `load null vibrate effect when nothing was ever set`() {
        val entity = TriggerEntity(flags = TriggerEntity.TRIGGER_FLAG_VIBRATE)

        val trigger = TriggerEntityMapper.fromEntity(entity, floatingButtons = emptyList())

        assertThat(trigger.vibrateEffect, `is`(nullValue()))
    }
}
